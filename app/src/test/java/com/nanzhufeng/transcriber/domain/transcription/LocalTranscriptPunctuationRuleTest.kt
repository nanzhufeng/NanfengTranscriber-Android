package com.nanzhufeng.transcriber.domain.transcription

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalTranscriptPunctuationRuleTest {
    @Test
    fun preservesModelPunctuation() {
        assertEquals(
            "本地模型已有标点，保持原样。",
            LocalTranscriptPunctuationRule.applyAfterPause(" 本地模型已有标点，保持原样。 ", 900L),
        )
    }

    @Test
    fun usesCommaOnlyForAShortMeasuredPause() {
        assertEquals("先说这一句，", LocalTranscriptPunctuationRule.applyAfterPause("先说这一句", 500L))
    }

    @Test
    fun usesSentenceEndingForALongMeasuredPause() {
        assertEquals("然后说下一句。", LocalTranscriptPunctuationRule.applyAfterPause("然后说下一句", 900L))
    }

    @Test
    fun doesNotInventPunctuationWithoutAnAcousticPause() {
        assertEquals("连续说完", LocalTranscriptPunctuationRule.applyAfterPause("连续说完", 0L))
    }

    @Test
    fun completesOnlyTheFinalDocumentResult() {
        assertEquals("完整结果。", LocalTranscriptPunctuationRule.ensureFinalTerminal("完整结果"))
        assertEquals("local result.", LocalTranscriptPunctuationRule.ensureFinalTerminal("local result"))
    }
}
