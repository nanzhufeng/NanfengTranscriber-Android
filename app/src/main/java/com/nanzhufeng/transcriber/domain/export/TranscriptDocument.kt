package com.nanzhufeng.transcriber.domain.export

data class TranscriptDocument(
    val title: String,
    val segments: List<TranscriptDocumentSegment>,
    val polishedText: String? = null,
) {
    init {
        require(title.isNotBlank()) { "转写标题不能为空" }
        require(segments.all { it.endMillis >= it.startMillis }) { "转写片段时间范围无效" }
    }
}

data class TranscriptDocumentSegment(
    val startMillis: Long,
    val endMillis: Long,
    val text: String,
)

enum class TranscriptExportFormat {
    TXT,
    MARKDOWN,
    SRT,
    DOCX,
}
