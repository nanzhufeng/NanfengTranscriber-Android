package com.nanzhufeng.transcriber.engine

import com.nanzhufeng.transcriber.data.media.PcmAudioArtifact
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.coroutines.coroutineContext

class PcmTranscriptionCoordinator(
    private val engine: SpeechEngine,
    private val chunkDurationMillis: Long = 5 * 60 * 1_000L,
) {
    init {
        require(chunkDurationMillis > 0L) { "转写分块时长必须大于 0" }
    }

    suspend fun transcribe(
        artifact: PcmAudioArtifact,
        model: LoadedModel,
        language: String?,
        resume: PcmTranscriptionResume = PcmTranscriptionResume(),
        onCheckpoint: suspend (PcmTranscriptionCheckpoint) -> Unit = {},
        onProgress: suspend (PcmTranscriptionProgress) -> Unit = {},
    ): PcmTranscriptionResult = withContext(Dispatchers.IO) {
        require(artifact.channels == 1) { "转写输入必须是单声道 PCM" }
        require(artifact.sampleRate == 16_000) { "转写输入必须是 16 kHz PCM" }
        require(resume.processedSamples in 0L..artifact.sampleCount) {
            "转写断点超出 PCM 范围"
        }

        val chunkSamples = (artifact.sampleRate * chunkDurationMillis / 1_000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        val segments = resume.segments.toMutableList()
        var detectedLanguage: String? = resume.detectedLanguage
        var processedSamples = resume.processedSamples
        var requestCount = 0
        var billableAudioMillis = 0L
        var inputTokens: Long? = null
        var outputTokens: Long? = null
        var totalTokens: Long? = null

        try {
            FileChannel.open(artifact.path, StandardOpenOption.READ).use { channel ->
                channel.position(processedSamples * PCM16_BYTES_PER_SAMPLE)
                while (processedSamples < artifact.sampleCount) {
                    coroutineContext.ensureActive()
                    val remaining = artifact.sampleCount - processedSamples
                    val samplesToRead = minOf(chunkSamples.toLong(), remaining).toInt()
                    val samples = readPcm16(channel, samplesToRead)
                    if (samples.isEmpty()) break

                    val chunkStartMillis = processedSamples * 1_000L / artifact.sampleRate
                    val transcript = engine.transcribe(model, samples, language)
                    transcript.invocationUsage?.let { usage ->
                        requestCount += usage.requestCount
                        billableAudioMillis += usage.billableAudioMillis
                        inputTokens = inputTokens.plusReported(usage.inputTokens)
                        outputTokens = outputTokens.plusReported(usage.outputTokens)
                        totalTokens = totalTokens.plusReported(usage.totalTokens)
                    }
                    if (detectedLanguage == null) detectedLanguage = transcript.detectedLanguage
                    transcript.segments.forEach { segment ->
                        segments += segment.copy(
                            startMillis = segment.startMillis + chunkStartMillis,
                            endMillis = segment.endMillis + chunkStartMillis,
                        )
                    }
                    processedSamples += samples.size
                    onCheckpoint(
                        PcmTranscriptionCheckpoint(
                            processedSamples = processedSamples,
                            detectedLanguage = detectedLanguage,
                            segments = segments.toList(),
                        ),
                    )
                    onProgress(
                        PcmTranscriptionProgress(
                            processedMillis = processedSamples * 1_000L / artifact.sampleRate,
                            totalMillis = artifact.durationMillis,
                            segmentCount = segments.size,
                        ),
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: EngineTranscriptionException) {
            return@withContext PcmTranscriptionResult.Failure(
                message = error.userMessage,
                technicalDetail = error.safeTechnicalDetail,
                errorCode = error.errorCode,
                requestCount = requestCount + if (error.providerRequestAttempted) 1 else 0,
                billableAudioMillis = billableAudioMillis + error.providerAttemptedAudioMillis,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                totalTokens = totalTokens,
            )
        } catch (error: IOException) {
            return@withContext PcmTranscriptionResult.Failure(
                message = "读取转写音频失败，可重新准备此任务",
                technicalDetail = error.message,
                errorCode = "PCM_READ_FAILED",
            )
        }

        if (processedSamples != artifact.sampleCount) {
            return@withContext PcmTranscriptionResult.Failure(
                message = "PCM 文件提前结束，任务需要重新准备",
                technicalDetail = "expected=${artifact.sampleCount}, actual=$processedSamples",
                errorCode = "PCM_TRUNCATED",
            )
        }
        PcmTranscriptionResult.Success(
            transcript = EngineTranscript(
                detectedLanguage = detectedLanguage,
                segments = segments,
            ),
            processedSamples = processedSamples,
            requestCount = requestCount,
            billableAudioMillis = billableAudioMillis,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = totalTokens,
        )
    }

    private fun readPcm16(channel: FileChannel, sampleCount: Int): FloatArray {
        val bytes = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        while (bytes.hasRemaining()) {
            val read = channel.read(bytes)
            if (read < 0) break
        }
        bytes.flip()
        if (bytes.remaining() % 2 != 0) throw IOException("PCM 字节没有按 16-bit 对齐")
        return FloatArray(bytes.remaining() / 2) { bytes.short / 32768.0f }
    }

    private companion object {
        const val PCM16_BYTES_PER_SAMPLE = 2L
    }
}

private fun Long?.plusReported(next: Long?): Long? = when {
    next == null -> this
    this == null -> next
    else -> this + next
}

data class PcmTranscriptionResume(
    val processedSamples: Long = 0L,
    val detectedLanguage: String? = null,
    val segments: List<TranscriptSegment> = emptyList(),
)

data class PcmTranscriptionCheckpoint(
    val processedSamples: Long,
    val detectedLanguage: String?,
    val segments: List<TranscriptSegment>,
)

data class PcmTranscriptionProgress(
    val processedMillis: Long,
    val totalMillis: Long,
    val segmentCount: Int,
)

sealed interface PcmTranscriptionResult {
    data class Success(
        val transcript: EngineTranscript,
        val processedSamples: Long,
        val requestCount: Int = 0,
        val billableAudioMillis: Long = 0L,
        val inputTokens: Long? = null,
        val outputTokens: Long? = null,
        val totalTokens: Long? = null,
    ) : PcmTranscriptionResult

    data class Failure(
        val message: String,
        val technicalDetail: String? = null,
        val errorCode: String = "TRANSCRIPTION_FAILED",
        val requestCount: Int = 0,
        val billableAudioMillis: Long = 0L,
        val inputTokens: Long? = null,
        val outputTokens: Long? = null,
        val totalTokens: Long? = null,
    ) : PcmTranscriptionResult
}
