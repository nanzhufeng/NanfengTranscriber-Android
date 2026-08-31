package com.nanzhufeng.transcriber.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPauseSegmenterTest {
    @Test
    fun splitsOnlyAtASustainedAcousticPause() {
        val samples = speech(1_000) + silence(620) + speech(1_000)

        val utterances = SpeechPauseSegmenter.split(samples)

        assertEquals(2, utterances.size)
        assertTrue(utterances.first().trailingPauseMillis in 580L..660L)
    }

    @Test
    fun keepsABriefSpeakingGapInsideOneUtterance() {
        val samples = speech(700) + silence(160) + speech(700)

        val utterances = SpeechPauseSegmenter.split(samples)

        assertEquals(1, utterances.size)
    }

    private fun speech(millis: Int): FloatArray = FloatArray(millis * 16) { 0.08f }
    private fun silence(millis: Int): FloatArray = FloatArray(millis * 16)
}
