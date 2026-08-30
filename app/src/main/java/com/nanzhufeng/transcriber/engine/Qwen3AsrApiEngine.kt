package com.nanzhufeng.transcriber.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.util.Base64

/** Qwen3-ASR is a transcription engine, never a post-processing step. */
class Qwen3AsrApiEngine(
    private val readApiKey: () -> String?,
    private val client: OkHttpClient,
    private val endpoint: String = ENDPOINT,
) : SpeechEngine {
    override suspend fun loadModel(modelPath: Path): LoadedModel = LoadedModel(modelPath.toRealPath(), 0L)

    override suspend fun transcribe(model: LoadedModel, samples: FloatArray, language: String?): EngineTranscript =
        withContext(Dispatchers.IO) {
            val key = readApiKey() ?: throw EngineTranscriptionException(
                userMessage = "高精度 Qwen 未配置 API Key，请在设置中保存后重试",
                errorCode = "QWEN_API_KEY_MISSING",
            )
            val audio = Base64.getEncoder().encodeToString(wav(samples))
            val content = JSONArray()
                .put(JSONObject().put("type", "input_audio").put("input_audio", JSONObject().put("data", "data:audio/wav;base64,$audio")))
            val asrOptions = JSONObject().put("enable_itn", false).apply {
                language?.takeIf(String::isNotBlank)?.let { put("language", it) }
            }
            val payload = JSONObject()
                .put("model", MODEL)
                .put("stream", false)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
                .put("asr_options", asrOptions)
            val request = Request.Builder().url(endpoint)
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON))
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw httpFailure(response.code, samples.size)
                    val payload = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrElse {
                        throw EngineTranscriptionException(
                            userMessage = "千问3-ASR 返回格式异常，请稍后重试",
                            errorCode = "QWEN_RESPONSE_INVALID",
                            safeTechnicalDetail = "invalid JSON response",
                            providerRequestAttempted = true,
                            providerAttemptedAudioMillis = samples.size.durationMillis(),
                        )
                    }
                    val text = runCatching {
                        payload
                            .getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                            .getString("content").trim()
                    }.getOrElse {
                        throw EngineTranscriptionException(
                            userMessage = "千问3-ASR 返回格式异常，请稍后重试",
                            errorCode = "QWEN_RESPONSE_INVALID",
                            safeTechnicalDetail = "missing choices.message.content",
                            providerRequestAttempted = true,
                            providerAttemptedAudioMillis = samples.size.durationMillis(),
                        )
                    }
                    if (text.isBlank()) throw EngineTranscriptionException(
                        userMessage = "千问3-ASR 未返回转写文字，请重试",
                        errorCode = "QWEN_EMPTY_TRANSCRIPT",
                        providerRequestAttempted = true,
                        providerAttemptedAudioMillis = samples.size.durationMillis(),
                    )
                    EngineTranscript(
                        detectedLanguage = language,
                        segments = listOf(TranscriptSegment(0L, samples.size * 1_000L / SAMPLE_RATE, text)),
                        invocationUsage = EngineInvocationUsage(
                            requestCount = 1,
                            billableAudioMillis = samples.size.durationMillis(),
                            inputTokens = payload.optJSONObject("usage")?.safeLong("input_tokens"),
                            outputTokens = payload.optJSONObject("usage")?.safeLong("output_tokens"),
                            totalTokens = payload.optJSONObject("usage")?.safeLong("total_tokens"),
                        ),
                    )
                }
            } catch (error: EngineTranscriptionException) {
                throw error
            } catch (error: IOException) {
                val failure = EngineTranscriptionException(
                    userMessage = "无法连接千问3-ASR，请检查网络后重试",
                    errorCode = "QWEN_NETWORK_ERROR",
                    safeTechnicalDetail = error.javaClass.simpleName,
                    providerRequestAttempted = true,
                    providerAttemptedAudioMillis = samples.size.durationMillis(),
                )
                throw failure
            }
        }

    override fun releaseModel(model: LoadedModel) = Unit
    override fun close() = Unit

    private fun wav(samples: FloatArray): ByteArray {
        val pcm = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { pcm.putShort((it.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()) }
        val data = pcm.array()
        return ByteArrayOutputStream(44 + data.size).apply {
            write("RIFF".toByteArray()); writeIntLE(36 + data.size); write("WAVEfmt ".toByteArray())
            writeIntLE(16); writeShortLE(1); writeShortLE(1); writeIntLE(SAMPLE_RATE); writeIntLE(SAMPLE_RATE * 2)
            writeShortLE(2); writeShortLE(16); write("data".toByteArray()); writeIntLE(data.size); write(data)
        }.toByteArray()
    }
    private fun ByteArrayOutputStream.writeIntLE(value: Int) { write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()) }
    private fun ByteArrayOutputStream.writeShortLE(value: Int) { write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()) }
    private fun httpFailure(code: Int, sampleCount: Int) = EngineTranscriptionException(
        userMessage = when (code) {
            400 -> "千问3-ASR 请求被拒绝，请检查音频后重试"
            401, 403 -> "千问 API Key 无效、无权限或地域不匹配"
            413 -> "音频片段过大，请缩短音频后重试"
            429 -> "千问服务限流或额度不足，请稍后重试"
            in 500..599 -> "千问服务暂不可用，请稍后重试"
            else -> "千问3-ASR 请求失败，请稍后重试"
        },
        errorCode = "QWEN_HTTP_$code",
        safeTechnicalDetail = "http=$code",
        providerRequestAttempted = true,
        providerAttemptedAudioMillis = sampleCount.durationMillis(),
    )

    private fun Int.durationMillis(): Long = toLong() * 1_000L / SAMPLE_RATE

    private fun JSONObject.safeLong(name: String): Long? =
        takeIf { has(name) && !isNull(name) }?.optLong(name)

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val MODEL = "qwen3-asr-flash"
        const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
