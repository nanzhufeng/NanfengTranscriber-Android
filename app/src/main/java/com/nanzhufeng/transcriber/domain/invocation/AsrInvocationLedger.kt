package com.nanzhufeng.transcriber.domain.invocation

/**
 * Content-free audit fact for one complete source-file cloud ASR attempt.
 *
 * Audio bytes, transcript text, request payload and credentials are deliberately not part of
 * this contract. A record is written only after the HTTP request has actually been attempted.
 */
data class AsrInvocationRecord(
    val id: String,
    val taskId: String,
    val sourceDisplayName: String,
    val providerId: String,
    val modelId: String,
    val completedAtMillis: Long,
    val durationMillis: Long,
    val requestCount: Int,
    val billableAudioMillis: Long,
    val status: AsrInvocationStatus,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val totalTokens: Long? = null,
    val costPriceVersion: String? = null,
    val costCurrencyCode: String? = null,
    val estimatedCostMicros: Long? = null,
    val errorCode: String? = null,
)

enum class AsrInvocationStatus {
    SUCCEEDED,
    FAILED,
}

interface AsrInvocationRecorder {
    suspend fun record(record: AsrInvocationRecord)
}

object NoopAsrInvocationRecorder : AsrInvocationRecorder {
    override suspend fun record(record: AsrInvocationRecord) = Unit
}
