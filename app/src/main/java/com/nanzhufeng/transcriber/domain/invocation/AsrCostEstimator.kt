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

    /** Beijing list price; only provider-reported token usage can produce an estimate. */
    fun estimateQwenAudio31Cn(inputTokens: Long?, outputTokens: Long?): AsrCostEstimate? {
        if (inputTokens == null || outputTokens == null || inputTokens < 0 || outputTokens < 0) return null
        val micros = BigDecimal.valueOf(inputTokens).multiply(BigDecimal("0.8"))
            .add(BigDecimal.valueOf(outputTokens).multiply(BigDecimal("2.7")))
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
        return AsrCostEstimate("qwen-audio-3.1-filetrans-cn-token-2026-10-09-v1", "CNY", micros)
    }

    /** Use speech duration reported by the provider, not the entire source file duration. */
    fun estimateParaformerV2Cn(reportedSpeechMillis: Long?): AsrCostEstimate? {
        if (reportedSpeechMillis == null || reportedSpeechMillis < 0) return null
        val micros = BigDecimal.valueOf(reportedSpeechMillis)
            .multiply(BigDecimal("0.08")).setScale(0, RoundingMode.HALF_UP).longValueExact()
        return AsrCostEstimate("paraformer-v2-cn-duration-2026-10-09-v1", "CNY", micros)
    }
}

data class AsrCostEstimate(
    val priceVersion: String,
    val currencyCode: String,
    val estimatedCostMicros: Long,
)
