package com.nanzhufeng.transcriber.domain.transcription

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptCompletionPolicyTest {
    @Test
    fun blankSegmentsAreNotReadableSpeech() {
        assertFalse(TranscriptCompletionPolicy.hasReadableSpeech(emptyList()))
        assertFalse(TranscriptCompletionPolicy.hasReadableSpeech(listOf("", "  ", "\n")))
    }

    @Test
    fun anyReadableSegmentCountsAsSpeech() {
        assertTrue(TranscriptCompletionPolicy.hasReadableSpeech(listOf("  ", "南枫转写")))
    }
}
