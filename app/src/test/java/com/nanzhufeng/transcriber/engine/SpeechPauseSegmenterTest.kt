package com.nanzhufeng.transcriber.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPauseSegmenterTest {
    @Test
    fun preservesQuietSpeechAndBriefSyllablesRatherThanDiscardingSamples() {
        val samples = speech(60) + silence(700) + FloatArray(16_000) { 0.001f } +
            silence(700) + speech(1_000)

        val utterances = SpeechPauseSegmenter.split(samples)

        assertEquals(0, utterances.first().startSample)
        assertEquals(samples.size, utterances.last().endSample)
        assertEquals(samples.size, utterances.sumOf { it.endSample - it.startSample })
        utterances.zipWithNext().forEach { (previous, next) ->
            assertEquals(previous.endSample, next.startSample)
        }
    }

    @Test
    fun keepsShortPhrasesTogetherForRecognitionContext() {
        val samples = speech(1_000) + silence(620) + speech(1_000)

        assertEquals(1, SpeechPauseSegmenter.split(samples).size)
    }

    @Test
    fun boundsContinuousSpeechWithoutLosingItsTail() {
        val samples = speech(95_123)

        val utterances = SpeechPauseSegmenter.split(samples)

        assertEquals(4, utterances.size)
        assertTrue(utterances.all { it.endSample - it.startSample <= 30 * 16_000 })
        assertEquals(samples.size, utterances.sumOf { it.endSample - it.startSample })
    }

    @Test
    fun preservesEverySampleAcrossMultiplePausesIncludingAnIncompleteFrame() {
        val samples = silence(600) + speech(18_000) + silence(800) +
            speech(22_000) + silence(540) + speech(24_000) + FloatArray(13) { 0.001f }
        val reconstructed = SpeechPauseSegmenter.split(samples)
            .flatMap { samples.copyOfRange(it.startSample, it.endSample).asList() }
            .toFloatArray()

        org.junit.Assert.assertArrayEquals(samples, reconstructed, 0f)
    }

    @Test
    fun retainsAllQuietAudioAndHandlesEmptyInput() {
        val samples = silence(65_001)
        val utterances = SpeechPauseSegmenter.split(samples)

        assertTrue(SpeechPauseSegmenter.split(FloatArray(0)).isEmpty())
        assertEquals(0, utterances.first().startSample)
        assertEquals(samples.size, utterances.last().endSample)
        assertEquals(samples.size, utterances.sumOf { it.endSample - it.startSample })
        assertTrue(utterances.all { it.endSample - it.startSample in 1..480_000 })
    }

    @Test
    fun splitsOnlyAtASustainedAcousticPause() {
        val samples = speech(20_000) + silence(620) + speech(20_000)

        val utterances = SpeechPauseSegmenter.split(samples)

        assertEquals(2, utterances.size)
        assertTrue(utterances.first().trailingPauseMillis in 580L..660L)
        assertEquals(20_310 * 16, utterances.first().endSample)
        assertEquals(utterances.first().endSample, utterances.last().startSample)
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
