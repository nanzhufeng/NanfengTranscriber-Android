package com.nanzhufeng.transcriber.ui.components

private val COLLAPSIBLE_WHITESPACE = Regex("\\s+")

fun formatTranscriptPreview(text: String?, maxCharacters: Int = 96): String {
    require(maxCharacters >= 8) { "文字预览长度不能小于 8" }
    val normalized = text
        ?.trim()
        ?.replace(COLLAPSIBLE_WHITESPACE, " ")
        .orEmpty()
    if (normalized.isBlank()) return "暂无可显示的文字"
    if (normalized.length <= maxCharacters) return normalized
    return normalized.take(maxCharacters - 1).trimEnd() + "…"
}
