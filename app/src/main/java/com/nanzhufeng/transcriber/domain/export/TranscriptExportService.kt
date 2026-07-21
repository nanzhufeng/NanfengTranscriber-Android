package com.nanzhufeng.transcriber.domain.export

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TranscriptExportService {
    fun export(
        document: TranscriptDocument,
        format: TranscriptExportFormat,
        output: OutputStream,
    ) {
        when (format) {
            TranscriptExportFormat.TXT -> writeUtf8(output, renderTxt(document))
            TranscriptExportFormat.MARKDOWN -> writeUtf8(output, renderMarkdown(document))
            TranscriptExportFormat.SRT -> writeUtf8(output, renderSrt(document))
            TranscriptExportFormat.DOCX -> writeDocx(output, document)
        }
        output.flush()
    }

    fun renderTxt(document: TranscriptDocument): String {
        val text = document.polishedText?.trim().takeUnless { it.isNullOrEmpty() }
            ?: readableSegments(document.segments).joinToString("\n") { it.text }
        return text.trimEnd() + "\n"
    }

    fun renderMarkdown(document: TranscriptDocument): String {
        val polished = document.polishedText?.trim().takeUnless { it.isNullOrEmpty() }
        if (polished != null) {
            val paragraphs = polished.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .joinToString("\n\n")
            return "# ${document.title}\n\n$paragraphs\n"
        }

        val lines = mutableListOf(
            "# ${document.title}",
            "",
            "| 时间 | 内容 |",
            "| --- | --- |",
        )
        readableSegments(document.segments).forEach { segment ->
            lines += "| ${formatTimestamp(segment.startMillis, decimalSeparator = '.')} | " +
                "${segment.text.replace("|", "\\|")} |"
        }
        return lines.joinToString("\n") + "\n"
    }

    fun renderSrt(document: TranscriptDocument): String {
        val lines = mutableListOf<String>()
        var index = 1
        document.segments.forEach { segment ->
            val text = cleanText(segment.text)
            if (text.isEmpty()) return@forEach
            lines += index.toString()
            lines += "${formatTimestamp(segment.startMillis, ',')} --> " +
                formatTimestamp(segment.endMillis, ',')
            lines += text
            lines += ""
            index += 1
        }
        return lines.joinToString("\n")
    }

    fun readableSegments(
        segments: List<TranscriptDocumentSegment>,
        maxGapMillis: Long = 1_000L,
        maxCharacters: Int = 90,
    ): List<TranscriptDocumentSegment> {
        val merged = mutableListOf<TranscriptDocumentSegment>()
        segments.forEach { raw ->
            val text = cleanText(raw.text)
            if (text.isEmpty()) return@forEach
            val current = raw.copy(text = text)
            val previous = merged.lastOrNull()
            if (previous != null && shouldMerge(previous, current, maxGapMillis, maxCharacters)) {
                merged[merged.lastIndex] = previous.copy(
                    endMillis = current.endMillis,
                    text = joinText(previous.text, current.text),
                )
            } else {
                merged += current
            }
        }
        return merged
    }

    private fun shouldMerge(
        current: TranscriptDocumentSegment,
        next: TranscriptDocumentSegment,
        maxGapMillis: Long,
        maxCharacters: Int,
    ): Boolean {
        if (current.text.endsWithAny(SENTENCE_ENDINGS)) return false
        if (next.startMillis - current.endMillis > maxGapMillis) return false
        return current.text.length + next.text.length <= maxCharacters
    }

    private fun joinText(left: String, right: String): String = when {
        left.isEmpty() -> right
        right.isEmpty() -> left
        left.endsWithAny(JOIN_PUNCTUATION) -> left + right
        else -> "$left，$right"
    }

    private fun cleanText(text: String): String = text.trim().split(WHITESPACE)
        .filter(String::isNotEmpty)
        .joinToString(" ")

    private fun formatTimestamp(milliseconds: Long, decimalSeparator: Char): String {
        val safe = milliseconds.coerceAtLeast(0L)
        val totalSeconds = safe / 1_000L
        val millis = safe % 1_000L
        val seconds = totalSeconds % 60L
        val totalMinutes = totalSeconds / 60L
        val minutes = totalMinutes % 60L
        val hours = totalMinutes / 60L
        return "%02d:%02d:%02d%c%03d".format(
            hours,
            minutes,
            seconds,
            decimalSeparator,
            millis,
        )
    }

    private fun writeDocx(output: OutputStream, document: TranscriptDocument) {
        val zip = ZipOutputStream(output, StandardCharsets.UTF_8)
        zip.writeEntry("[Content_Types].xml", CONTENT_TYPES)
        zip.writeEntry("_rels/.rels", ROOT_RELATIONSHIPS)
        zip.writeEntry("word/document.xml", buildDocumentXml(document))
        zip.finish()
        zip.flush()
    }

    private fun buildDocumentXml(document: TranscriptDocument): String {
        val bodyText = document.polishedText?.trim().takeUnless { it.isNullOrEmpty() }
            ?: readableSegments(document.segments).joinToString("\n") { it.text }
        val paragraphs = bodyText.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString("") { paragraphXml(it) }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            |<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            |<w:body>${headingXml(document.title)}$paragraphs<w:sectPr/></w:body>
            |</w:document>
        """.trimMargin()
    }

    private fun headingXml(text: String): String =
        "<w:p><w:r><w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr>" +
            "<w:t xml:space=\"preserve\">${escapeXml(text)}</w:t></w:r></w:p>"

    private fun paragraphXml(text: String): String =
        "<w:p><w:r><w:t xml:space=\"preserve\">${escapeXml(text)}</w:t></w:r></w:p>"

    private fun escapeXml(text: String): String = buildString(text.length) {
        text.forEach { character ->
            append(
                when (character) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&apos;"
                    else -> character
                },
            )
        }
    }

    private fun ZipOutputStream.writeEntry(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(StandardCharsets.UTF_8))
        closeEntry()
    }

    private fun writeUtf8(output: OutputStream, text: String) {
        output.write(text.toByteArray(StandardCharsets.UTF_8))
    }

    private fun String.endsWithAny(values: Set<Char>): Boolean = lastOrNull() in values

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private val SENTENCE_ENDINGS = setOf('。', '！', '？', '!', '?', '；', ';')
        private val JOIN_PUNCTUATION = setOf('，', '、', '。', '！', '？', '；', ',', '.', '!', '?', ';')

        private const val CONTENT_TYPES =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/word/document.xml\" " +
                "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                "</Types>"

        private const val ROOT_RELATIONSHIPS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" " +
                "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" " +
                "Target=\"word/document.xml\"/>" +
                "</Relationships>"
    }
}
