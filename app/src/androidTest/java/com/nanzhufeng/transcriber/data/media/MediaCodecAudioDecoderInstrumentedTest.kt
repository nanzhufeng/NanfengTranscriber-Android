package com.nanzhufeng.transcriber.data.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(AndroidJUnit4::class)
class MediaCodecAudioDecoderInstrumentedTest {
    @Test
    fun decodesRealWavToPersistentPcm16WithoutHoldingWholeAudio() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = context.noBackupFilesDir.resolve("staging/decode-test").toPath()
        Files.createDirectories(directory)
        val source = directory.resolve("jfk.wav")
        val target = directory.resolve("jfk-16k-mono.pcm")
        try {
            instrumentation.context.assets.open("jfk.wav").use { input ->
                Files.copy(input, source, StandardCopyOption.REPLACE_EXISTING)
            }
            val progress = mutableListOf<AudioDecodeProgress>()
            val result = MediaCodecAudioDecoder(context.contentResolver)
                .decodeFile(source, target, progress::add)

            assertTrue("真实 WAV 解码失败：$result", result is AudioDecodeResult.Success)
            val artifact = (result as AudioDecodeResult.Success).artifact
            assertEquals(16_000, artifact.sampleRate)
            assertEquals(1, artifact.channels)
            assertTrue(artifact.durationMillis in 10_900L..11_100L)
            assertEquals(artifact.sampleCount * 2L, Files.size(target))
            assertTrue(progress.isNotEmpty())
            val probe = ByteArray(4_096)
            val read = Files.newInputStream(target).use { it.read(probe) }
            assertTrue(read > 0 && probe.copyOf(read).any { it.toInt() != 0 })
        } finally {
            Files.deleteIfExists(target)
            Files.deleteIfExists(source)
            Files.deleteIfExists(directory)
        }
    }
}
