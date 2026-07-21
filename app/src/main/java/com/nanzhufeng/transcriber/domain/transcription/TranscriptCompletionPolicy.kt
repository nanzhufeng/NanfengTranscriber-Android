package com.nanzhufeng.transcriber.domain.transcription

object TranscriptCompletionPolicy {
    fun hasReadableSpeech(segmentTexts: Iterable<String>): Boolean =
        segmentTexts.any { it.isNotBlank() }
}
