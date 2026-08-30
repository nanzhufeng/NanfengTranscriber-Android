package com.nanzhufeng.transcriber.engine

import com.nanzhufeng.transcriber.data.modelstore.AsrProviderId

object SpeechEngineFactory {
    fun create(provider: AsrProviderId, threadCount: Int, qwen3AsrEngine: SpeechEngine): SpeechEngine = when (provider) {
        AsrProviderId.SENSEVOICE -> SenseVoiceEngine(threadCount = threadCount)
        AsrProviderId.QWEN3_ASR_API -> qwen3AsrEngine
    }
}
