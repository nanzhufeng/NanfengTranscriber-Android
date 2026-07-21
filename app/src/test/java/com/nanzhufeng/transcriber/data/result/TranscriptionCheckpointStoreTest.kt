package com.nanzhufeng.transcriber.data.result

import com.nanzhufeng.transcriber.domain.export.TranscriptDocumentSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class TranscriptionCheckpointStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun checkpointRoundTripsAndValidatesThePreparedPcm() {
        val directory = temporaryFolder.root.toPath()
        val target = directory.resolve("checkpoint.json")
        val pcm = directory.resolve("decoded.pcm")
        Files.write(pcm, ByteArray(32_000))
        val expected = StoredTranscriptionCheckpoint(
            checkpointKey = "source-model-settings",
            sampleRate = 16_000,
            channels = 1,
            sampleCount = 16_000L,
            durationMillis = 1_000L,
            pcmBytes = 32_000L,
            processedSamples = 8_000L,
            detectedLanguage = "en",
            decodeMillis = 72L,
            modelLoadMillis = 164L,
            transcribeMillis = 1_200L,
            segments = listOf(TranscriptDocumentSegment(0L, 500L, "first")),
        )

        val store = TranscriptionCheckpointStore()
        store.save(target, expected)
        val restored = store.loadOrNull(target)

        assertEquals(expected, restored)
        assertTrue(requireNotNull(restored).isCompatible("source-model-settings", pcm))
        assertFalse(restored.isCompatible("different-source", pcm))
        assertFalse(Files.exists(target.resolveSibling("checkpoint.json.part")))
    }

    @Test
    fun malformedCheckpointIsIgnoredInsteadOfBreakingRecovery() {
        val target = temporaryFolder.newFile("checkpoint.json").toPath()
        Files.write(target, "not-json".toByteArray())

        assertNull(TranscriptionCheckpointStore().loadOrNull(target))
    }
}
