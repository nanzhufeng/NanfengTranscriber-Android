package com.nanzhufeng.transcriber.domain.invocation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AsrCostEstimatorTest {
    @Test
    fun `new qwen filetrans uses reported tokens rather than old audio duration price`() {
        val estimate = requireNotNull(AsrCostEstimator.estimateQwenAudio31Cn(1_000_000, 1_000_000))
        assertEquals(3_500_000L, estimate.estimatedCostMicros)
        assertNull(AsrCostEstimator.estimateQwenAudio31Cn(null, 100))
        assertNull(AsrCostEstimator.estimateQwenAudio31Cn(100, null))
    }

    @Test
    fun `paraformer uses provider speech duration with a separate price version`() {
        val estimate = requireNotNull(AsrCostEstimator.estimateParaformerV2Cn(60_000))
        assertEquals(4_800L, estimate.estimatedCostMicros)
        assertNull(AsrCostEstimator.estimateParaformerV2Cn(null))
        assertEquals(0L, AsrCostEstimator.estimateParaformerV2Cn(0)?.estimatedCostMicros)
    }

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
