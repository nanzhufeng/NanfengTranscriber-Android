package com.nanzhufeng.transcriber.data.result

import com.nanzhufeng.transcriber.domain.export.TranscriptDocument
import com.nanzhufeng.transcriber.domain.export.TranscriptDocumentSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TranscriptDocumentStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun completeDocumentRoundTripsAndPartFileIsNotLeftBehind() {
        val target = temporaryFolder.root.toPath().resolve("result.json")
        val expected = StoredTranscript(
            document = TranscriptDocument(
                title = "测试文件",
                segments = listOf(
                    TranscriptDocumentSegment(0L, 1_250L, "第一段"),
                    TranscriptDocumentSegment(1_250L, 2_800L, "第二段"),
                ),
                polishedText = "整理后的全文",
            ),
            detectedLanguage = "zh",
            performanceSummary = "转写 1200 ms｜实时系数 0.43",
        )

        TranscriptDocumentStore().save(target, expected)

        assertEquals(expected, TranscriptDocumentStore().load(target))
        assertFalse(target.resolveSibling("result.json.part").toFile().exists())
    }
}
