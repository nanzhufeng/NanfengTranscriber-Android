package com.nanzhufeng.transcriber.data.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sin

class StreamingMonoResamplerTest {
    @Test
    fun downsamplingConstantSignalKeepsExpectedDurationAcrossBlocks() {
        val resampler = StreamingMonoResampler(
            inputSampleRate = 48_000,
            inputChannels = 1,
        )

        val first = resampler.process(FloatArray(480) { 0.25f })
        val second = resampler.process(FloatArray(480) { 0.25f })

        assertEquals(160, first.size)
        assertEquals(160, second.size)
        assertArrayEquals(FloatArray(160) { 0.25f }, first, 0.00001f)
        assertArrayEquals(FloatArray(160) { 0.25f }, second, 0.00001f)
    }

    @Test
    fun stereoFramesAreDownmixedBeforeResampling() {
        val resampler = StreamingMonoResampler(
            inputSampleRate = 16_000,
            inputChannels = 2,
        )

        val output = resampler.process(
            floatArrayOf(
                1.0f, -1.0f,
                0.5f, 0.5f,
            ),
        )

        assertArrayEquals(floatArrayOf(0.0f, 0.5f), output, 0.00001f)
    }

    @Test
    fun chunkedAndSingleBlockResultsMatch() {
        val input = FloatArray(100) { index -> sin(index / 7.0).toFloat() }
        val single = StreamingMonoResampler(44_100, 1).process(input)
        val chunkedResampler = StreamingMonoResampler(44_100, 1)
        val chunked = listOf(
            input.copyOfRange(0, 30),
            input.copyOfRange(30, 70),
            input.copyOfRange(70, 100),
        ).flatMap { chunkedResampler.process(it).asList() }.toFloatArray()

        assertArrayEquals(single, chunked, 0.00001f)
    }
}
