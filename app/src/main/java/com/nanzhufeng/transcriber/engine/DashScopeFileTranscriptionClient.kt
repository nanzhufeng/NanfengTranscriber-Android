package com.nanzhufeng.transcriber.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resumeWithException

enum class FileTranscriptionModel(val modelId: String, val displayName: String) {
    QWEN_AUDIO_31("qwen-audio-3.1-asr-flash-filetrans", "高精度 Qwen"),
    PARAFORMER_V2("paraformer-v2", "低成本 Paraformer"),
}

data class FileTranscriptionUsage(
    val durationMillis: Long?,
    val inputTokens: Long?,
    val outputTokens: Long?,
)

sealed interface FileTranscriptionState {
    data class Waiting(val status: String) : FileTranscriptionState
    data class Ready(val resultUrl: String, val usage: FileTranscriptionUsage) : FileTranscriptionState
    data class Failed(val message: String, val safeTechnicalDetail: String? = null) : FileTranscriptionState
}

/** File URL transport only. The caller must persist task IDs before polling and supply a private,
 * time-limited audio URL. This client never uploads audio or silently resubmits an existing task. */
class DashScopeFileTranscriptionClient(
    private val readApiKey: () -> String?,
    client: OkHttpClient,
    private val baseUrl: HttpUrl,
) {
    init {
        require((baseUrl.isHttps && (baseUrl.host == "dashscope.aliyuncs.com" ||
            baseUrl.host.endsWith(".maas.aliyuncs.com"))) ||
            (baseUrl.host == "localhost" || baseUrl.host == "127.0.0.1")) {
            "文件转写服务必须使用百炼官方 HTTPS 端点"
        }
        require(baseUrl.encodedPath.endsWith('/')) { "文件转写服务地址必须以斜杠结束" }
    }
    private val apiClient = client.newBuilder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).build()
    private val resultClient = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false).build()

    suspend fun uploadAudio(model: FileTranscriptionModel, wav: ByteArray): String = withContext(Dispatchers.IO) {
        val policy = exchange(request("uploads?action=getPolicy&model=${model.modelId}").get().build())
            .optJSONObject("data") ?: invalidResponse()
        val uploadHost = policy.requiredString("upload_host")
        if (!trustedResourceUrl(uploadHost)) invalidResponse()
        val directory = policy.requiredString("upload_dir").trim('/')
        if (!directory.matches(Regex("[A-Za-z0-9_./-]+")) || directory.split('/').any { it == ".." }) invalidResponse()
        val fileName = "${UUID.randomUUID()}.wav"
        val objectKey = "$directory/$fileName"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("OSSAccessKeyId", policy.requiredString("oss_access_key_id"))
            .addFormDataPart("Signature", policy.requiredString("signature"))
            .addFormDataPart("policy", policy.requiredString("policy"))
            .addFormDataPart("x-oss-object-acl", policy.requiredString("x_oss_object_acl"))
            .addFormDataPart("x-oss-forbid-overwrite", policy.requiredString("x_oss_forbid_overwrite"))
            .addFormDataPart("key", objectKey)
            .addFormDataPart("success_action_status", "200")
            .addFormDataPart("file", fileName, wav.toRequestBody("audio/wav".toMediaType())).build()
        try {
            execute(resultClient.newCall(Request.Builder().url(uploadHost).post(body).build())).use { response ->
                if (!response.isSuccessful) throw failure("音频上传失败，请检查网络后重试", "FILETRANS_UPLOAD_HTTP_${response.code}")
            }
        } catch (error: EngineTranscriptionException) {
            throw error
        } catch (error: IOException) {
            throw failure("音频上传失败，请检查网络后重试", "FILETRANS_UPLOAD_NETWORK")
        }
        "oss://$objectKey"
    }

    suspend fun submit(model: FileTranscriptionModel, audioUrl: String, language: String? = null): String {
        val isTemporaryOss = audioUrl.startsWith("oss://") &&
            audioUrl.removePrefix("oss://").matches(Regex("[A-Za-z0-9_./-]+")) &&
            audioUrl.removePrefix("oss://").split('/').none { it == ".." }
        require(isTemporaryOss || runCatching {
            val url = audioUrl.toHttpUrl()
            url.isHttps && url.username.isEmpty() && url.password.isEmpty()
        }.getOrDefault(false)) {
            "音频必须使用有效的百炼临时文件链接或 HTTPS 文件链接"
        }
        val payload = JSONObject().put("model", model.modelId)
            .put("input", JSONObject().put("file_urls", JSONArray().put(audioUrl)))
            .put("parameters", JSONObject().put("channel_id", JSONArray().put(0)).apply {
                language?.takeIf(String::isNotBlank)?.let { put("language_hints", JSONArray().put(it)) }
            })
        val response = exchange(
            request("services/audio/asr/transcription")
                .header("X-DashScope-Async", "enable")
                .apply { if (isTemporaryOss) header("X-DashScope-OssResourceResolve", "enable") }
                .post(payload.toString().toRequestBody(JSON)).build(),
        )
        val output = response.optJSONObject("output") ?: invalidResponse()
        val taskId = output.optString("task_id")
        if (!TASK_ID.matches(taskId) || output.optString("task_status") !in setOf("PENDING", "RUNNING", "SUCCEEDED")) {
            invalidResponse()
        }
        return taskId
    }

    suspend fun query(taskId: String): FileTranscriptionState {
        require(TASK_ID.matches(taskId)) { "转写任务编号无效" }
        val response = exchange(request("tasks/$taskId").get().build())
        val output = response.optJSONObject("output") ?: invalidResponse()
        if (output.optString("task_id") != taskId) invalidResponse()
        return when (val status = output.optString("task_status")) {
            "PENDING", "RUNNING" -> FileTranscriptionState.Waiting(status)
            "FAILED", "CANCELED" -> FileTranscriptionState.Failed(
                "云端文件转写失败或已取消，请检查音频和文件链接后重试",
                failureDetail(output, response, status),
            )
            "SUCCEEDED" -> {
                val results = output.optJSONArray("results") ?: invalidResponse()
                if (results.length() != 1) invalidResponse()
                val result = results.optJSONObject(0) ?: invalidResponse()
                if (result.optString("subtask_status") != "SUCCEEDED") {
                    FileTranscriptionState.Failed(
                        "云端无法完成此音频，请检查文件链接的有效期、访问权限和格式",
                        failureDetail(result, response, "FAILED"),
                    )
                } else {
                    val url = result.optString("transcription_url")
                    if (!trustedResourceUrl(url)) invalidResponse()
                    val usage = response.optJSONObject("usage")
                    FileTranscriptionState.Ready(url, FileTranscriptionUsage(
                        durationMillis = usage?.nonNegativeNumber("duration")?.let { (it * 1_000).toLong() },
                        inputTokens = usage?.nonNegativeNumber("input_tokens")?.toLong(),
                        outputTokens = usage?.nonNegativeNumber("output_tokens")?.toLong(),
                    ))
                }
            }
            else -> invalidResponse()
        }
    }

    /** Provider result URLs are fetched without the user's API key or application interceptors. */
    suspend fun downloadResult(resultUrl: String): List<TranscriptSegment> = withContext(Dispatchers.IO) {
        if (!trustedResourceUrl(resultUrl)) invalidResponse()
        coroutineContext.ensureActive()
        try {
            execute(resultClient.newCall(Request.Builder().url(resultUrl).get().build())).use { response ->
                if (!response.isSuccessful) throw failure("云端结果暂时无法下载，请重试读取", "FILETRANS_RESULT_HTTP_${response.code}")
                parseTranscript(readBody(response.body?.source() ?: invalidResponse()))
            }
        } catch (error: EngineTranscriptionException) {
            throw error
        } catch (error: IOException) {
            throw failure("无法下载云端转写结果，请检查网络后重试", "FILETRANS_RESULT_NETWORK")
        }
    }

    private fun request(path: String): Request.Builder {
        val key = readApiKey()?.trim()?.takeIf(String::isNotEmpty)
            ?: throw failure("未配置百炼 API Key，请在设置中保存后重试", "FILETRANS_API_KEY_MISSING")
        return Request.Builder().url(requireNotNull(baseUrl.resolve(path)))
            .header("Authorization", "Bearer $key").header("Content-Type", "application/json")
    }

    private fun trustedResourceUrl(url: String): Boolean = validResultUrl(url) || runCatching {
        val parsed = url.toHttpUrl()
        baseUrl.host in setOf("localhost", "127.0.0.1") &&
            parsed.host == baseUrl.host && parsed.port == baseUrl.port
    }.getOrDefault(false)

    private suspend fun exchange(request: Request): JSONObject = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        try {
            execute(apiClient.newCall(request)).use { response ->
                if (!response.isSuccessful) throw failure(when (response.code) {
                    401, 403 -> "百炼 API Key 无效、无权限或地域不匹配"
                    429 -> "百炼服务限流或额度不足，请稍后重试"
                    else -> "云端文件转写请求失败，请检查配置后重试"
                }, "FILETRANS_HTTP_${response.code}")
                runCatching { JSONObject(readBody(response.body?.source() ?: invalidResponse())) }
                    .getOrElse { invalidResponse() }
            }
        } catch (error: EngineTranscriptionException) {
            throw error
        } catch (error: IOException) {
            throw failure("无法连接文件转写服务，请检查网络后重试", "FILETRANS_NETWORK")
        }
    }

    companion object {
        // Only protocol metadata is retained. Never retain a provider message, audio URL or body.
        private fun failureDetail(error: JSONObject, response: JSONObject, status: String): String = buildString {
            append("status=").append(status)
            val code = error.optString("code").ifBlank { response.optString("code") }
            if (code in setOf("SUCCESS_WITH_NO_VALID_FRAGMENT", "ASR_RESPONSE_HAVE_NO_WORDS", "FILE_DOWNLOAD_FAILED", "FILE_CHECK_FAILED", "Arrearage") ||
                code.matches(Regex("(?:InvalidFile|InvalidParameter|BadRequest|AccessDenied|InternalError|Throttling|LimitExceeded|QuotaExhausted|ServiceUnavailable|ModelNotFound|ModelNotSupported|DataInspectionFailed|Audio|Unsupported)[A-Za-z0-9_.]{0,80}"))) {
                append("; provider_code=").append(code)
            }
            for ((label, value) in listOf("task_id" to response.optJSONObject("output")?.optString("task_id"),
                "request_id" to response.optString("request_id"))) {
                if (value != null && value.matches(Regex("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))) {
                    append("; ").append(label).append('=').append(value)
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private suspend fun execute(call: Call): Response = suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) { response.close() }
                }
            })
        }
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val TASK_ID = Regex("[A-Za-z0-9_-]{1,128}")
        private const val MAX_RESPONSE_BYTES = 16L * 1024 * 1024

        fun parseTranscript(raw: String): List<TranscriptSegment> = try {
            val transcripts = JSONObject(raw).getJSONArray("transcripts")
            if (transcripts.length() != 1) invalidResponse()
            val transcript = transcripts.getJSONObject(0)
            if (transcript.getInt("channel_id") != 0) invalidResponse()
            val sentences = transcript.getJSONArray("sentences")
            val segments = (0 until sentences.length()).map { index ->
                val sentence = sentences.getJSONObject(index)
                val start = sentence.getLong("begin_time")
                val end = sentence.getLong("end_time")
                if (start < 0 || end < start) invalidResponse()
                TranscriptSegment(start, end, sentence.getString("text").trim())
            }.filter { it.text.isNotEmpty() }
            if (segments.isEmpty()) throw failure("云端未识别到文字，请核对音频后重试", "FILETRANS_EMPTY_TRANSCRIPT")
            segments
        } catch (error: EngineTranscriptionException) {
            throw error
        } catch (error: Exception) {
            invalidResponse()
        }

        private fun validResultUrl(url: String): Boolean = runCatching {
            val parsed = url.toHttpUrl()
            parsed.isHttps && parsed.username.isEmpty() && parsed.password.isEmpty() &&
                parsed.host.endsWith(".aliyuncs.com") && parsed.host.contains(".oss-")
        }.getOrDefault(false)

        private fun readBody(source: okio.BufferedSource): String {
            if (source.request(MAX_RESPONSE_BYTES + 1)) invalidResponse()
            return source.readUtf8()
        }

        private fun JSONObject.nonNegativeNumber(name: String): Double? =
            (opt(name) as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 }

        private fun JSONObject.requiredString(name: String): String =
            (opt(name) as? String)?.takeIf(String::isNotBlank) ?: invalidResponse()

        private fun failure(message: String, code: String) = EngineTranscriptionException(
            userMessage = message, errorCode = code,
        )

        private fun invalidResponse(): Nothing = throw failure(
            "云端文件转写返回格式异常，请稍后重试", "FILETRANS_RESPONSE_INVALID",
        )
    }
}
