package com.nanzhufeng.transcriber.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class TranscriptExportServiceTest {
    private val service = TranscriptExportService()
    private val document = TranscriptDocument(
        title = "示例视频",
        segments = listOf(
            TranscriptDocumentSegment(0L, 1_000L, " 第一句  "),
            TranscriptDocumentSegment(1_200L, 2_000L, "第二句"),
            TranscriptDocumentSegment(4_000L, 5_250L, "结束。"),
        ),
    )

    @Test
    fun txtAndMarkdownMergeReadableSegmentsLikeWindowsVersion() {
        assertEquals("第一句，第二句\n结束。\n", service.renderTxt(document))
        assertEquals(
            """# 示例视频

                || 时间 | 内容 |
                || --- | --- |
                || 00:00:00.000 | 第一句，第二句 |
                || 00:00:04.000 | 结束。 |
                |
            """.trimMargin(),
            service.renderMarkdown(document),
        )
    }

    @Test
    fun srtKeepsOriginalSegmentRhythmAndCommaMilliseconds() {
        assertEquals(
            """1
                |00:00:00,000 --> 00:00:01,000
                |第一句
                |
                |2
                |00:00:01,200 --> 00:00:02,000
                |第二句
                |
                |3
                |00:00:04,000 --> 00:00:05,250
                |结束。
                |
            """.trimMargin(),
            service.renderSrt(document),
        )
    }

    @Test
    fun docxIsARealOpenXmlPackageWithEscapedText() {
        val output = ByteArrayOutputStream()
        service.export(
            document.copy(title = "A&B", polishedText = "第一段\n第二段 <完成>"),
            TranscriptExportFormat.DOCX,
            output,
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        assertEquals(
            setOf("[Content_Types].xml", "_rels/.rels", "word/document.xml"),
            entries.keys,
        )
        val documentXml = requireNotNull(entries["word/document.xml"])
        assertTrue("A&amp;B" in documentXml)
        assertTrue("第二段 &lt;完成&gt;" in documentXml)
    }
}
