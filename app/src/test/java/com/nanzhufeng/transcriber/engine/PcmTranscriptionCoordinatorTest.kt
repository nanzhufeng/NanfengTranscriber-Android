package com.nanzhufeng.transcriber.engine

import com.nanzhufeng.transcriber.data.media.PcmAudioArtifact
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class PcmTranscriptionCoordinatorTest {
    @Test
    fun `aggregates internal provider segments into one file level usage`() = runBlocking {
        val pcmFile = Files.createTempFile("qwen3-asr-pcm", ".pcm")
        Files.write(pcmFile, ByteArray(64_000))
        try {
            val coordinator = PcmTranscriptionCoordinator(
                engine = object : SpeechEngine {
                    override suspend fun loadModel(modelPath: Path) = LoadedModel(modelPath, 0L)

                    override suspend fun transcribe(
                        model: LoadedModel,
                        samples: FloatArray,
                        language: String?,
                    ) = EngineTranscript(
                        detectedLanguage = "zh",
                        segments = listOf(TranscriptSegment(0L, 1_000L, "结果")),
                        invocationUsage = EngineInvocationUsage(
                            requestCount = 1,
                            billableAudioMillis = 1_000L,
                            totalTokens = 10L,
                        ),
                    )

                    override fun releaseModel(model: LoadedModel) = Unit
                    override fun close() = Unit
                },
                chunkDurationMillis = 1_000L,
            )

            val result = coordinator.transcribe(
                artifact = PcmAudioArtifact(pcmFile, 16_000, 1, 32_000, 2_000L),
                model = LoadedModel(pcmFile, 0L),
                language = null,
            ) as PcmTranscriptionResult.Success

            assertEquals(2, result.requestCount)
            assertEquals(2_000L, result.billableAudioMillis)
            assertEquals(20L, result.totalTokens)
        } finally {
            Files.deleteIfExists(pcmFile)
        }
    }

    @Test
    fun `preserves engine failure code and safe message`() = runBlocking {
        val pcmFile = Files.createTempFile("qwen3-asr-pcm", ".pcm")
        Files.write(pcmFile, ByteArray(32_000))
        try {
            val coordinator = PcmTranscriptionCoordinator(
                engine = object : SpeechEngine {
                    override suspend fun loadModel(modelPath: Path) = LoadedModel(modelPath, 0L)

                    override suspend fun transcribe(
                        model: LoadedModel,
                        samples: FloatArray,
                        language: String?,
                    ): EngineTranscript = throw EngineTranscriptionException(
                        userMessage = "千问服务限流或额度不足，请稍后重试",
                        errorCode = "QWEN_HTTP_429",
                        safeTechnicalDetail = "http=429",
                    )

                    override fun releaseModel(model: LoadedModel) = Unit
                    override fun close() = Unit
                },
            )

            val result = coordinator.transcribe(
                artifact = PcmAudioArtifact(
                    path = pcmFile,
                    sampleRate = 16_000,
                    channels = 1,
                    sampleCount = 16_000,
                    durationMillis = 1_000L,
                ),
                model = LoadedModel(pcmFile, 0L),
                language = null,
            ) as PcmTranscriptionResult.Failure

            assertEquals("QWEN_HTTP_429", result.errorCode)
            assertEquals("千问服务限流或额度不足，请稍后重试", result.message)
            assertEquals("http=429", result.technicalDetail)
            assertEquals(0, result.requestCount)
        } finally {
            Files.deleteIfExists(pcmFile)
        }
    }
}
