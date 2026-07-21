package com.nanzhufeng.transcriber.data.task

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcription_tasks",
    indices = [
        Index(value = ["state"]),
        Index(value = ["queuePosition"]),
        Index(value = ["sourceFingerprint"]),
    ],
)
data class TranscriptionTaskEntity(
    @PrimaryKey val id: String,
    val sourceUri: String,
    val sourceDisplayName: String,
    val sourceAccessMode: String,
    val stagedInputPath: String?,
    val sourceFingerprint: String?,
    val modelId: String,
    val modelVersion: String,
    val language: String?,
    val threadCount: Int,
    val outputFormat: String,
    val outputUri: String?,
    val exportDirectoryUri: String?,
    val outputConflictPolicy: String,
    val postProcessEnabled: Boolean,
    val postProcessBaseUrl: String,
    val postProcessModel: String,
    val state: String,
    val progressMillis: Long,
    val totalDurationMillis: Long?,
    val errorCode: String?,
    val userMessage: String?,
    val technicalDetail: String?,
    val attemptCount: Int,
    val queuePosition: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

enum class SourceAccessMode {
    PERSISTED_DOCUMENT,
    PRIVATE_STAGING,
    RESELECT_REQUIRED,
}

enum class TranscriptionOutputFormat {
    TXT,
    MARKDOWN,
    SRT,
    DOCX,
}

enum class OutputConflictPolicy {
    RENAME,
    OVERWRITE,
    SKIP,
}

data class NewTranscriptionTask(
    val sourceUri: String,
    val sourceDisplayName: String,
    val sourceAccessMode: SourceAccessMode,
    val stagedInputPath: String? = null,
    val sourceFingerprint: String? = null,
    val modelId: String,
    val modelVersion: String,
    val language: String? = null,
    val threadCount: Int = 0,
    val outputFormat: TranscriptionOutputFormat,
    val outputUri: String? = null,
    val exportDirectoryUri: String? = null,
    val outputConflictPolicy: OutputConflictPolicy = OutputConflictPolicy.RENAME,
    val postProcessEnabled: Boolean = false,
    val postProcessBaseUrl: String = "https://api.openai.com/v1",
    val postProcessModel: String = "gpt-4o-mini",
    val queuePosition: Long,
) {
    init {
        require(sourceUri.isNotBlank()) { "sourceUri 不能为空" }
        require(sourceDisplayName.isNotBlank()) { "sourceDisplayName 不能为空" }
        require(modelId.isNotBlank()) { "modelId 不能为空" }
        require(modelVersion.isNotBlank()) { "modelVersion 不能为空" }
        require(threadCount in 0..12) { "threadCount 必须在 0 到 12 之间" }
        require(queuePosition >= 0L) { "queuePosition 不能小于 0" }
        require(!postProcessEnabled || postProcessBaseUrl.startsWith("https://")) {
            "翻译润色服务地址必须使用 HTTPS"
        }
        require(!postProcessEnabled || postProcessModel.isNotBlank()) { "翻译润色模型名不能为空" }
        if (sourceAccessMode == SourceAccessMode.PRIVATE_STAGING) {
            require(!stagedInputPath.isNullOrBlank()) { "私有暂存输入必须记录路径" }
        }
    }
}
