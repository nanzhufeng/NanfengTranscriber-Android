package com.nanzhufeng.transcriber.engine

import com.nanzhufeng.transcriber.data.media.PcmAudioArtifact
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path

class PcmTranscriptionCoordinatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun longPcmIsReadInBoundedChunksAndTimestampsBecomeAbsolute() = runBlocking {
        val sampleCount = 40_000
        val pcm = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(sampleCount) { pcm.putShort((it % Short.MAX_VALUE).toShort()) }
        val pcmPath = temporaryFolder.newFile("audio.pcm").toPath()
        Files.write(pcmPath, pcm.array())
        val engine = RecordingSpeechEngine()
        val coordinator = PcmTranscriptionCoordinator(
            engine = engine,
            chunkDurationMillis = 1_000L,
        )
        val progress = mutableListOf<PcmTranscriptionProgress>()

        val result = coordinator.transcribe(
            artifact = PcmAudioArtifact(
                path = pcmPath,
                sampleRate = 16_000,
                channels = 1,
                sampleCount = sampleCount.toLong(),
                durationMillis = 2_500L,
            ),
            model = LoadedModel(pcmPath, 1L),
            language = "zh",
            onProgress = progress::add,
        )

        assertTrue(result is PcmTranscriptionResult.Success)
        result as PcmTranscriptionResult.Success
        assertEquals(listOf(16_000, 16_000, 8_000), engine.receivedChunkSizes)
        assertEquals(listOf(0L, 1_000L, 2_000L), result.transcript.segments.map { it.startMillis })
        assertEquals(2_500L, progress.last().processedMillis)
        assertEquals(sampleCount.toLong(), result.processedSamples)
    }

    @Test
    fun resumeStartsAtTheCheckpointAndPreservesExistingSegments() = runBlocking {
        val sampleCount = 40_000
        val pcm = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(sampleCount) { pcm.putShort((it % Short.MAX_VALUE).toShort()) }
        val pcmPath = temporaryFolder.newFile("resume.pcm").toPath()
        Files.write(pcmPath, pcm.array())
        val engine = RecordingSpeechEngine()
        val checkpoints = mutableListOf<PcmTranscriptionCheckpoint>()

        val result = PcmTranscriptionCoordinator(
            engine = engine,
            chunkDurationMillis = 1_000L,
        ).transcribe(
            artifact = PcmAudioArtifact(
                path = pcmPath,
                sampleRate = 16_000,
                channels = 1,
                sampleCount = sampleCount.toLong(),
                durationMillis = 2_500L,
            ),
            model = LoadedModel(pcmPath, 1L),
            language = "en",
            resume = PcmTranscriptionResume(
                processedSamples = 16_000L,
                detectedLanguage = "en",
                segments = listOf(TranscriptSegment(0L, 1_000L, "restored")),
            ),
            onCheckpoint = checkpoints::add,
        )

        assertTrue(result is PcmTranscriptionResult.Success)
        result as PcmTranscriptionResult.Success
        assertEquals(listOf(16_000, 8_000), engine.receivedChunkSizes)
        assertEquals(
            listOf("restored", "chunk-1", "chunk-2"),
            result.transcript.segments.map { it.text },
        )
        assertEquals(listOf(0L, 1_000L, 2_000L), result.transcript.segments.map { it.startMillis })
        assertEquals(sampleCount.toLong(), checkpoints.last().processedSamples)
    }

    private class RecordingSpeechEngine : SpeechEngine {
        val receivedChunkSizes = mutableListOf<Int>()

        override suspend fun loadModel(modelPath: Path): LoadedModel = LoadedModel(modelPath, 1L)

        override suspend fun transcribe(
            model: LoadedModel,
            samples: FloatArray,
            language: String?,
        ): EngineTranscript {
            receivedChunkSizes += samples.size
            return EngineTranscript(
                detectedLanguage = language,
                segments = listOf(
                    TranscriptSegment(
                        startMillis = 0L,
                        endMillis = samples.size * 1_000L / 16_000L,
                        text = "chunk-${receivedChunkSizes.size}",
                    ),
                ),
            )
        }

        override fun releaseModel(model: LoadedModel) = Unit
        override fun close() = Unit
    }
}
