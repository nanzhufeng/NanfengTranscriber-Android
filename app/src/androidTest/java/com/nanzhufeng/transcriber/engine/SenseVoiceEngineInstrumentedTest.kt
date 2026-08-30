package com.nanzhufeng.transcriber.engine

import androidx.test.platform.app.InstrumentationRegistry
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class SenseVoiceEngineInstrumentedTest {
    @Test
    fun officialInt8ModelProducesReadableTextFromRealWav() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val modelDirectory = targetContext.noBackupFilesDir
            .resolve("models/sensevoice-small-int8/hf-355f4d48")
        val modelPath = modelDirectory.resolve("model.int8.onnx").toPath()
        val tokensPath = modelDirectory.resolve("tokens.txt").toPath()
        require(Files.isRegularFile(modelPath) && Files.isRegularFile(tokensPath)) {
            "请先把已校验的 SenseVoice 模型放入隔离模拟器测试目录"
        }
        val samples = instrumentation.context.assets.open("jfk.wav").use { input ->
            readMonoPcm16Wav(input.readBytes())
        }

        val engine = SenseVoiceEngine(threadCount = 4)
        val startedAt = SystemClock.elapsedRealtime()
        val model = engine.loadModel(modelPath)
        try {
            val transcript = engine.transcribe(model, samples, language = "en")
            val text = transcript.segments.joinToString(" ") { it.text }
            Log.i("SenseVoiceSmoke", "elapsedMs=${SystemClock.elapsedRealtime() - startedAt} text=$text")
            assertTrue(text.isNotBlank())
            assertTrue(transcript.segments.all { it.endMillis > it.startMillis })
        } finally {
            engine.releaseModel(model)
            engine.close()
        }
    }

    private fun readMonoPcm16Wav(wav: ByteArray): FloatArray {
        require(wav.size >= 44 && String(wav, 0, 4, Charsets.US_ASCII) == "RIFF") {
            "测试 WAV 格式无效"
        }
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
                }
            }
            offset += 8 + chunkLength + (chunkLength and 1)
        }
        require(channels == 1 && sampleRate == 16_000 && bitsPerSample == 16 && dataOffset >= 0) {
            "测试 WAV 必须是 16 kHz 单声道 PCM16"
        }
        val pcm = ByteBuffer.wrap(wav, dataOffset, dataLength).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(dataLength / 2) { pcm.short / 32768.0f }
    }
}
