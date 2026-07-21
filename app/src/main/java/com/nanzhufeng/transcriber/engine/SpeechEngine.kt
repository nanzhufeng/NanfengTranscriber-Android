package com.nanzhufeng.transcriber.engine

import java.io.Closeable
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
)

data class EngineTranscript(
    val detectedLanguage: String?,
    val segments: List<TranscriptSegment>,
)

data class TranscriptSegment(
    val startMillis: Long,
    val endMillis: Long,
    val text: String,
)
