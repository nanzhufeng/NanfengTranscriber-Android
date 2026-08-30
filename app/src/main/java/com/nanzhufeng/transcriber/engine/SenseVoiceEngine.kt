package com.nanzhufeng.transcriber.engine

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import java.nio.file.Files
import java.nio.file.Path

class SenseVoiceEngine(
    private val threadCount: Int,
) : SpeechEngine {
    override suspend fun loadModel(modelPath: Path): LoadedModel {
        val canonicalModel = modelPath.toRealPath()
        val tokens = canonicalModel.resolveSibling(TOKENS_FILE)
        require(Files.isRegularFile(tokens)) { "SenseVoice tokens.txt 不存在或不可读" }
        val session = SenseVoiceSession(
            modelPath = canonicalModel,
            tokensPath = tokens.toRealPath(),
            threadCount = threadCount,
        )
        session.ensureRecognizer(language = null)
        return LoadedModel(
            canonicalPath = canonicalModel,
            nativeHandle = 0L,
            runtime = session,
        )
    }

    override suspend fun transcribe(
        model: LoadedModel,
        samples: FloatArray,
        language: String?,
    ): EngineTranscript {
        val session = model.runtime as? SenseVoiceSession
            ?: error("SenseVoice 模型会话无效")
        val recognizer = session.ensureRecognizer(language)
        val result = recognizer.createStream().let { stream ->
            try {
                stream.acceptWaveform(samples, SAMPLE_RATE)
                recognizer.decode(stream)
                recognizer.getResult(stream)
            } finally {
                stream.release()
            }
        }
        val text = result.text.trim()
        val durationMillis = samples.size.toLong() * 1_000L / SAMPLE_RATE
        return EngineTranscript(
            detectedLanguage = result.lang.takeIf(String::isNotBlank),
            segments = if (text.isBlank()) {
                emptyList()
            } else {
                listOf(TranscriptSegment(0L, durationMillis.coerceAtLeast(1L), text))
            },
        )
    }

    override fun releaseModel(model: LoadedModel) {
        (model.runtime as? SenseVoiceSession)?.close()
    }

    override fun close() = Unit

    private class SenseVoiceSession(
        private val modelPath: Path,
        private val tokensPath: Path,
        private val threadCount: Int,
    ) {
        private var language: String = AUTO_LANGUAGE
        private var recognizer: OfflineRecognizer? = null

        fun ensureRecognizer(language: String?): OfflineRecognizer {
            val normalizedLanguage = normalizeLanguage(language)
            recognizer?.takeIf { this.language == normalizedLanguage }?.let { return it }
            recognizer?.release()
            val modelConfig = OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = modelPath.toString(),
                    language = normalizedLanguage,
                    useInverseTextNormalization = true,
                ),
                tokens = tokensPath.toString(),
                numThreads = threadCount,
                debug = false,
                provider = "cpu",
            )
            return OfflineRecognizer(
                config = OfflineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                    modelConfig = modelConfig,
                ),
            ).also {
                recognizer = it
                this.language = normalizedLanguage
            }
        }

        fun close() {
            recognizer?.release()
            recognizer = null
        }

        private fun normalizeLanguage(language: String?): String = when (language?.lowercase()) {
            "zh", "en", "ja", "ko", "yue" -> language.lowercase()
            else -> AUTO_LANGUAGE
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val TOKENS_FILE = "tokens.txt"
        const val AUTO_LANGUAGE = ""
    }
}
