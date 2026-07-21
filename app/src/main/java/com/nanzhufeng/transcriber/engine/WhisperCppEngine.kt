package com.nanzhufeng.transcriber.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.file.Path

class WhisperCppEngine(
    private val useGpu: Boolean,
    private val threadCount: Int,
) : SpeechEngine {
    override suspend fun loadModel(modelPath: Path): LoadedModel = withContext(Dispatchers.Default) {
        val canonicalPath = modelPath.toRealPath()
        val handle = NativeWhisperBridge.loadModel(canonicalPath.toString(), useGpu)
        check(handle != 0L) { "模型加载失败：${canonicalPath.fileName}" }
        LoadedModel(canonicalPath, handle)
    }

    override suspend fun transcribe(
        model: LoadedModel,
        samples: FloatArray,
        language: String?,
    ): EngineTranscript = withContext(Dispatchers.Default) {
        val payload = JSONObject(
            NativeWhisperBridge.transcribe(
                handle = model.nativeHandle,
                samples = samples,
                language = language,
                threads = threadCount,
            ),
        )
        check(payload.optBoolean("ok")) {
            payload.optString("error", "whisper.cpp 推理失败")
        }
        val segmentsJson = payload.getJSONArray("segments")
        val segments = buildList {
            for (index in 0 until segmentsJson.length()) {
                val segment = segmentsJson.getJSONObject(index)
                add(
                    TranscriptSegment(
                        startMillis = segment.getLong("startMillis"),
                        endMillis = segment.getLong("endMillis"),
                        text = segment.getString("text"),
                    ),
                )
            }
        }
        EngineTranscript(
            detectedLanguage = payload.optString("language").takeIf(String::isNotBlank),
            segments = segments,
        )
    }

    override fun releaseModel(model: LoadedModel) {
        if (model.nativeHandle != 0L) NativeWhisperBridge.freeModel(model.nativeHandle)
    }

    override fun cancel(model: LoadedModel) {
        if (model.nativeHandle != 0L) NativeWhisperBridge.cancelTranscription(model.nativeHandle)
    }

    override fun prepare(model: LoadedModel) {
        if (model.nativeHandle != 0L) NativeWhisperBridge.prepareTranscription(model.nativeHandle)
    }

    override fun close() = Unit
}
