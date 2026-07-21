package com.nanzhufeng.transcriber.data.postprocess

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiCompatibleTextPostProcessorTest {
    @Test
    fun splitTextRespectsChunkLimitAndKeepsAllContent() {
        val source = "第一段\n${"长".repeat(13)}\n最后"
        val chunks = splitTextForPostProcessing(source, maxChars = 6)

        assertTrue(chunks.all { it.length <= 6 })
        assertEquals(source.replace("\n", ""), chunks.joinToString("").replace("\n", ""))
    }

    @Test
    fun compatibleRequestSendsOnlyTextAndReturnsAssistantContent() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("""{"choices":[{"message":{"content":"整理后的正文"}}]}"""))
            start()
        }
        try {
            val processor = OpenAiCompatibleTextPostProcessor(OkHttpClient(), true)
            val result = processor.polishToSimplifiedChinese(
                text = "原始转写",
                config = TextPostProcessConfig(
                    baseUrl = server.url("/v1").toString(),
                    model = "compatible-model",
                    apiKey = "test-secret",
                ),
            )

            assertEquals("整理后的正文", result)
            val request = server.takeRequest()
            assertEquals("/v1/chat/completions", request.path)
            assertEquals("Bearer test-secret", request.getHeader("Authorization"))
            assertTrue(request.body.readUtf8().contains("原始转写"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun unauthorizedResponseBecomesChineseCredentialError() = runBlocking {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(401))
            start()
        }
        try {
            val error = runCatching {
                OpenAiCompatibleTextPostProcessor(OkHttpClient(), true).polishToSimplifiedChinese(
                    text = "原始转写",
                    config = TextPostProcessConfig(server.url("/v1").toString(), "model", "bad-key"),
                )
            }.exceptionOrNull() as TextPostProcessException

            assertTrue(error.userMessage.contains("凭据无效"))
            assertEquals("http 401", error.technicalDetail)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun productionConfigurationRejectsPlainHttp() = runBlocking {
        val error = runCatching {
            OpenAiCompatibleTextPostProcessor(OkHttpClient()).polishToSimplifiedChinese(
                text = "原始转写",
                config = TextPostProcessConfig("http://example.com/v1", "model", "key"),
            )
        }.exceptionOrNull() as TextPostProcessException

        assertTrue(error.userMessage.contains("HTTPS"))
    }
}
