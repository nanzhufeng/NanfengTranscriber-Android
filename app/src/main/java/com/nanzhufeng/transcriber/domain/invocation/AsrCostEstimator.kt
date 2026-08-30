package com.nanzhufeng.transcriber.domain.invocation

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Versioned local fallback, following 南枫 AI's immutable accounting approach.
 *
 * Qwen3-ASR-Flash's fixed China endpoint is billed by submitted audio duration, not output
 * tokens. This remains an estimate: promotions, free quota and the provider settlement win.
 */
object AsrCostEstimator {
    const val QWEN_ASR_CN_PRICE_VERSION = "qwen-asr-cn-duration-2026-08-v1"

    private val qwenCnYuanPerSecond = BigDecimal("0.00022")
    private val microsPerYuan = BigDecimal("1000000")

    fun estimateQwenAsrCn(billableAudioMillis: Long): AsrCostEstimate? {
        if (billableAudioMillis <= 0L) return null
        val estimatedMicros = BigDecimal.valueOf(billableAudioMillis)
            .multiply(qwenCnYuanPerSecond)
            .multiply(microsPerYuan)
            .divide(BigDecimal("1000"), 0, RoundingMode.HALF_UP)
            .longValueExact()
        return AsrCostEstimate(
            priceVersion = QWEN_ASR_CN_PRICE_VERSION,
            currencyCode = "CNY",
            estimatedCostMicros = estimatedMicros,
        )
    }
}

data class AsrCostEstimate(
    val priceVersion: String,
    val currencyCode: String,
    val estimatedCostMicros: Long,
)
