package com.nanzhufeng.transcriber.data.result

import com.nanzhufeng.transcriber.domain.export.TranscriptDocumentSegment
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class TranscriptionCheckpointStore {
    fun save(target: Path, checkpoint: StoredTranscriptionCheckpoint) {
        checkpoint.validate()
        Files.createDirectories(target.parent)
        val temporary = target.resolveSibling("${target.fileName}.part")
        Files.newBufferedWriter(temporary, StandardCharsets.UTF_8).use { writer ->
            writer.write(encode(checkpoint).toString())
        }
        try {
            Files.move(
                temporary,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun loadOrNull(source: Path): StoredTranscriptionCheckpoint? = runCatching {
        Files.newBufferedReader(source, StandardCharsets.UTF_8).use { reader ->
            decode(JSONObject(reader.readText())).also(StoredTranscriptionCheckpoint::validate)
        }
    }.getOrNull()

    private fun encode(checkpoint: StoredTranscriptionCheckpoint): JSONObject = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("checkpointKey", checkpoint.checkpointKey)
        put("sampleRate", checkpoint.sampleRate)
        put("channels", checkpoint.channels)
        put("sampleCount", checkpoint.sampleCount)
        put("durationMillis", checkpoint.durationMillis)
        put("pcmBytes", checkpoint.pcmBytes)
        put("processedSamples", checkpoint.processedSamples)
        put("detectedLanguage", checkpoint.detectedLanguage ?: JSONObject.NULL)
        put("decodeMillis", checkpoint.decodeMillis)
        put("modelLoadMillis", checkpoint.modelLoadMillis)
        put("transcribeMillis", checkpoint.transcribeMillis)
        put("segments", JSONArray().apply {
            checkpoint.segments.forEach { segment ->
                put(JSONObject().apply {
                    put("startMillis", segment.startMillis)
                    put("endMillis", segment.endMillis)
                    put("text", segment.text)
                })
            }
        })
    }

    private fun decode(source: JSONObject): StoredTranscriptionCheckpoint {
        require(source.getInt("schemaVersion") == SCHEMA_VERSION) {
            "不支持的转写断点版本"
        }
        val segments = source.getJSONArray("segments")
        return StoredTranscriptionCheckpoint(
            checkpointKey = source.getString("checkpointKey"),
            sampleRate = source.getInt("sampleRate"),
            channels = source.getInt("channels"),
            sampleCount = source.getLong("sampleCount"),
            durationMillis = source.getLong("durationMillis"),
            pcmBytes = source.getLong("pcmBytes"),
            processedSamples = source.getLong("processedSamples"),
            detectedLanguage = source.optionalString("detectedLanguage"),
            decodeMillis = source.getLong("decodeMillis"),
            modelLoadMillis = source.getLong("modelLoadMillis"),
            transcribeMillis = source.getLong("transcribeMillis"),
            segments = List(segments.length()) { index ->
                segments.getJSONObject(index).let { segment ->
                    TranscriptDocumentSegment(
                        startMillis = segment.getLong("startMillis"),
                        endMillis = segment.getLong("endMillis"),
                        text = segment.getString("text"),
                    )
                }
            },
        )
    }

    private fun JSONObject.optionalString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}

data class StoredTranscriptionCheckpoint(
    val checkpointKey: String,
    val sampleRate: Int,
    val channels: Int,
    val sampleCount: Long,
    val durationMillis: Long,
    val pcmBytes: Long,
    val processedSamples: Long,
    val detectedLanguage: String?,
    val decodeMillis: Long,
    val modelLoadMillis: Long,
    val transcribeMillis: Long,
    val segments: List<TranscriptDocumentSegment>,
) {
    fun isCompatible(expectedKey: String, pcmPath: Path): Boolean = runCatching {
        checkpointKey == expectedKey &&
            sampleRate == 16_000 &&
            channels == 1 &&
            sampleCount > 0L &&
            durationMillis == sampleCount * 1_000L / sampleRate &&
            pcmBytes == sampleCount * PCM16_BYTES_PER_SAMPLE &&
            processedSamples in 0L..sampleCount &&
            Files.isRegularFile(pcmPath) &&
            Files.size(pcmPath) == pcmBytes
    }.getOrDefault(false)

    fun validate() {
        require(checkpointKey.isNotBlank()) { "转写断点缺少任务标识" }
        require(sampleRate == 16_000) { "转写断点采样率不正确" }
        require(channels == 1) { "转写断点声道数不正确" }
        require(sampleCount > 0L) { "转写断点样本数不正确" }
        require(durationMillis == sampleCount * 1_000L / sampleRate) { "转写断点时长不一致" }
        require(pcmBytes == sampleCount * PCM16_BYTES_PER_SAMPLE) { "转写断点 PCM 长度不一致" }
        require(processedSamples in 0L..sampleCount) { "转写断点进度越界" }
        require(decodeMillis >= 0L && modelLoadMillis >= 0L && transcribeMillis >= 0L) {
            "转写断点性能数据不正确"
        }
        require(segments.all { it.startMillis >= 0L && it.endMillis >= it.startMillis }) {
            "转写断点分段时间不正确"
        }
    }

    private companion object {
        const val PCM16_BYTES_PER_SAMPLE = 2L
    }
}
