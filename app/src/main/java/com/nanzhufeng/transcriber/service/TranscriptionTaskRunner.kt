package com.nanzhufeng.transcriber.service

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.nanzhufeng.transcriber.AppContainer
import com.nanzhufeng.transcriber.data.media.AudioDecodeResult
import com.nanzhufeng.transcriber.data.media.EmbeddedSubtitleExtractor
import com.nanzhufeng.transcriber.data.media.HistoryMediaPreviewStore
import com.nanzhufeng.transcriber.data.media.MediaCodecAudioDecoder
import com.nanzhufeng.transcriber.data.media.PcmAudioArtifact
import com.nanzhufeng.transcriber.data.modelstore.OfficialModelCatalog
import com.nanzhufeng.transcriber.data.modelstore.AsrProviderId
import com.nanzhufeng.transcriber.data.result.StoredTranscript
import com.nanzhufeng.transcriber.data.result.StoredTranscriptionCheckpoint
import com.nanzhufeng.transcriber.data.result.TranscriptionCheckpointStore
import com.nanzhufeng.transcriber.data.result.TranscriptDocumentStore
import com.nanzhufeng.transcriber.data.task.SourceAccessMode
import com.nanzhufeng.transcriber.data.task.TaskMutationResult
import com.nanzhufeng.transcriber.data.task.TranscriptionTaskEntity
import com.nanzhufeng.transcriber.domain.export.TranscriptDocument
import com.nanzhufeng.transcriber.domain.export.TranscriptDocumentSegment
import com.nanzhufeng.transcriber.domain.export.TranscriptExportFormat
import com.nanzhufeng.transcriber.domain.export.TranscriptExportService
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationRecord
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationStatus
import com.nanzhufeng.transcriber.domain.invocation.AsrCostEstimator
import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import com.nanzhufeng.transcriber.domain.transcription.LocalTranscriptPunctuationRule
import com.nanzhufeng.transcriber.domain.transcription.TranscriptCompletionPolicy
import com.nanzhufeng.transcriber.engine.PcmTranscriptionCoordinator
import com.nanzhufeng.transcriber.engine.PcmTranscriptionResume
import com.nanzhufeng.transcriber.engine.PcmTranscriptionResult
import com.nanzhufeng.transcriber.engine.TranscriptSegment
import com.nanzhufeng.transcriber.engine.QueueModelSessionOwner
import com.nanzhufeng.transcriber.engine.SpeechEngineFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(markerClass = [UnstableApi::class])
class TranscriptionTaskRunner(
    private val context: Context,
    private val container: AppContainer,
    private val exportService: TranscriptExportService = TranscriptExportService(),
    private val documentStore: TranscriptDocumentStore = TranscriptDocumentStore(),
    private val checkpointStore: TranscriptionCheckpointStore = TranscriptionCheckpointStore(),
) {
    private val decoder = MediaCodecAudioDecoder(context.contentResolver)
    private val queueModelSession = QueueModelSessionOwner { provider, threadCount ->
        SpeechEngineFactory.create(provider, threadCount, container.qwen3AsrEngine)
    }
    private val historyMediaPreviewStore = HistoryMediaPreviewStore(context)
    private val embeddedSubtitleExtractor = EmbeddedSubtitleExtractor(context)
    @Volatile private var activeTaskId: String? = null

    fun cancel(taskId: String) {
        if (activeTaskId == taskId) queueModelSession.cancelCurrent()
    }

    fun close() = queueModelSession.close()

    suspend fun run(
        taskId: String,
        onProgress: (TaskRuntimeProgress) -> Unit,
    ): TaskRunOutcome {
        val reset = container.tasks.resetForExecution(taskId)
        val queued = (reset as? TaskMutationResult.Updated)?.task
            ?: return TaskRunOutcome.Ignored("任务不存在或当前不能重新开始")
        if (queued.state != TranscriptionTaskState.QUEUED.name) {
            return TaskRunOutcome.Ignored("任务当前状态为 ${queued.state}")
        }

        val sourceUri = Uri.parse(queued.sourceUri)
        val stagedSourcePath = queued.stagedInputPath
            ?.let { Paths.get(it) }
            ?.takeIf { Files.isRegularFile(it) }
        if (stagedSourcePath == null && !canReadSource(sourceUri)) {
            container.tasks.transition(
                id = taskId,
                target = TranscriptionTaskState.WAITING_INPUT,
                errorCode = "SOURCE_PERMISSION_LOST",
                userMessage = "原文件读取权限已失效，请重新选择文件",
                technicalDetail = "sourceAccessMode=${queued.sourceAccessMode}",
            )
            return TaskRunOutcome.Waiting("需要重新选择原文件")
        }

        val taskModel = OfficialModelCatalog.find(queued.modelId)
        if (taskModel == null || taskModel.manifest.version != queued.modelVersion) {
            container.tasks.transition(
                id = taskId,
                target = TranscriptionTaskState.WAITING_MODEL,
                errorCode = "MODEL_NOT_SUPPORTED",
                userMessage = "该任务使用的模型版本已不受支持，请在设置里重新选择模型后新建任务",
                technicalDetail = "modelId=${queued.modelId}, version=${queued.modelVersion}",
            )
            return TaskRunOutcome.Waiting("需要重新选择模型")
        }
        val usesQwenApi = taskModel.provider == AsrProviderId.QWEN3_ASR_API
        if (usesQwenApi && !container.textApiCredentials.hasApiKey()) {
            container.tasks.transition(
                id = taskId,
                target = TranscriptionTaskState.FAILED,
                errorCode = "QWEN_API_KEY_MISSING",
                userMessage = "高精度 Qwen 未配置 API Key，请在设置中保存后重试",
            )
            return TaskRunOutcome.Failed("高精度 Qwen 未配置 API Key")
        }
        val taskDirectory = context.noBackupFilesDir.resolve("tasks/$taskId").toPath()
        val pcmPath = taskDirectory.resolve("decoded.pcm")
        val checkpointPath = taskDirectory.resolve("checkpoint.json")
        val checkpointKey = listOf(
            queued.sourceUri,
            queued.sourceFingerprint.orEmpty(),
            queued.modelId,
            queued.modelVersion,
            taskModel.provider.name,
            taskModel.manifest.engineVersion,
            taskModel.capabilities.recommendedChunkMillis.toString(),
            queued.language.orEmpty(),
            queued.threadCount.toString(),
        ).joinToString("\u001f")
        var state = TranscriptionTaskState.QUEUED
        var completed = false
        try {
            withContext(Dispatchers.IO) {
                Files.createDirectories(taskDirectory)
                requireUpdated(
                    container.tasks.transition(
                        id = taskId,
                        target = TranscriptionTaskState.PREPARING,
                        userMessage = "正在检查音轨和可恢复断点",
                    ),
                )
            }
            state = TranscriptionTaskState.PREPARING
            val subtitleStart = SystemClock.elapsedRealtime()
            val embeddedSubtitles = withContext(Dispatchers.IO) {
                embeddedSubtitleExtractor.extract(
                    sourceUri = sourceUri,
                    stagedSourcePath = stagedSourcePath,
                    preferredLanguage = queued.language,
                )
            }
            if (embeddedSubtitles != null) {
                val durationMillis = embeddedSubtitles.durationMillis.coerceAtLeast(1L)
                val extractionMillis = SystemClock.elapsedRealtime() - subtitleStart
                withContext(Dispatchers.IO) {
                    requireUpdated(
                        container.tasks.transition(
                            id = taskId,
                            target = TranscriptionTaskState.TRANSCRIBING,
                            progressMillis = durationMillis,
                            totalDurationMillis = durationMillis,
                            userMessage = "检测到可读内嵌字幕，正在直接提取文字",
                        ),
                    )
                }
                state = TranscriptionTaskState.TRANSCRIBING
                onProgress(TaskRuntimeProgress("正在直接提取内嵌字幕", durationMillis, durationMillis))
                val rawDocument = TranscriptDocument(
                    title = queued.sourceDisplayName.substringBeforeLast('.').ifBlank { "南枫转写结果" },
                    segments = embeddedSubtitles.segments,
                )
                val performanceSummary = "文字来源：内嵌字幕｜提取 ${formatSeconds(extractionMillis)}｜" +
                    "${embeddedSubtitles.segments.size} 段"
                withContext(Dispatchers.IO) {
                    requireUpdated(
                        container.tasks.transition(
                            id = taskId,
                            target = TranscriptionTaskState.EXPORTING,
                            progressMillis = durationMillis,
                            totalDurationMillis = durationMillis,
                            userMessage = "正在保存内嵌字幕文字",
                        ),
                    )
                }
                state = TranscriptionTaskState.EXPORTING
                val documentPath = persistCompletedDocument(
                    queued = queued,
                    sourceUri = sourceUri,
                    stagedSourcePath = stagedSourcePath,
                    taskDirectory = taskDirectory,
                    rawDocument = rawDocument,
                    detectedLanguage = embeddedSubtitles.language,
                    performanceSummary = performanceSummary,
                    durationMillis = durationMillis,
                    onProgress = onProgress,
                )
                completed = true
                return TaskRunOutcome.Completed(documentPath)
            }
            val inspection = taskModel.takeIf { it.requiresLocalCache }?.let { model ->
                withContext(Dispatchers.IO) { container.modelStore.inspect(model.manifest) }
            }
            val modelPath = if (taskModel.requiresLocalCache) inspection?.modelPath else withContext(Dispatchers.IO) {
                context.noBackupFilesDir.resolve("qwen3-asr-api.session").toPath().also { path ->
                    if (Files.notExists(path)) Files.write(path, "qwen3-asr-api".toByteArray())
                }
            }
            if (taskModel.requiresLocalCache && (inspection?.state != ModelInstallState.READY || modelPath == null)) {
                withContext(Dispatchers.IO) {
                    requireUpdated(
                        container.tasks.transition(
                            id = taskId,
                            target = TranscriptionTaskState.WAITING_MODEL,
                            errorCode = "MODEL_NOT_READY",
                            userMessage = "${taskModel.displayName} 尚未就绪，模型准备好后可继续",
                            technicalDetail = inspection?.reason,
                        ),
                    )
                }
                state = TranscriptionTaskState.WAITING_MODEL
                return TaskRunOutcome.Waiting("需要先准备 ${taskModel.displayName}")
            }
            val restoredCheckpoint = withContext(Dispatchers.IO) {
                checkpointStore.loadOrNull(checkpointPath)
                    ?.takeIf { it.isCompatible(checkpointKey, pcmPath) }
            }
            val artifact: PcmAudioArtifact
            val decodeMillis: Long
            val checkpoint: StoredTranscriptionCheckpoint
            if (restoredCheckpoint != null) {
                checkpoint = restoredCheckpoint
                artifact = checkpoint.toArtifact(pcmPath)
                decodeMillis = checkpoint.decodeMillis
                val resumedMillis = checkpoint.processedSamples * 1_000L / checkpoint.sampleRate
                container.tasks.updateProgress(
                    id = taskId,
                    expectedState = TranscriptionTaskState.PREPARING,
                    progressMillis = resumedMillis,
                    totalDurationMillis = checkpoint.durationMillis,
                    userMessage = "已找到 ${formatPercent(resumedMillis, checkpoint.durationMillis)} 断点，正在恢复",
                )
                onProgress(
                    TaskRuntimeProgress(
                        "正在恢复已有转写断点",
                        resumedMillis,
                        checkpoint.durationMillis,
                    ),
                )
            } else {
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(checkpointPath)
                    Files.deleteIfExists(checkpointPath.resolveSibling("${checkpointPath.fileName}.part"))
                    Files.deleteIfExists(pcmPath)
                }
                onProgress(TaskRuntimeProgress("正在准备音轨", 0L, null))
                val decodeStart = SystemClock.elapsedRealtime()
                var lastDecodeUpdate = 0L
                val progressCallback: suspend (com.nanzhufeng.transcriber.data.media.AudioDecodeProgress) -> Unit = { progress ->
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastDecodeUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                        lastDecodeUpdate = now
                        val processed = progress.presentationTimeMicros.coerceAtLeast(0L) / 1_000L
                        val total = progress.durationMicros?.takeIf { it > 0L }?.div(1_000L)
                        container.tasks.updateProgress(
                            id = taskId,
                            expectedState = TranscriptionTaskState.PREPARING,
                            progressMillis = processed,
                            totalDurationMillis = total,
                            userMessage = "正在准备音轨 ${formatPercent(processed, total)}",
                        )
                        onProgress(TaskRuntimeProgress("正在准备音轨", processed, total))
                    }
                }
                val decodeResult = if (stagedSourcePath != null) {
                    decoder.decodeFile(stagedSourcePath, pcmPath, progressCallback)
                } else {
                    decoder.decodeDocument(sourceUri, pcmPath, progressCallback)
                }
                decodeMillis = SystemClock.elapsedRealtime() - decodeStart
                if (decodeResult is AudioDecodeResult.Failure) {
                    failTask(
                        taskId,
                        state,
                        "AUDIO_DECODE_FAILED",
                        decodeResult.message,
                        decodeResult.technicalDetail,
                    )
                    return TaskRunOutcome.Failed(decodeResult.message)
                }
                artifact = (decodeResult as AudioDecodeResult.Success).artifact
                checkpoint = StoredTranscriptionCheckpoint(
                    checkpointKey = checkpointKey,
                    sampleRate = artifact.sampleRate,
                    channels = artifact.channels,
                    sampleCount = artifact.sampleCount,
                    durationMillis = artifact.durationMillis,
                    pcmBytes = withContext(Dispatchers.IO) { Files.size(pcmPath) },
                    processedSamples = 0L,
                    detectedLanguage = null,
                    decodeMillis = decodeMillis,
                    modelLoadMillis = 0L,
                    transcribeMillis = 0L,
                    segments = emptyList(),
                )
                withContext(Dispatchers.IO) { checkpointStore.save(checkpointPath, checkpoint) }
            }

            val resumedMillis = checkpoint.processedSamples * 1_000L / artifact.sampleRate

            withContext(Dispatchers.IO) {
                requireUpdated(
                    container.tasks.transition(
                        id = taskId,
                        target = TranscriptionTaskState.TRANSCRIBING,
                        progressMillis = resumedMillis,
                        totalDurationMillis = artifact.durationMillis,
                        userMessage = if (checkpoint.processedSamples > 0L) {
                            "正在从 ${formatPercent(resumedMillis, artifact.durationMillis)} 断点继续"
                        } else if (usesQwenApi) {
                            "正在连接千问 Qwen3-ASR"
                        } else {
                            "正在从长期缓存加载 ${taskModel.displayName}"
                        },
                    ),
                )
            }
            state = TranscriptionTaskState.TRANSCRIBING
            onProgress(
                TaskRuntimeProgress(
                    if (usesQwenApi) "正在连接千问 Qwen3-ASR" else "正在加载长期缓存模型",
                    resumedMillis,
                    artifact.durationMillis,
                ),
            )

            val effectiveThreadCount = queued.threadCount.takeIf { it > 0 }
                ?: Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            activeTaskId = taskId
            val modelLoadStart = SystemClock.elapsedRealtime()
            val acquisition = queueModelSession.acquire(
                provider = taskModel.provider,
                modelPath = requireNotNull(modelPath),
                threadCount = effectiveThreadCount,
            )
            val modelLoadMillis = SystemClock.elapsedRealtime() - modelLoadStart
            val engine = acquisition.engine
            val loadedModel = acquisition.model
            engine.prepare(loadedModel)
            val totalModelLoadMillis = checkpoint.modelLoadMillis + modelLoadMillis
            val modelSessionLabel = when {
                usesQwenApi -> "千问服务连接"
                acquisition.reused -> "模型热复用"
                else -> "模型加载"
            }

            container.tasks.updateProgress(
                id = taskId,
                expectedState = TranscriptionTaskState.TRANSCRIBING,
                progressMillis = resumedMillis,
                totalDurationMillis = artifact.durationMillis,
                userMessage = if (checkpoint.processedSamples > 0L) {
                    "已从 ${formatPercent(resumedMillis, artifact.durationMillis)} 断点恢复；$modelSessionLabel，正在${if (usesQwenApi) "调用千问 Qwen3-ASR" else "本机转写"}"
                } else if (usesQwenApi) {
                    "$modelSessionLabel；正在向千问 Qwen3-ASR 发送音频片段"
                } else {
                    "$modelSessionLabel；正在本机转写，音视频不会上传"
                },
            )
            onProgress(
                TaskRuntimeProgress(
                    message = if (usesQwenApi) {
                        "$modelSessionLabel；正在向千问 Qwen3-ASR 发送音频片段"
                    } else {
                        "$modelSessionLabel；正在本机转写"
                    },
                    processedMillis = resumedMillis,
                    totalMillis = artifact.durationMillis,
                ),
            )

            val transcribeStart = SystemClock.elapsedRealtime()
            val baseTranscribeMillis = checkpoint.transcribeMillis
            val transcription = PcmTranscriptionCoordinator(
                engine = engine,
                chunkDurationMillis = taskModel.capabilities.recommendedChunkMillis,
            ).transcribe(
                artifact = artifact,
                model = loadedModel,
                language = queued.language,
                resume = PcmTranscriptionResume(
                    processedSamples = checkpoint.processedSamples,
                    detectedLanguage = checkpoint.detectedLanguage,
                    segments = checkpoint.segments.map { segment ->
                        TranscriptSegment(segment.startMillis, segment.endMillis, segment.text)
                    },
                ),
                onCheckpoint = { runtimeCheckpoint ->
                    checkpointStore.save(
                        checkpointPath,
                        checkpoint.copy(
                            processedSamples = runtimeCheckpoint.processedSamples,
                            detectedLanguage = runtimeCheckpoint.detectedLanguage,
                            modelLoadMillis = totalModelLoadMillis,
                            transcribeMillis = baseTranscribeMillis +
                                (SystemClock.elapsedRealtime() - transcribeStart),
                            segments = runtimeCheckpoint.segments.map { segment ->
                                TranscriptDocumentSegment(
                                    startMillis = segment.startMillis,
                                    endMillis = segment.endMillis,
                                    text = segment.text,
                                )
                            },
                        ),
                    )
                },
                onProgress = { progress ->
                    container.tasks.updateProgress(
                        id = taskId,
                        expectedState = TranscriptionTaskState.TRANSCRIBING,
                        progressMillis = progress.processedMillis,
                        totalDurationMillis = progress.totalMillis,
                        userMessage = if (usesQwenApi) {
                            "Qwen3-ASR 已完成 ${progress.segmentCount} 个片段 ${formatPercent(progress.processedMillis, progress.totalMillis)}"
                        } else {
                            "已转写 ${progress.segmentCount} 个片段 ${formatPercent(progress.processedMillis, progress.totalMillis)}"
                        },
                    )
                    onProgress(
                        TaskRuntimeProgress(
                            message = if (usesQwenApi) {
                                "千问 Qwen3-ASR 正在转写，已完成 ${progress.segmentCount} 个片段"
                            } else {
                                "正在本机转写，已完成 ${progress.segmentCount} 个片段"
                            },
                            processedMillis = progress.processedMillis,
                            totalMillis = progress.totalMillis,
                        ),
                    )
                },
            )
            val attemptTranscribeMillis = SystemClock.elapsedRealtime() - transcribeStart
            val totalTranscribeMillis = baseTranscribeMillis + attemptTranscribeMillis
            if (transcription is PcmTranscriptionResult.Failure) {
                if (usesQwenApi) {
                    recordQwenFileInvocation(
                        task = queued,
                        status = AsrInvocationStatus.FAILED,
                        durationMillis = totalTranscribeMillis,
                        requestCount = transcription.requestCount,
                        billableAudioMillis = transcription.billableAudioMillis,
                        inputTokens = transcription.inputTokens,
                        outputTokens = transcription.outputTokens,
                        totalTokens = transcription.totalTokens,
                        errorCode = transcription.errorCode,
                    )
                }
                failTask(
                    taskId,
                    state,
                    transcription.errorCode,
                    transcription.message,
                    transcription.technicalDetail,
                )
                return TaskRunOutcome.Failed(transcription.message)
            }

            val success = transcription as PcmTranscriptionResult.Success
            if (usesQwenApi) {
                recordQwenFileInvocation(
                    task = queued,
                    status = AsrInvocationStatus.SUCCEEDED,
                    durationMillis = totalTranscribeMillis,
                    requestCount = success.requestCount,
                    billableAudioMillis = success.billableAudioMillis,
                    inputTokens = success.inputTokens,
                    outputTokens = success.outputTokens,
                    totalTokens = success.totalTokens,
                )
            }
            if (!TranscriptCompletionPolicy.hasReadableSpeech(success.transcript.segments.map(TranscriptSegment::text))) {
                withContext(Dispatchers.IO) {
                    requireUpdated(
                        container.tasks.transition(
                            id = taskId,
                            target = TranscriptionTaskState.NO_SPEECH,
                            progressMillis = artifact.durationMillis,
                            totalDurationMillis = artifact.durationMillis,
                            errorCode = "NO_SPEECH_DETECTED",
                            userMessage = "未识别到可读语音，可检查音轨、语言或更换模型后重试",
                            technicalDetail = "segments=${success.transcript.segments.size}, durationMillis=${artifact.durationMillis}",
                        ),
                    )
                }
                state = TranscriptionTaskState.NO_SPEECH
                return TaskRunOutcome.NoSpeech("未识别到可读语音")
            }
            val rawDocument = TranscriptDocument(
                title = queued.sourceDisplayName.substringBeforeLast('.').ifBlank { "南枫转写结果" },
                segments = success.transcript.segments.mapIndexed { index, segment ->
                    TranscriptDocumentSegment(
                        startMillis = segment.startMillis,
                        endMillis = segment.endMillis,
                        text = if (
                            taskModel.provider == AsrProviderId.SENSEVOICE &&
                            index == success.transcript.segments.lastIndex
                        ) {
                            LocalTranscriptPunctuationRule.ensureFinalTerminal(segment.text)
                        } else {
                            segment.text
                        },
                    )
                },
            )
            val realTimeFactor = if (artifact.durationMillis > 0L) {
                totalTranscribeMillis.toDouble() / artifact.durationMillis.toDouble()
            } else {
                0.0
            }
            val performanceSummary = if (checkpoint.processedSamples > 0L) {
                "Provider ${taskModel.provider.name}｜${taskModel.manifest.engineVersion}｜" +
                    "分块 ${taskModel.capabilities.recommendedChunkMillis / 1_000} 秒｜" +
                    "解码 ${formatSeconds(decodeMillis)}｜${modelSessionLabel}累计 ${formatSeconds(totalModelLoadMillis)}｜" +
                    "转写累计 ${formatSeconds(totalTranscribeMillis)}｜实时系数 ${"%.2f".format(realTimeFactor)}｜" +
                    "从 ${formatPercent(resumedMillis, artifact.durationMillis)} 断点续写"
            } else {
                "Provider ${taskModel.provider.name}｜${taskModel.manifest.engineVersion}｜" +
                    "分块 ${taskModel.capabilities.recommendedChunkMillis / 1_000} 秒｜" +
                    "解码 ${formatSeconds(decodeMillis)}｜$modelSessionLabel ${formatSeconds(totalModelLoadMillis)}｜" +
                    "转写 ${formatSeconds(totalTranscribeMillis)}｜实时系数 ${"%.2f".format(realTimeFactor)}"
            }
            withContext(Dispatchers.IO) {
                requireUpdated(
                    container.tasks.transition(
                        id = taskId,
                        target = TranscriptionTaskState.EXPORTING,
                        progressMillis = artifact.durationMillis,
                        totalDurationMillis = artifact.durationMillis,
                        userMessage = "正在保存完整转写结果",
                    ),
                )
            }
            state = TranscriptionTaskState.EXPORTING
            val documentPath = persistCompletedDocument(
                queued = queued,
                sourceUri = sourceUri,
                stagedSourcePath = stagedSourcePath,
                taskDirectory = taskDirectory,
                rawDocument = rawDocument,
                detectedLanguage = success.transcript.detectedLanguage,
                performanceSummary = performanceSummary,
                durationMillis = artifact.durationMillis,
                onProgress = onProgress,
            )
            completed = true
            return TaskRunOutcome.Completed(documentPath)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            failTask(
                taskId,
                state,
                "WORKFLOW_FAILED",
                "转写未完成，请确认文件可正常播放后重试",
                error.stackTraceToString(),
            )
            return TaskRunOutcome.Failed("转写未完成，可从历史重试")
        } finally {
            if (activeTaskId == taskId) {
                activeTaskId = null
            }
            if (completed) {
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(pcmPath)
                    Files.deleteIfExists(checkpointPath)
                    Files.deleteIfExists(checkpointPath.resolveSibling("${checkpointPath.fileName}.part"))
                    stagedSourcePath?.let { Files.deleteIfExists(it) }
                }
            }
        }
    }

    private suspend fun persistCompletedDocument(
        queued: TranscriptionTaskEntity,
        sourceUri: Uri,
        stagedSourcePath: Path?,
        taskDirectory: Path,
        rawDocument: TranscriptDocument,
        detectedLanguage: String?,
        performanceSummary: String,
        durationMillis: Long,
        onProgress: (TaskRuntimeProgress) -> Unit,
    ): Path {
        val document = rawDocument.copy(polishedText = null)
        val documentPath = taskDirectory.resolve("transcript.json")
        val plainTextPath = taskDirectory.resolve("transcript.txt")

        withContext(Dispatchers.IO) {
            historyMediaPreviewStore.ensureVideoThumbnail(
                taskId = queued.id,
                sourceUri = sourceUri,
                stagedSourcePath = stagedSourcePath,
                displayName = queued.sourceDisplayName,
            )
            documentStore.save(
                documentPath,
                StoredTranscript(
                    document = document,
                    detectedLanguage = detectedLanguage,
                    performanceSummary = performanceSummary,
                ),
            )
            Files.newOutputStream(plainTextPath).use { output ->
                exportService.export(document, TranscriptExportFormat.TXT, output)
            }
            requireUpdated(
                container.tasks.transition(
                    id = queued.id,
                    target = TranscriptionTaskState.COMPLETED,
                    progressMillis = durationMillis,
                    totalDurationMillis = durationMillis,
                    outputUri = documentPath.toUri().toString(),
                    userMessage = listOfNotNull(
                        if (performanceSummary.startsWith("文字来源：内嵌字幕")) {
                            "已直接提取内嵌字幕，完整结果已保存"
                        } else {
                            "转写完成，完整结果已保存"
                        },
                    ).joinToString("；"),
                    technicalDetail = performanceSummary,
                ),
            )
        }
        return documentPath
    }

    private fun StoredTranscriptionCheckpoint.toArtifact(pcmPath: Path): PcmAudioArtifact =
        PcmAudioArtifact(
            path = pcmPath,
            sampleRate = sampleRate,
            channels = channels,
            sampleCount = sampleCount,
            durationMillis = durationMillis,
        )

    private fun canReadSource(uri: Uri): Boolean = runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
    }.getOrDefault(false)

    private suspend fun failTask(
        taskId: String,
        currentState: TranscriptionTaskState,
        errorCode: String,
        userMessage: String,
        technicalDetail: String?,
    ) {
        if (currentState in ACTIVE_STATES) {
            container.tasks.transition(
                id = taskId,
                target = TranscriptionTaskState.FAILED,
                errorCode = errorCode,
                userMessage = userMessage,
                technicalDetail = technicalDetail,
            )
        }
    }

    /**
     * Internal chunk requests are aggregated before this boundary. One source-file run produces
     * at most one ledger fact, so settings never exposes individual audio chunks.
     */
    private suspend fun recordQwenFileInvocation(
        task: TranscriptionTaskEntity,
        status: AsrInvocationStatus,
        durationMillis: Long,
        requestCount: Int,
        billableAudioMillis: Long,
        inputTokens: Long?,
        outputTokens: Long?,
        totalTokens: Long?,
        errorCode: String? = null,
    ) {
        if (requestCount <= 0) return
        val estimatedCost = AsrCostEstimator.estimateQwenAsrCn(billableAudioMillis)
        runCatching {
            container.asrInvocations.record(
                AsrInvocationRecord(
                    id = UUID.randomUUID().toString(),
                    taskId = task.id,
                    sourceDisplayName = task.sourceDisplayName,
                    providerId = "QWEN",
                    modelId = "qwen3-asr-flash",
                    completedAtMillis = System.currentTimeMillis(),
                    durationMillis = durationMillis.coerceAtLeast(0L),
                    requestCount = requestCount,
                    billableAudioMillis = billableAudioMillis,
                    status = status,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    totalTokens = totalTokens,
                    costPriceVersion = estimatedCost?.priceVersion,
                    costCurrencyCode = estimatedCost?.currencyCode,
                    estimatedCostMicros = estimatedCost?.estimatedCostMicros,
                    errorCode = errorCode,
                ),
            )
        }
    }

    private fun requireUpdated(result: TaskMutationResult) {
        require(result is TaskMutationResult.Updated) {
            when (result) {
                is TaskMutationResult.Rejected -> result.reason
                is TaskMutationResult.NotFound -> "任务不存在：${result.id}"
                is TaskMutationResult.Updated -> ""
            }
        }
    }

    private fun formatPercent(processedMillis: Long, totalMillis: Long?): String {
        if (totalMillis == null || totalMillis <= 0L) return ""
        val percent = (processedMillis.toDouble() / totalMillis * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()
        return "$percent%"
    }

    private fun formatSeconds(millis: Long): String =
        String.format(Locale.ROOT, "%.2f 秒", millis / 1_000.0)

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MS = 1_000L
        val ACTIVE_STATES = setOf(
            TranscriptionTaskState.PREPARING,
            TranscriptionTaskState.TRANSCRIBING,
            TranscriptionTaskState.EXPORTING,
        )
    }
}

data class TaskRuntimeProgress(
    val message: String,
    val processedMillis: Long,
    val totalMillis: Long?,
) {
    val percentage: Int?
        get() = totalMillis?.takeIf { it > 0L }?.let {
            (processedMillis.toDouble() / it * 100.0).coerceIn(0.0, 100.0).roundToInt()
        }
}

sealed interface TaskRunOutcome {
    data class Completed(val documentPath: Path) : TaskRunOutcome
    data class NoSpeech(val message: String) : TaskRunOutcome
    data class Failed(val message: String) : TaskRunOutcome
    data class Waiting(val message: String) : TaskRunOutcome
    data class Ignored(val message: String) : TaskRunOutcome
}
