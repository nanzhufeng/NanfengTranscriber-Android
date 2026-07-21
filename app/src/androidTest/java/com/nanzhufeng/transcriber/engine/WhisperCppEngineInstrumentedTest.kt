package com.nanzhufeng.transcriber.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class WhisperCppEngineInstrumentedTest {
    @Test
    fun transcribesRealPcmWithExternalProbeModel() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val model = targetContext.noBackupFilesDir
            .resolve("probe/ggml-tiny.en-q5_1.bin")
            .toPath()
        assertTrue("模拟器探针模型不存在：$model", model.toFile().isFile)

        val wav = instrumentation.context.assets.open("jfk.wav").use { it.readBytes() }
        val samples = decodePcm16MonoWav(wav)
        val engine = WhisperCppEngine(useGpu = false, threadCount = 4)
        val loadStarted = System.nanoTime()
        val loaded = engine.loadModel(model)
        val loadMillis = (System.nanoTime() - loadStarted) / 1_000_000
        try {
            val inferenceStarted = System.nanoTime()
            val transcript = engine.transcribe(loaded, samples, language = "en")
            val inferenceMillis = (System.nanoTime() - inferenceStarted) / 1_000_000
            val text = transcript.segments.joinToString(" ") { it.text }.lowercase()
            targetContext.noBackupFilesDir.resolve("probe/result.json").writeText(
                JSONObject()
                    .put("model", model.fileName.toString())
                    .put("sampleCount", samples.size)
                    .put("loadMillis", loadMillis)
                    .put("inferenceMillis", inferenceMillis)
                    .put("detectedLanguage", transcript.detectedLanguage)
                    .put("segmentCount", transcript.segments.size)
                    .put("text", text)
                    .toString(2),
            )
            assertTrue("真实推理没有生成分段", transcript.segments.isNotEmpty())
            assertTrue("真实推理结果不符合 JFK 样本：$text", "americans" in text || "country" in text)
        } finally {
            engine.releaseModel(loaded)
            engine.close()
        }
    }

    private fun decodePcm16MonoWav(wav: ByteArray): FloatArray {
        require(wav.size > 44) { "WAV 文件过短" }
        require(String(wav, 0, 4, Charsets.US_ASCII) == "RIFF") { "不是 RIFF WAV" }
        require(String(wav, 8, 4, Charsets.US_ASCII) == "WAVE") { "不是 WAVE 音频" }

        val littleEndian = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 12
        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var dataOffset = -1
        var dataLength = 0
        while (offset + 8 <= wav.size) {
            val chunkId = String(wav, offset, 4, Charsets.US_ASCII)
            val chunkLength = littleEndian.getInt(offset + 4)
            require(chunkLength >= 0 && offset + 8L + chunkLength <= wav.size) { "WAV 分块损坏" }
            when (chunkId) {
                "fmt " -> {
                    require(littleEndian.getShort(offset + 8).toInt() == 1) { "只支持 PCM WAV" }
                    channels = littleEndian.getShort(offset + 10).toInt()
                    sampleRate = littleEndian.getInt(offset + 12)
                    bitsPerSample = littleEndian.getShort(offset + 22).toInt()
                }
                "data" -> {
                    dataOffset = offset + 8
                    dataLength = chunkLength
                    break
                }
            }
            offset += 8 + chunkLength + (chunkLength and 1)
        }

        require(channels == 1) { "探针样本必须是单声道" }
        require(sampleRate == 16_000) { "探针样本必须是 16 kHz" }
        require(bitsPerSample == 16) { "探针样本必须是 16-bit PCM" }
        require(dataOffset >= 0 && dataLength > 0) { "WAV 缺少 data 分块" }

        return FloatArray(dataLength / 2) { index ->
            littleEndian.getShort(dataOffset + index * 2) / 32768.0f
        }
    }
}
