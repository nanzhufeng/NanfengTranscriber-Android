package com.nanzhufeng.transcriber.engine

import java.io.Closeable
import java.io.IOException
import java.nio.file.Path

interface SpeechEngine : Closeable {
    suspend fun loadModel(modelPath: Path): LoadedModel

    suspend fun transcribe(
        model: LoadedModel,
        samples: FloatArray,
        language: String?,
    ): EngineTranscript

    fun releaseModel(model: LoadedModel)

    fun cancel(model: LoadedModel) = Unit

    fun prepare(model: LoadedModel) = Unit
}

data class LoadedModel(
    val canonicalPath: Path,
    val nativeHandle: Long,
    val runtime: Any? = null,
)

data class EngineTranscript(
    val detectedLanguage: String?,
    val segments: List<TranscriptSegment>,
    /** Safe provider usage for this internal audio segment, if the provider returned it. */
    val invocationUsage: EngineInvocationUsage? = null,
)

data class EngineInvocationUsage(
    val requestCount: Int = 0,
    val billableAudioMillis: Long = 0L,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val totalTokens: Long? = null,
)

data class TranscriptSegment(
    val startMillis: Long,
    val endMillis: Long,
    val text: String,
)

/**
 * 推理引擎向任务层传递的、可安全展示给用户的失败。
 * 请求体、音频内容和 API Key 不得作为技术详情持久化。
 */
class EngineTranscriptionException(
    val userMessage: String,
    val errorCode: String,
    val safeTechnicalDetail: String? = null,
    val providerRequestAttempted: Boolean = false,
    val providerAttemptedAudioMillis: Long = 0L,
) : IOException(userMessage)
