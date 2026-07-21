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
import com.nanzhufeng.transcriber.data.output.AndroidTranscriptOutputStore
import com.nanzhufeng.transcriber.data.postprocess.TextPostProcessConfig
import com.nanzhufeng.transcriber.data.postprocess.TextPostProcessException
import com.nanzhufeng.transcriber.data.result.StoredTranscript
import com.nanzhufeng.transcriber.data.result.StoredTranscriptionCheckpoint
import com.nanzhufeng.transcriber.data.result.TranscriptionCheckpointStore
import com.nanzhufeng.transcriber.data.result.TranscriptDocumentStore
import com.nanzhufeng.transcriber.data.task.SourceAccessMode
import com.nanzhufeng.transcriber.data.task.OutputConflictPolicy
import com.nanzhufeng.transcriber.data.task.TranscriptionOutputFormat
import com.nanzhufeng.transcriber.data.task.TaskMutationResult
import com.nanzhufeng.transcriber.data.task.TranscriptionTaskEntity
import com.nanzhufeng.transcriber.domain.export.TranscriptDocument
import com.nanzhufeng.transcriber.domain.export.TranscriptDocumentSegment
import com.nanzhufeng.transcriber.domain.export.TranscriptExportFormat
import com.nanzhufeng.transcriber.domain.export.TranscriptExportService
import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import com.nanzhufeng.transcriber.domain.transcription.TranscriptCompletionPolicy
import com.nanzhufeng.transcriber.engine.PcmTranscriptionCoordinator
import com.nanzhufeng.transcriber.engine.PcmTranscriptionResume
import com.nanzhufeng.transcriber.engine.PcmTranscriptionResult
import com.nanzhufeng.transcriber.engine.TranscriptSegment
import com.nanzhufeng.transcriber.engine.QueueModelSessionOwner
import com.nanzhufeng.transcriber.engine.WhisperCppEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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
    private val queueModelSession = QueueModelSessionOwner { threadCount ->
        WhisperCppEngine(useGpu = false, threadCount = threadCount)
    }
    private val automaticOutputStore = AndroidTranscriptOutputStore(context.contentResolver, exportService)
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
        val taskDirectory = context.noBackupFilesDir.resolve("tasks/$taskId").toPath()
        val pcmPath = taskDirectory.resolve("decoded.pcm")
        val checkpointPath = taskDirectory.resolve("checkpoint.json")
        val checkpointKey = listOf(
            queued.sourceUri,
            queued.sourceFingerprint.orEmpty(),
            queued.modelId,
            queued.modelVersion,
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
                val performanceSummary = "文字来源：内嵌字幕｜提取 ${extractionMillis} ms｜" +
                    "${embeddedSubtitles.segments.size} 段"
                withContext(Dispatchers.IO) {
                    requireUpdated(
                        container.tasks.transition(
                            id = taskId,
                            target = TranscriptionTaskState.EXPORTING,
                            progressMillis = durationMillis,
                            totalDurationMillis = durationMillis,
                            userMessage = if (queued.postProcessEnabled) {
                                "正在翻译润色字幕文字"
                            } else {
                                "正在保存内嵌字幕文字"
                            },
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
            val inspection = withContext(Dispatchers.IO) {
                container.modelStore.inspect(taskModel.manifest)
            }
            val modelPath = inspection.modelPath
            if (inspection.state != ModelInstallState.READY || modelPath == null) {
                withContext(Dispatchers.IO) {
                    requireUpdated(
                        container.tasks.transition(
                            id = taskId,
                            target = TranscriptionTaskState.WAITING_MODEL,
                            errorCode = "MODEL_NOT_READY",
                            userMessage = "${taskModel.displayName} 尚未就绪，模型准备好后可继续",
                            technicalDetail = inspection.reason,
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
                        } else {
                            "正在从长期缓存加载 ${taskModel.displayName}"
                        },
                    ),
                )
            }
            state = TranscriptionTaskState.TRANSCRIBING
            onProgress(
                TaskRuntimeProgress(
                    "正在加载长期缓存模型",
                    resumedMillis,
                    artifact.durationMillis,
                ),
            )

            val effectiveThreadCount = queued.threadCount.takeIf { it > 0 }
                ?: Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            activeTaskId = taskId
            val modelLoadStart = SystemClock.elapsedRealtime()
            val acquisition = queueModelSession.acquire(
                modelPath = modelPath,
                threadCount = effectiveThreadCount,
            )
            val modelLoadMillis = SystemClock.elapsedRealtime() - modelLoadStart
            val engine = acquisition.engine
            val loadedModel = acquisition.model
            engine.prepare(loadedModel)
            val totalModelLoadMillis = checkpoint.modelLoadMillis + modelLoadMillis
            val modelSessionLabel = if (acquisition.reused) "模型热复用" else "模型加载"

            container.tasks.updateProgress(
                id = taskId,
                expectedState = TranscriptionTaskState.TRANSCRIBING,
                progressMillis = resumedMillis,
                totalDurationMillis = artifact.durationMillis,
                userMessage = if (checkpoint.processedSamples > 0L) {
                    "已从 ${formatPercent(resumedMillis, artifact.durationMillis)} 断点恢复；$modelSessionLabel，正在本机转写"
                } else {
                    "$modelSessionLabel；正在本机转写，音视频不会上传"
                },
            )
            onProgress(
                TaskRuntimeProgress(
                    message = "$modelSessionLabel；正在本机转写",
                    processedMillis = resumedMillis,
                    totalMillis = artifact.durationMillis,
                ),
            )

            val transcribeStart = SystemClock.elapsedRealtime()
            val baseTranscribeMillis = checkpoint.transcribeMillis
            val transcription = PcmTranscriptionCoordinator(engine).transcribe(
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
                        userMessage = "已转写 ${progress.segmentCount} 个片段 ${formatPercent(progress.processedMillis, progress.totalMillis)}",
                    )
                    onProgress(
                        TaskRuntimeProgress(
                            message = "正在本机转写，已完成 ${progress.segmentCount} 个片段",
                            processedMillis = progress.processedMillis,
                            totalMillis = progress.totalMillis,
                        ),
                    )
                },
            )
            val attemptTranscribeMillis = SystemClock.elapsedRealtime() - transcribeStart
            val totalTranscribeMillis = baseTranscribeMillis + attemptTranscribeMillis
            if (transcription is PcmTranscriptionResult.Failure) {
                failTask(
                    taskId,
                    state,
                    "TRANSCRIPTION_FAILED",
                    transcription.message,
                    transcription.technicalDetail,
                )
                return TaskRunOutcome.Failed(transcription.message)
            }

            val success = transcription as PcmTranscriptionResult.Success
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
                segments = success.transcript.segments.map { segment ->
                    TranscriptDocumentSegment(
                        startMillis = segment.startMillis,
                        endMillis = segment.endMillis,
                        text = segment.text,
                    )
                },
            )
            val realTimeFactor = if (artifact.durationMillis > 0L) {
                totalTranscribeMillis.toDouble() / artifact.durationMillis.toDouble()
            } else {
                0.0
            }
            val performanceSummary = if (checkpoint.processedSamples > 0L) {
                "解码 ${decodeMillis} ms｜${modelSessionLabel}累计 ${totalModelLoadMillis} ms｜" +
                    "转写累计 ${totalTranscribeMillis} ms｜实时系数 ${"%.2f".format(realTimeFactor)}｜" +
                    "从 ${formatPercent(resumedMillis, artifact.durationMillis)} 断点续写"
            } else {
                "解码 ${decodeMillis} ms｜$modelSessionLabel ${totalModelLoadMillis} ms｜" +
                    "转写 ${totalTranscribeMillis} ms｜实时系数 ${"%.2f".format(realTimeFactor)}"
            }
            withContext(Dispatchers.IO) {
                requireUpdated(
                    container.tasks.transition(
                        id = taskId,
                        target = TranscriptionTaskState.EXPORTING,
                        progressMillis = artifact.durationMillis,
                        totalDurationMillis = artifact.durationMillis,
                        userMessage = if (queued.postProcessEnabled) {
                            "正在翻译润色转写文字"
                        } else {
                            "正在保存完整转写结果"
                        },
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
        var postProcessMessage: String? = null
        var postProcessTechnical: String? = null
        val polishedText = if (queued.postProcessEnabled) {
            val apiKey = withContext(Dispatchers.IO) { container.textApiCredentials.readApiKey() }
            if (apiKey == null) {
                postProcessMessage = "未找到 API Key，已保留原始转写"
                postProcessTechnical = "postprocess api key unavailable"
                null
            } else {
                try {
                    container.textPostProcessor.polishToSimplifiedChinese(
                        text = exportService.renderTxt(rawDocument),
                        config = TextPostProcessConfig(
                            baseUrl = queued.postProcessBaseUrl,
                            model = queued.postProcessModel,
                            apiKey = apiKey,
                        ),
                    ) { completedChunks, totalChunks ->
                        val displayIndex = (completedChunks + 1).coerceAtMost(totalChunks)
                        withContext(Dispatchers.IO) {
                            container.tasks.updateProgress(
                                id = queued.id,
                                expectedState = TranscriptionTaskState.EXPORTING,
                                progressMillis = durationMillis,
                                totalDurationMillis = durationMillis,
                                userMessage = "正在翻译润色 $displayIndex/$totalChunks",
                            )
                        }
                        onProgress(
                            TaskRuntimeProgress(
                                message = "正在翻译润色 $displayIndex/$totalChunks",
                                processedMillis = durationMillis,
                                totalMillis = durationMillis,
                            ),
                        )
                    }.also {
                        postProcessMessage = "翻译润色完成"
                        postProcessTechnical = "postprocess completed with ${queued.postProcessModel}"
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: TextPostProcessException) {
                    postProcessMessage = error.userMessage
                    postProcessTechnical = error.technicalDetail
                    null
                } catch (error: Exception) {
                    postProcessMessage = "翻译润色失败，已保留原始转写"
                    postProcessTechnical = "${error::class.java.simpleName}: ${error.message.orEmpty().take(200)}"
                    null
                }
            }
        } else {
            null
        }
        val document = rawDocument.copy(polishedText = polishedText)
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
            val automaticExportMessage = queued.exportDirectoryUri?.let { directoryUri ->
                runCatching {
                    val outputFormat = TranscriptionOutputFormat.valueOf(queued.outputFormat).toExportFormat()
                    val conflictPolicy = runCatching {
                        OutputConflictPolicy.valueOf(queued.outputConflictPolicy)
                    }.getOrDefault(OutputConflictPolicy.RENAME)
                    automaticOutputStore.export(
                        treeUri = Uri.parse(directoryUri),
                        taskId = queued.id,
                        sourceDisplayName = queued.sourceDisplayName,
                        document = document,
                        format = outputFormat,
                        conflictPolicy = conflictPolicy,
                    ).message
                }.getOrElse {
                    "默认目录导出失败，可在历史页重新导出"
                }
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
                        postProcessMessage,
                        automaticExportMessage,
                    ).joinToString("；"),
                    technicalDetail = listOfNotNull(performanceSummary, postProcessTechnical).joinToString("｜"),
                ),
            )
        }
        return documentPath
    }

    private fun TranscriptionOutputFormat.toExportFormat(): TranscriptExportFormat = when (this) {
        TranscriptionOutputFormat.TXT -> TranscriptExportFormat.TXT
        TranscriptionOutputFormat.MARKDOWN -> TranscriptExportFormat.MARKDOWN
        TranscriptionOutputFormat.SRT -> TranscriptExportFormat.SRT
        TranscriptionOutputFormat.DOCX -> TranscriptExportFormat.DOCX
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
