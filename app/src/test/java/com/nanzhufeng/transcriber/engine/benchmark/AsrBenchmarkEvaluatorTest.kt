package com.nanzhufeng.transcriber.engine.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AsrBenchmarkEvaluatorTest {
    @Test
    fun mixedChineseEnglishMetricsKeepProfessionalTermsVisible() {
        val metrics = AsrBenchmarkEvaluator.evaluate(
            reference = "使用 After Effects 和 Qwen3-ASR 完成中英混合转写。",
            hypothesis = "使用 After Effect 和 Qwen3 ASR 完成中英混合转写",
            terminology = listOf("After Effects", "Qwen3-ASR", "VFX"),
        )

        assertEquals(3, metrics.terminologyCount)
        assertEquals(0, metrics.matchedTerminologyCount)
        assertEquals(0.0, metrics.terminologyRetentionRate!!, 0.0001)
        assertEquals(1.0, metrics.englishWordErrorRate!!, 0.0001)
        assertEquals(1.0 / 31.0, metrics.characterErrorRate!!, 0.0001)
    }

    @Test
    fun missingReferenceDimensionIsReportedAsUnavailable() {
        val metrics = AsrBenchmarkEvaluator.evaluate("中文", "中文")

        assertEquals(0.0, metrics.characterErrorRate!!, 0.0001)
        assertNull(metrics.englishWordErrorRate)
        assertNull(metrics.terminologyRetentionRate)
    }

    @Test
    fun realTimeFactorUsesWholeProcessingDuration() {
        val record = AsrBenchmarkRecord(
            sampleId = "sample-01",
            providerId = "sensevoice",
            modelId = "sensevoice-small-int8",
            modelVersion = "v1",
            device = "OPPO Find N5",
            durationMillis = 60_000L,
            processingMillis = 15_000L,
            peakPssBytes = null,
            startBatteryPercent = null,
            endBatteryPercent = null,
            startTemperatureCelsius = null,
            endTemperatureCelsius = null,
            metrics = AsrBenchmarkEvaluator.evaluate("测试", "测试"),
        )

        assertEquals(0.25, record.realTimeFactor!!, 0.0001)
    }
}
