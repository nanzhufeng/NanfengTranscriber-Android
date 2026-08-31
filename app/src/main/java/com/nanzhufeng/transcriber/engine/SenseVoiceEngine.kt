package com.nanzhufeng.transcriber.engine

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.nanzhufeng.transcriber.domain.transcription.LocalTranscriptPunctuationRule
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
        val utterances = SpeechPauseSegmenter.split(samples).ifEmpty {
            listOf(
                SpeechPauseSegmenter.Utterance(
                    startSample = 0,
                    endSample = samples.size,
                    trailingPauseMillis = 0L,
                ),
            )
        }
        var detectedLanguage: String? = null
        val segments = utterances.mapNotNull { utterance ->
            val result = recognize(
                recognizer = recognizer,
                samples = samples.copyOfRange(utterance.startSample, utterance.endSample),
            )
            detectedLanguage = detectedLanguage ?: result.lang.takeIf(String::isNotBlank)
            result.text.trim().takeIf(String::isNotBlank)?.let { text ->
                TranscriptSegment(
                    startMillis = utterance.startSample.toLong() * 1_000L / SAMPLE_RATE,
                    endMillis = utterance.endSample.toLong() * 1_000L / SAMPLE_RATE,
                    text = LocalTranscriptPunctuationRule.applyAfterPause(text, utterance.trailingPauseMillis),
                )
            }
        }
        return EngineTranscript(
            detectedLanguage = detectedLanguage,
            segments = segments,
        )
    }

    override fun releaseModel(model: LoadedModel) {
        (model.runtime as? SenseVoiceSession)?.close()
    }

    override fun close() = Unit

    private fun recognize(recognizer: OfflineRecognizer, samples: FloatArray) = recognizer.createStream().let { stream ->
        try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            recognizer.decode(stream)
            recognizer.getResult(stream)
        } finally {
            stream.release()
        }
    }

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
        const val SAMPLE_RATE = SpeechPauseSegmenter.SAMPLE_RATE
        const val TOKENS_FILE = "tokens.txt"
        const val AUTO_LANGUAGE = ""
    }
}
