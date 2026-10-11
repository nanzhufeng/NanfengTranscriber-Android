package com.nanzhufeng.transcriber.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Same user contract as the previous Qwen engine: existing Key, PCM chunks, one file ledger. */
class DashScopeFileTranscriptionEngine(
    private val providerModel: FileTranscriptionModel,
    private val client: DashScopeFileTranscriptionClient,
    private val readApiKey: () -> String?,
    private val pollIntervalMillis: Long = 2_000,
    private val maxPolls: Int = 900,
) : SpeechEngine {
    private class Session(val path: Path, var chunkPosition: Long = 0L)

    override suspend fun loadModel(modelPath: Path): LoadedModel =
        LoadedModel(modelPath.toRealPath(), 0L, Session(modelPath.toRealPath()))

    override fun setChunkPosition(model: LoadedModel, processedSamples: Long) {
        (model.runtime as Session).chunkPosition = processedSamples
    }

    override suspend fun transcribe(model: LoadedModel, samples: FloatArray, language: String?): EngineTranscript =
        withContext(Dispatchers.IO) {
            val key = readApiKey()?.takeIf(String::isNotBlank) ?: throw failure(
                "未配置百炼 API Key，请在设置中保存后重试", "FILETRANS_API_KEY_MISSING",
            )
            val session = model.runtime as Session
            val audio = wav(samples)
            val identity = digest(audio + "${providerModel.modelId}|${session.chunkPosition}|${language.orEmpty()}|${digest(key.toByteArray())}".toByteArray())
            val statePath = session.path.resolveSibling("filetrans-$identity.json")
            var state = if (Files.exists(statePath)) {
                try { JSONObject(String(Files.readAllBytes(statePath), Charsets.UTF_8)) } catch (error: Exception) {
                    throw failure("云端任务恢复记录损坏，请重新建立此任务", "FILETRANS_CHECKPOINT_INVALID")
                }
            } else JSONObject()
            if (state.has("segments")) return@withContext restore(state, language)
            var taskId = state.optString("task_id")
            if (taskId.isBlank()) {
                if (state.optBoolean("submission_pending")) throw failure(
                    "云端提交结果未确认，请先核对百炼任务记录后重新建立任务，避免重复计费",
                    "FILETRANS_SUBMISSION_UNCERTAIN",
                )
                val audioUrl = client.uploadAudio(providerModel, audio)
                // Persist intent before sending. A lost submission response must not cause auto-resubmit.
                state = JSONObject().put("submission_pending", true)
                save(statePath, state)
                try {
                    taskId = client.submit(providerModel, audioUrl, language)
                } catch (error: EngineTranscriptionException) {
                    // A definite HTTP rejection creates no remote task and can safely be retried.
                    if (error.errorCode.startsWith("FILETRANS_HTTP_") &&
                        error.errorCode.removePrefix("FILETRANS_HTTP_").toIntOrNull() in 400..499) {
                        save(statePath, JSONObject())
                    }
                    throw error
                }
                state = JSONObject().put("task_id", taskId)
                save(statePath, state)
            }
            repeat(maxPolls) {
                coroutineContext.ensureActive()
                val remote = try { client.query(taskId) } catch (error: EngineTranscriptionException) {
                    if (error.errorCode == "FILETRANS_HTTP_404") {
                        save(statePath, JSONObject())
                        throw failure("云端结果已过期或任务不存在；重试会重新转写，可能再次计费", "FILETRANS_TASK_EXPIRED")
                    }
                    throw error
                }
                when (remote) {
                    is FileTranscriptionState.Waiting -> delay(pollIntervalMillis)
                    is FileTranscriptionState.Failed -> {
                        // Confirmed terminal failure: a user-requested retry may create a fresh task.
                        save(statePath, JSONObject())
                        throw failure(remote.message, "FILETRANS_TASK_FAILED", attempted = true)
                    }
                    is FileTranscriptionState.Ready -> {
                        val segments = client.downloadResult(remote.resultUrl)
                        val duration = samples.size * 1_000L / 16_000
                        if (segments.any { it.endMillis > duration + 1_000 }) throw failure(
                            "云端结果时间戳超出音频范围，请重新读取结果", "FILETRANS_TIMESTAMPS_INVALID",
                        )
                        val completed = JSONObject().put("task_id", taskId)
                            .put("segments", JSONArray().apply {
                                segments.forEach { segment -> put(JSONObject()
                                    .put("start", segment.startMillis).put("end", segment.endMillis).put("text", segment.text)) }
                            })
                        remote.usage.durationMillis?.let { completed.put("billable_millis", it) }
                        remote.usage.inputTokens?.let { completed.put("input_tokens", it) }
                        remote.usage.outputTokens?.let { completed.put("output_tokens", it) }
                        save(statePath, completed)
                        return@withContext restore(completed, language)
                    }
                }
            }
            throw failure("云端仍在处理，请稍后重试读取；已有任务不会重复提交", "FILETRANS_POLL_TIMEOUT")
        }

    override fun releaseModel(model: LoadedModel) = Unit
    override fun close() = Unit

    private fun restore(state: JSONObject, language: String?): EngineTranscript {
        val segments = state.getJSONArray("segments")
        val input = state.longOrNull("input_tokens")
        val output = state.longOrNull("output_tokens")
        return EngineTranscript(language, (0 until segments.length()).map { index ->
            val segment = segments.getJSONObject(index)
            TranscriptSegment(segment.getLong("start"), segment.getLong("end"), segment.getString("text"))
        }, EngineInvocationUsage(
            requestCount = 1,
            billableAudioMillis = state.longOrNull("billable_millis") ?: 0,
            inputTokens = input, outputTokens = output,
            totalTokens = if (input != null && output != null) input + output else null,
        ))
    }

    private fun save(path: Path, state: JSONObject) {
        val part = path.resolveSibling("${path.fileName}.part")
        Files.write(part, state.toString().toByteArray(Charsets.UTF_8))
        try { Files.move(part, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
        catch (error: AtomicMoveNotSupportedException) { Files.move(part, path, StandardCopyOption.REPLACE_EXISTING) }
    }

    private fun JSONObject.longOrNull(name: String): Long? = if (has(name) && !isNull(name)) getLong(name) else null
    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun failure(message: String, code: String, attempted: Boolean = false) =
        EngineTranscriptionException(message, code, providerRequestAttempted = attempted)

    companion object {
        internal fun wav(samples: FloatArray): ByteArray = ByteBuffer.allocate(44 + samples.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + samples.size * 2); put("WAVEfmt ".toByteArray())
                putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000)
                putShort(2); putShort(16); put("data".toByteArray()); putInt(samples.size * 2)
                samples.forEach { putShort((it.coerceIn(-1f, 1f) * 32767).toInt().toShort()) }
            }.array()
    }
}
