package com.nanzhufeng.transcriber.engine

import kotlin.math.sqrt

/**
 * Conservative silence splitter for the local recogniser.
 *
 * It creates an utterance boundary only after a sustained acoustic pause. Short consonant and
 * breathing gaps stay within one utterance, so punctuation follows speaking rhythm rather than
 * a fixed text length.
 */
internal object SpeechPauseSegmenter {
    const val SAMPLE_RATE = 16_000
    private const val FRAME_MILLIS = 20
    private const val MIN_PAUSE_MILLIS = 380
    private const val CONTEXT_MILLIS = 120
    private const val MIN_VOICED_MILLIS = 80

    data class Utterance(
        val startSample: Int,
        val endSample: Int,
        val trailingPauseMillis: Long,
    )

    fun split(samples: FloatArray): List<Utterance> {
        if (samples.isEmpty()) return emptyList()
        val frameSize = SAMPLE_RATE * FRAME_MILLIS / 1_000
        val levels = samples.asList().chunked(frameSize).map(::rms)
        if (levels.isEmpty()) return emptyList()
        val threshold = speechThreshold(levels)
        val rawRuns = voicedRuns(levels.map { it >= threshold })
        if (rawRuns.isEmpty()) return emptyList()

        val mergedRuns = mutableListOf<IntRange>()
        rawRuns.filter { it.length() >= ceilFrames(MIN_VOICED_MILLIS) }.forEach { run ->
            val previous = mergedRuns.lastOrNull()
            if (previous != null && run.first - previous.last - 1 < ceilFrames(MIN_PAUSE_MILLIS)) {
                mergedRuns[mergedRuns.lastIndex] = previous.first..run.last
            } else {
                mergedRuns += run
            }
        }
        if (mergedRuns.isEmpty()) return emptyList()

        val contextSamples = SAMPLE_RATE * CONTEXT_MILLIS / 1_000
        return mergedRuns.mapIndexed { index, run ->
            val rawStart = run.first * frameSize
            val rawEnd = minOf(samples.size, (run.last + 1) * frameSize)
            val nextRawStart = mergedRuns.getOrNull(index + 1)?.first?.times(frameSize)
            Utterance(
                startSample = (rawStart - contextSamples).coerceAtLeast(0),
                endSample = (rawEnd + contextSamples).coerceAtMost(samples.size),
                trailingPauseMillis = nextRawStart
                    ?.let { ((it - rawEnd).coerceAtLeast(0) * 1_000L) / SAMPLE_RATE }
                    ?: 0L,
            )
        }
    }

    private fun speechThreshold(levels: List<Float>): Float {
        val sorted = levels.sorted()
        val floor = sorted[sorted.lastIndex / 5]
        val peak = sorted.last()
        return maxOf(0.0035f, minOf(floor * 2.5f, peak * 0.55f))
    }

    private fun voicedRuns(voiced: List<Boolean>): List<IntRange> {
        val runs = mutableListOf<IntRange>()
        var start = -1
        voiced.forEachIndexed { index, isVoiced ->
            when {
                isVoiced && start < 0 -> start = index
                !isVoiced && start >= 0 -> {
                    runs += start until index
                    start = -1
                }
            }
        }
        if (start >= 0) runs += start..voiced.lastIndex
        return runs
    }

    private fun IntRange.length(): Int = last - first + 1
    private fun ceilFrames(millis: Int): Int = (millis + FRAME_MILLIS - 1) / FRAME_MILLIS

    private fun rms(frame: List<Float>): Float {
        if (frame.isEmpty()) return 0f
        val meanSquare = frame.sumOf { sample -> (sample * sample).toDouble() } / frame.size
        return sqrt(meanSquare).toFloat()
    }
}
