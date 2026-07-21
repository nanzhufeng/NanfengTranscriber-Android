package com.nanzhufeng.transcriber.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nanzhufeng.transcriber.data.media.AudioDecodeResult
import com.nanzhufeng.transcriber.data.media.MediaCodecAudioDecoder
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(AndroidJUnit4::class)
class EndToEndTranscriptionInstrumentedTest {
    @Test
    fun realMediaDecodeAndWhisperInferenceSharePersistentFiles() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val modelPath = context.noBackupFilesDir
            .resolve("probe/ggml-tiny.en-q5_1.bin")
            .toPath()
        assertTrue("模拟器探针模型不存在：$modelPath", Files.isRegularFile(modelPath))

        val staging = context.noBackupFilesDir.resolve("staging/e2e-test").toPath()
        Files.createDirectories(staging)
        val source = staging.resolve("jfk.wav")
        val pcm = staging.resolve("jfk.pcm")
        try {
            instrumentation.context.assets.open("jfk.wav").use { input ->
                Files.copy(input, source, StandardCopyOption.REPLACE_EXISTING)
            }
            val decodeStarted = System.nanoTime()
            val decoded = MediaCodecAudioDecoder(context.contentResolver).decodeFile(source, pcm)
            val decodeMillis = (System.nanoTime() - decodeStarted) / 1_000_000L
            assertTrue("真实文件解码失败：$decoded", decoded is AudioDecodeResult.Success)

            val engine = WhisperCppEngine(useGpu = false, threadCount = 4)
            val loadStarted = System.nanoTime()
            val model = engine.loadModel(modelPath)
            val loadMillis = (System.nanoTime() - loadStarted) / 1_000_000L
            try {
                val inferenceStarted = System.nanoTime()
                val result = PcmTranscriptionCoordinator(engine).transcribe(
                    artifact = (decoded as AudioDecodeResult.Success).artifact,
                    model = model,
                    language = "en",
                )
                val inferenceMillis = (System.nanoTime() - inferenceStarted) / 1_000_000L
                assertTrue("真实 PCM 推理失败：$result", result is PcmTranscriptionResult.Success)
                val transcript = (result as PcmTranscriptionResult.Success).transcript
                val text = transcript.segments.joinToString(" ") { it.text }.lowercase()
                assertTrue("端到端结果不符合 JFK 样本：$text", "americans" in text || "country" in text)

                context.noBackupFilesDir.resolve("probe/full-pipeline-result.json").writeText(
                    JSONObject()
                        .put("source", "jfk.wav")
                        .put("decodeMillis", decodeMillis)
                        .put("modelLoadMillis", loadMillis)
                        .put("inferenceMillis", inferenceMillis)
                        .put("pcmSamples", decoded.artifact.sampleCount)
                        .put("segmentCount", transcript.segments.size)
                        .put("text", text)
                        .toString(2),
                )
            } finally {
                engine.releaseModel(model)
                engine.close()
            }
        } finally {
            Files.deleteIfExists(pcm)
            Files.deleteIfExists(source)
            Files.deleteIfExists(staging)
        }
    }
}
