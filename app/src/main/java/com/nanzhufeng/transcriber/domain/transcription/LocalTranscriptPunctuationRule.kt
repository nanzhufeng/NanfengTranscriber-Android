package com.nanzhufeng.transcriber.domain.transcription

/** Punctuation follows the measured pause between two locally recognised utterances. */
object LocalTranscriptPunctuationRule {
    private const val SENTENCE_PAUSE_MILLIS = 800L
    private val existingPunctuation = setOf('，', '。', '！', '？', '；', '：', '、', ',', '.', '!', '?', ';', ':')
    private val sentenceEndings = setOf('。', '！', '？', '.', '!', '?', '…')

    /** Adds one separator only when the audio analyser found a real pause after this utterance. */
    fun applyAfterPause(text: String, pauseMillis: Long): String {
        val normalized = normalize(text)
        if (normalized.isBlank() || normalized.last() in existingPunctuation) return normalized
        if (pauseMillis <= 0L) return normalized
        return normalized + separatorFor(normalized, pauseMillis)
    }

    /** Used once after all audio chunks are joined, never at an arbitrary chunk boundary. */
    fun ensureFinalTerminal(text: String): String {
        val normalized = normalize(text)
        if (normalized.isBlank() || normalized.last() in sentenceEndings) return normalized
        return normalized + if (normalized.any(::isCjk)) '。' else '.'
    }

    private fun separatorFor(text: String, pauseMillis: Long): Char = when {
        pauseMillis >= SENTENCE_PAUSE_MILLIS && text.any(::isCjk) -> '。'
        pauseMillis >= SENTENCE_PAUSE_MILLIS -> '.'
        text.any(::isCjk) -> '，'
        else -> ','
    }

    private fun normalize(text: String): String = text.trim().replace(WHITESPACE, " ")

    private fun isCjk(character: Char): Boolean = character in '\u3400'..'\u9FFF'

    private val WHITESPACE = Regex("\\s+")
}
