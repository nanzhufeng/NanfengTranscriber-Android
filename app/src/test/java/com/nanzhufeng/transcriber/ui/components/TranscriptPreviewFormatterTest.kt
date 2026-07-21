package com.nanzhufeng.transcriber.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptPreviewFormatterTest {
    @Test
    fun `空结果显示诚实占位`() {
        assertEquals("暂无可显示的文字", formatTranscriptPreview("  \n  "))
    }

    @Test
    fun `预览压缩换行和重复空格`() {
        assertEquals("第一句 第二句 第三句", formatTranscriptPreview("第一句\n  第二句\t第三句"))
    }

    @Test
    fun `超长预览保留长度并显示省略号`() {
        assertEquals("1234567…", formatTranscriptPreview("1234567890", maxCharacters = 8))
    }
}
