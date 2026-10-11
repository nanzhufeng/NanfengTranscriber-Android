package com.nanzhufeng.transcriber.engine

import kotlin.math.sqrt

/**
 * Lossless recognition windows. Energy is used only to choose a boundary, never to discard audio.
 * Inputs up to 30 seconds retain their full context, including quiet speech and brief syllables.
 */
internal object SpeechPauseSegmenter {
    const val SAMPLE_RATE = 16_000
    private const val FRAME_MILLIS = 20
    private const val MIN_PAUSE_MILLIS = 500
    private const val MIN_WINDOW_SAMPLES = SAMPLE_RATE * 15
    private const val MAX_WINDOW_SAMPLES = SAMPLE_RATE * 30

    data class Utterance(
        val startSample: Int,
        val endSample: Int,
        val trailingPauseMillis: Long,
    )

    fun split(samples: FloatArray): List<Utterance> {
        if (samples.isEmpty()) return emptyList()
        if (samples.size <= MAX_WINDOW_SAMPLES) {
            return listOf(Utterance(0, samples.size, 0L))
        }
        val frameSize = SAMPLE_RATE * FRAME_MILLIS / 1_000
        val levels = List((samples.size + frameSize - 1) / frameSize) { index ->
            val start = index * frameSize
            rms(samples, start, minOf(start + frameSize, samples.size))
        }
        val threshold = speechThreshold(levels)
        val pauses = runs(levels.map { it < threshold })
            .filter { it.last - it.first + 1 >= ceilFrames(MIN_PAUSE_MILLIS) }
        val utterances = mutableListOf<Utterance>()
        var startSample = 0
        while (samples.size - startSample > MAX_WINDOW_SAMPLES) {
            val limit = startSample + MAX_WINDOW_SAMPLES
            val pause = pauses.lastOrNull { run ->
                val midpoint = ((run.first.toLong() + run.last + 1) * frameSize / 2).toInt()
                midpoint in (startSample + MIN_WINDOW_SAMPLES)..limit
            }
            val endSample = pause?.let {
                ((it.first.toLong() + it.last + 1) * frameSize / 2).toInt()
            } ?: limit
            utterances += Utterance(
                startSample,
                endSample,
                pause?.let { (it.last - it.first + 1) * FRAME_MILLIS.toLong() } ?: 0L,
            )
            startSample = endSample
        }
        utterances += Utterance(startSample, samples.size, 0L)
        return utterances
    }

    private fun speechThreshold(levels: List<Float>): Float {
        val sorted = levels.sorted()
        val floor = sorted[sorted.lastIndex / 5]
        val peak = sorted.last()
        return maxOf(0.0035f, minOf(floor * 2.5f, peak * 0.55f))
    }

    private fun runs(voiced: List<Boolean>): List<IntRange> {
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

    private fun ceilFrames(millis: Int): Int = (millis + FRAME_MILLIS - 1) / FRAME_MILLIS

    private fun rms(samples: FloatArray, start: Int, end: Int): Float {
        var sum = 0.0
        for (index in start until end) {
            val sample = samples[index].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / (end - start)).toFloat()
    }
}
