package com.nanzhufeng.transcriber.domain.invocation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AsrCostEstimatorTest {
    @Test
    fun `estimates qwen ASR China duration with a versioned CNY price`() {
        val estimate = requireNotNull(AsrCostEstimator.estimateQwenAsrCn(60_000L))

        assertEquals("qwen-asr-cn-duration-2026-08-v1", estimate.priceVersion)
        assertEquals("CNY", estimate.currencyCode)
        assertEquals(13_200L, estimate.estimatedCostMicros)
    }

    @Test
    fun `does not fabricate an amount without submitted audio`() {
        assertNull(AsrCostEstimator.estimateQwenAsrCn(0L))
    }
}
