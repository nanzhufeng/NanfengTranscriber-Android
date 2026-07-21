package com.nanzhufeng.transcriber.data.media

import kotlin.math.floor

class StreamingMonoResampler(
    private val inputSampleRate: Int,
    private val inputChannels: Int,
    private val targetSampleRate: Int = 16_000,
) {
    private var sourceFrameBase = 0L
    private var nextSourcePosition = 0.0
    private var previousMonoSample: Float? = null

    init {
        require(inputSampleRate > 0) { "输入采样率必须大于 0" }
        require(inputChannels > 0) { "输入声道数必须大于 0" }
        require(targetSampleRate > 0) { "目标采样率必须大于 0" }
    }

    fun process(interleavedSamples: FloatArray): FloatArray {
        require(interleavedSamples.size % inputChannels == 0) { "PCM 样本没有按完整帧对齐" }
        val frameCount = interleavedSamples.size / inputChannels
        if (frameCount == 0) return FloatArray(0)

        val mono = FloatArray(frameCount) { frame ->
            var sum = 0.0f
            val offset = frame * inputChannels
            for (channel in 0 until inputChannels) sum += interleavedSamples[offset + channel]
            sum / inputChannels
        }

        val lastGlobalIndex = sourceFrameBase + frameCount - 1L
        val estimated = (frameCount.toDouble() * targetSampleRate / inputSampleRate).toInt() + 2
        var output = FloatArray(estimated.coerceAtLeast(2))
        var outputSize = 0
        val step = inputSampleRate.toDouble() / targetSampleRate

        while (nextSourcePosition <= lastGlobalIndex.toDouble()) {
            val leftIndex = floor(nextSourcePosition).toLong()
            val fraction = (nextSourcePosition - leftIndex).toFloat()
            val rightIndex = leftIndex + 1L
            if (fraction > 0f && rightIndex > lastGlobalIndex) break

            val left = sampleAt(leftIndex, mono)
            val right = if (fraction == 0f) left else sampleAt(rightIndex, mono)
            if (outputSize == output.size) output = output.copyOf(output.size * 2)
            output[outputSize++] = left + (right - left) * fraction
            nextSourcePosition += step
        }

        sourceFrameBase += frameCount
        previousMonoSample = mono.last()
        return output.copyOf(outputSize)
    }

    private fun sampleAt(globalIndex: Long, mono: FloatArray): Float {
        if (globalIndex == sourceFrameBase - 1L) {
            return requireNotNull(previousMonoSample) { "重采样缺少上一块边界样本" }
        }
        val localIndex = (globalIndex - sourceFrameBase).toInt()
        require(localIndex in mono.indices) { "重采样访问越界：$globalIndex" }
        return mono[localIndex]
    }
}
