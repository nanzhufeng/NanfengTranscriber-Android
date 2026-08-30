package com.nanzhufeng.transcriber.engine

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import kotlinx.coroutines.runBlocking

class Qwen3AsrApiEngineTest {
    @Test
    fun `sends exactly one official input audio content item`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"choices":[{"message":{"content":"转写结果"}}]}""",
                ),
            )
            val engine = Qwen3AsrApiEngine(
                readApiKey = { "test-key" },
                client = OkHttpClient(),
                endpoint = server.url("/compatible-mode/v1/chat/completions").toString(),
            )
            val modelFile = Files.createTempFile("qwen3-asr-test", ".session")
            try {
                val transcript = engine.transcribe(
                    model = engine.loadModel(modelFile),
                    samples = floatArrayOf(0f, 0.25f, -0.25f),
                    language = "zh",
                )

                assertEquals("转写结果", transcript.segments.single().text)
                val body = JSONObject(server.takeRequest().body.readUtf8())
                val content = body.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
                assertEquals(1, content.length())
                assertEquals("input_audio", content.getJSONObject(0).getString("type"))
                assertTrue(content.getJSONObject(0).getJSONObject("input_audio").getString("data").startsWith("data:audio/wav;base64,"))
                assertEquals("zh", body.getJSONObject("asr_options").getString("language"))
                assertEquals(1, transcript.invocationUsage?.requestCount)
            } finally {
                Files.deleteIfExists(modelFile)
            }
        }
    }

    @Test
    fun `maps API authentication errors without storing a response body`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"secret"}}"""))
            val engine = Qwen3AsrApiEngine(
                readApiKey = { "test-key" },
                client = OkHttpClient(),
                endpoint = server.url("/compatible-mode/v1/chat/completions").toString(),
            )
            val modelFile = Files.createTempFile("qwen3-asr-test", ".session")
            try {
                val failure = runCatching {
                    engine.transcribe(engine.loadModel(modelFile), floatArrayOf(0f), null)
                }.exceptionOrNull() as EngineTranscriptionException

                assertEquals("QWEN_HTTP_401", failure.errorCode)
                assertEquals("千问 API Key 无效、无权限或地域不匹配", failure.userMessage)
                assertEquals("http=401", failure.safeTechnicalDetail)
                assertTrue(failure.providerRequestAttempted)
            } finally {
                Files.deleteIfExists(modelFile)
            }
        }
    }
}
