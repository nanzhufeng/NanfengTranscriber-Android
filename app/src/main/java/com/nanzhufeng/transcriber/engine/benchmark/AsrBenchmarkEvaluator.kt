package com.nanzhufeng.transcriber.engine.benchmark

import java.util.Locale

object AsrBenchmarkEvaluator {
    fun evaluate(
        reference: String,
        hypothesis: String,
        terminology: List<String> = emptyList(),
    ): AsrAccuracyMetrics {
        val referenceChars = normalizeForCer(reference).toList()
        val hypothesisChars = normalizeForCer(hypothesis).toList()
        val referenceWords = englishTokens(reference)
        val hypothesisWords = englishTokens(hypothesis)
        val normalizedHypothesis = hypothesis.lowercase(Locale.ROOT)
        val normalizedTerms = terminology
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase(Locale.ROOT) }
        val matchedTerms = normalizedTerms.count { term ->
            normalizedHypothesis.contains(term.lowercase(Locale.ROOT))
        }

        return AsrAccuracyMetrics(
            characterErrorRate = errorRate(referenceChars, hypothesisChars),
            englishWordErrorRate = errorRate(referenceWords, hypothesisWords),
            terminologyRetentionRate = if (normalizedTerms.isEmpty()) {
                null
            } else {
                matchedTerms.toDouble() / normalizedTerms.size
            },
            referenceCharacterCount = referenceChars.size,
            referenceEnglishWordCount = referenceWords.size,
            terminologyCount = normalizedTerms.size,
            matchedTerminologyCount = matchedTerms,
        )
    }

    private fun normalizeForCer(text: String): String = buildString(text.length) {
        text.lowercase(Locale.ROOT).forEach { character ->
            if (character.isLetterOrDigit()) append(character)
        }
    }

    private fun englishTokens(text: String): List<String> = ENGLISH_TOKEN
        .findAll(text.lowercase(Locale.ROOT))
        .map(MatchResult::value)
        .toList()

    private fun <T> errorRate(reference: List<T>, hypothesis: List<T>): Double? {
        if (reference.isEmpty()) return null
        var previous = IntArray(hypothesis.size + 1) { it }
        reference.forEachIndexed { referenceIndex, referenceValue ->
            val current = IntArray(hypothesis.size + 1)
            current[0] = referenceIndex + 1
            hypothesis.forEachIndexed { hypothesisIndex, hypothesisValue ->
                val substitution = previous[hypothesisIndex] +
                    if (referenceValue == hypothesisValue) 0 else 1
                val deletion = previous[hypothesisIndex + 1] + 1
                val insertion = current[hypothesisIndex] + 1
                current[hypothesisIndex + 1] = minOf(substitution, deletion, insertion)
            }
            previous = current
        }
        return previous.last().toDouble() / reference.size
    }

    private val ENGLISH_TOKEN = Regex("[a-z0-9]+(?:[._+\\-][a-z0-9]+)*")
}

data class AsrAccuracyMetrics(
    val characterErrorRate: Double?,
    val englishWordErrorRate: Double?,
    val terminologyRetentionRate: Double?,
    val referenceCharacterCount: Int,
    val referenceEnglishWordCount: Int,
    val terminologyCount: Int,
    val matchedTerminologyCount: Int,
)

data class AsrBenchmarkRecord(
    val sampleId: String,
    val providerId: String,
    val modelId: String,
    val modelVersion: String,
    val device: String,
    val durationMillis: Long,
    val processingMillis: Long,
    val peakPssBytes: Long?,
    val startBatteryPercent: Int?,
    val endBatteryPercent: Int?,
    val startTemperatureCelsius: Double?,
    val endTemperatureCelsius: Double?,
    val metrics: AsrAccuracyMetrics,
) {
    val realTimeFactor: Double?
        get() = durationMillis.takeIf { it > 0L }?.let { processingMillis.toDouble() / it }
}
