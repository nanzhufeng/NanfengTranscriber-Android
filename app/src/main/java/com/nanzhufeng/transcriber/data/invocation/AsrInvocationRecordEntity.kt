package com.nanzhufeng.transcriber.data.invocation

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationRecord
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationStatus

@Entity(
    tableName = "asr_invocation_records",
    indices = [
        Index(value = ["completedAtMillis"]),
        Index(value = ["providerId", "modelId"]),
        Index(value = ["taskId"]),
    ],
)
data class AsrInvocationRecordEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val sourceDisplayName: String,
    val providerId: String,
    val modelId: String,
    val completedAtMillis: Long,
    val durationMillis: Long,
    val requestCount: Int,
    val billableAudioMillis: Long,
    val status: String,
    val inputTokens: Long?,
    val outputTokens: Long?,
    val totalTokens: Long?,
    val costPriceVersion: String?,
    val costCurrencyCode: String?,
    val estimatedCostMicros: Long?,
    val errorCode: String?,
)

internal fun AsrInvocationRecordEntity.toDomain() = AsrInvocationRecord(
    id = id,
    taskId = taskId,
    sourceDisplayName = sourceDisplayName,
    providerId = providerId,
    modelId = modelId,
    completedAtMillis = completedAtMillis,
    durationMillis = durationMillis,
    requestCount = requestCount,
    billableAudioMillis = billableAudioMillis,
    status = AsrInvocationStatus.valueOf(status),
    inputTokens = inputTokens,
    outputTokens = outputTokens,
    totalTokens = totalTokens,
    costPriceVersion = costPriceVersion,
    costCurrencyCode = costCurrencyCode,
    estimatedCostMicros = estimatedCostMicros,
    errorCode = errorCode,
)

internal fun AsrInvocationRecord.toEntity() = AsrInvocationRecordEntity(
    id = id,
    taskId = taskId,
    sourceDisplayName = sourceDisplayName,
    providerId = providerId,
    modelId = modelId,
    completedAtMillis = completedAtMillis,
    durationMillis = durationMillis,
    requestCount = requestCount,
    billableAudioMillis = billableAudioMillis,
    status = status.name,
    inputTokens = inputTokens,
    outputTokens = outputTokens,
    totalTokens = totalTokens,
    costPriceVersion = costPriceVersion,
    costCurrencyCode = costCurrencyCode,
    estimatedCostMicros = estimatedCostMicros,
    errorCode = errorCode,
)
