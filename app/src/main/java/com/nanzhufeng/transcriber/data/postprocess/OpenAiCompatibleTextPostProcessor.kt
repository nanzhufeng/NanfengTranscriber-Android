package com.nanzhufeng.transcriber.data.postprocess

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class TextPostProcessConfig(
    val baseUrl: String,
    val model: String,
    val apiKey: String,
)

class TextPostProcessException(
    val userMessage: String,
    val technicalDetail: String,
) : Exception(userMessage)

class OpenAiCompatibleTextPostProcessor internal constructor(
    httpClient: OkHttpClient,
    private val allowInsecureLocalhost: Boolean = false,
) {
    private val client = httpClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun polishToSimplifiedChinese(
        text: String,
        config: TextPostProcessConfig,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): String = withContext(Dispatchers.IO) {
        val source = text.trim()
        if (source.isEmpty()) return@withContext text
        val endpoint = chatCompletionsEndpoint(config.baseUrl)
        val model = config.model.trim().ifBlank {
            throw TextPostProcessException("未填写翻译润色模型名", "empty model")
        }
        val key = config.apiKey.trim().ifBlank {
            throw TextPostProcessException("未保存翻译润色 API Key", "empty api key")
        }
        val chunks = splitTextForPostProcessing(source)
        chunks.mapIndexed { index, chunk ->
            coroutineContext.ensureActive()
            onProgress(index, chunks.size)
            requestChunk(endpoint, model, key, chunk)
        }.also {
            onProgress(chunks.size, chunks.size)
        }.filter(String::isNotBlank).joinToString("\n\n")
    }

    private fun requestChunk(
        endpoint: String,
        model: String,
        apiKey: String,
        text: String,
    ): String {
        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", 0.2)
            put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", text)),
            )
        }
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw httpError(response.code)
                val responseBody = response.body?.string().orEmpty()
                JSONObject(responseBody)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
                    .ifBlank {
                        throw TextPostProcessException("翻译润色服务返回了空内容", "empty response content")
                    }
            }
        }.getOrElse { error ->
            if (error is TextPostProcessException) throw error
            throw TextPostProcessException(
                userMessage = "无法连接翻译润色服务，已保留原始转写",
                technicalDetail = "${error::class.java.simpleName}: ${error.message.orEmpty().take(200)}",
            )
        }
    }

    private fun chatCompletionsEndpoint(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/').toHttpUrlOrNull()
            ?: throw TextPostProcessException("翻译润色服务地址无效", "invalid base url")
        val isAllowedTestEndpoint = allowInsecureLocalhost &&
            normalized.scheme == "http" && normalized.host in setOf("localhost", "127.0.0.1")
        if (!normalized.isHttps && !isAllowedTestEndpoint) {
            throw TextPostProcessException("翻译润色服务必须使用 HTTPS，避免密钥泄露", "non-https base url")
        }
        return normalized.newBuilder().addPathSegments("chat/completions").build().toString()
    }

    private fun httpError(code: Int): TextPostProcessException = when (code) {
        401, 403 -> TextPostProcessException("翻译润色凭据无效，请在设置中更新 API Key", "http $code")
        408 -> TextPostProcessException("翻译润色服务请求超时，已保留原始转写", "http 408")
        429 -> TextPostProcessException("翻译润色服务限流或额度不足，已保留原始转写", "http 429")
        else -> TextPostProcessException("翻译润色服务返回错误（HTTP $code），已保留原始转写", "http $code")
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val SYSTEM_PROMPT =
            "你是专业的中文转写稿整理助手。把音视频转写稿整理为现代简体中文；" +
                "修正常见同音错字、断句和标点；不添加原文没有的新观点；" +
                "保留人名、地名、专有名词和数字；只输出整理后的正文，不要解释。"
    }
}

internal fun splitTextForPostProcessing(text: String, maxChars: Int = 6_000): List<String> {
    require(maxChars > 0) { "maxChars 必须大于 0" }
    val paragraphs = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    if (paragraphs.isEmpty()) return listOf(text.trim())
    val chunks = mutableListOf<String>()
    var current = StringBuilder()
    fun flush() {
        if (current.isNotEmpty()) chunks += current.toString()
        current = StringBuilder()
    }
    paragraphs.forEach { paragraph ->
        paragraph.chunked(maxChars).forEach { part ->
            val separator = if (current.isEmpty()) 0 else 1
            if (current.length + separator + part.length > maxChars) flush()
            if (current.isNotEmpty()) current.append('\n')
            current.append(part)
        }
    }
    flush()
    return chunks.ifEmpty { listOf(text.trim()) }
}
