package com.nanzhufeng.transcriber.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryMediaPreviewStoreTest {
    @Test
    fun `mime type has priority over file extension`() {
        assertEquals(HistoryMediaKind.VIDEO, classifyHistoryMedia("recording.bin", "video/mp4"))
        assertEquals(HistoryMediaKind.AUDIO, classifyHistoryMedia("clip.mp4", "audio/aac"))
        assertEquals(HistoryMediaKind.IMAGE, classifyHistoryMedia("cover.bin", "image/webp"))
    }

    @Test
    fun `common local media extensions are classified without mime type`() {
        assertEquals(HistoryMediaKind.VIDEO, classifyHistoryMedia("样片.MOV", null))
        assertEquals(HistoryMediaKind.AUDIO, classifyHistoryMedia("采访.m4a", null))
        assertEquals(HistoryMediaKind.IMAGE, classifyHistoryMedia("封面.HEIC", null))
        assertEquals(HistoryMediaKind.UNKNOWN, classifyHistoryMedia("说明.txt", null))
    }
}
