package com.nanzhufeng.transcriber.data.result

import com.nanzhufeng.transcriber.domain.export.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class TranscriptEditingAndAutoExportTest {
    @Test fun savedEditsSurviveReopenWithoutChangingTimestampsOrMetadata() {
        val directory = Files.createTempDirectory("transcript-edit")
        try {
            val path = directory.resolve("transcript.json")
            val original = StoredTranscript(TranscriptDocument("标题", listOf(TranscriptDocumentSegment(10, 900, "原文"))), "zh", "诊断")
            TranscriptDocumentStore().save(path, original)
            TranscriptDocumentStore().saveEdited(path, "修订第一行\n第二行")
            val reopened = TranscriptDocumentStore().load(path)
            assertEquals(original.document.segments, reopened.document.segments)
            assertEquals("诊断", reopened.performanceSummary)
            assertEquals("修订第一行\n第二行\n", TranscriptExportService().renderTxt(reopened.document))
            TranscriptDocumentStore().saveEdited(path, "")
            assertEquals("\n", TranscriptExportService().renderTxt(TranscriptDocumentStore().load(path).document))
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test fun reopeningOrRetryingCannotAutomaticallyExportTwice() {
        val directory = Files.createTempDirectory("automatic-export")
        try {
            var count = 0
            assertEquals("MD 已保存", AutomaticExportOnce().run(directory) { count++; "MD 已保存" })
            assertEquals("MD 已保存", AutomaticExportOnce().run(directory) { count++; "重复" })
            assertEquals(1, count)
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test fun failedOrInterruptedExportsAreNotRetriedAsNewCopies() {
        val directory = Files.createTempDirectory("automatic-export-failed")
        try {
            val message = AutomaticExportOnce().run(directory) { throw java.io.IOException("failed") }
            assertTrue(message.contains("失败"))
            assertEquals(message, AutomaticExportOnce().run(directory) { error("must not run") })
            Files.newBufferedWriter(directory.resolve("automatic-export.receipt")).use { it.write("") }
            assertTrue(AutomaticExportOnce().run(directory) { error("must not run") }.contains("中断"))
        } finally { directory.toFile().deleteRecursively() }
    }
}
