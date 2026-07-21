package com.nanzhufeng.transcriber.data.result

import com.nanzhufeng.transcriber.domain.export.TranscriptDocument
import com.nanzhufeng.transcriber.domain.export.TranscriptDocumentSegment
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class TranscriptDocumentStore {
    fun save(target: Path, stored: StoredTranscript) {
        Files.createDirectories(target.parent)
        val temporary = target.resolveSibling("${target.fileName}.part")
        Files.newBufferedWriter(temporary, StandardCharsets.UTF_8).use { writer ->
            writer.write(encode(stored).toString())
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

    fun load(source: Path): StoredTranscript = Files.newBufferedReader(
        source,
        StandardCharsets.UTF_8,
    ).use { reader -> decode(JSONObject(reader.readText())) }

    private fun encode(stored: StoredTranscript): JSONObject = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("title", stored.document.title)
        put("polishedText", stored.document.polishedText ?: JSONObject.NULL)
        put("detectedLanguage", stored.detectedLanguage ?: JSONObject.NULL)
        put("performanceSummary", stored.performanceSummary ?: JSONObject.NULL)
        put("segments", JSONArray().apply {
            stored.document.segments.forEach { segment ->
                put(JSONObject().apply {
                    put("startMillis", segment.startMillis)
                    put("endMillis", segment.endMillis)
                    put("text", segment.text)
                })
            }
        })
    }

    private fun decode(source: JSONObject): StoredTranscript {
        require(source.getInt("schemaVersion") == SCHEMA_VERSION) {
            "不支持的转写结果版本"
        }
        val segments = source.getJSONArray("segments")
        return StoredTranscript(
            document = TranscriptDocument(
                title = source.getString("title"),
                segments = List(segments.length()) { index ->
                    segments.getJSONObject(index).let { segment ->
                        TranscriptDocumentSegment(
                            startMillis = segment.getLong("startMillis"),
                            endMillis = segment.getLong("endMillis"),
                            text = segment.getString("text"),
                        )
                    }
                },
                polishedText = source.optionalString("polishedText"),
            ),
            detectedLanguage = source.optionalString("detectedLanguage"),
            performanceSummary = source.optionalString("performanceSummary"),
        )
    }

    private fun JSONObject.optionalString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}

data class StoredTranscript(
    val document: TranscriptDocument,
    val detectedLanguage: String? = null,
    val performanceSummary: String? = null,
)
