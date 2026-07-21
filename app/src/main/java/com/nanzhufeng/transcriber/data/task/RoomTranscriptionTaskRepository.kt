package com.nanzhufeng.transcriber.data.task

import androidx.room.withTransaction
import com.nanzhufeng.transcriber.domain.task.TaskTransitionPolicy
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RoomTranscriptionTaskRepository(
    private val database: TranscriptionDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = database.taskDao()

    fun observeAll(): Flow<List<TranscriptionTaskEntity>> = dao.observeAll()

    suspend fun findById(id: String): TranscriptionTaskEntity? = dao.findById(id)

    suspend fun findNextQueued(): TranscriptionTaskEntity? =
        dao.findFirstByState(TranscriptionTaskState.QUEUED.name)

    suspend fun delete(id: String): Boolean = dao.deleteById(id) == 1

    suspend fun create(draft: NewTranscriptionTask): TranscriptionTaskEntity {
        val now = nowMillis()
        val task = TranscriptionTaskEntity(
            id = newId(),
            sourceUri = draft.sourceUri,
            sourceDisplayName = draft.sourceDisplayName,
            sourceAccessMode = draft.sourceAccessMode.name,
            stagedInputPath = draft.stagedInputPath,
            sourceFingerprint = draft.sourceFingerprint,
            modelId = draft.modelId,
            modelVersion = draft.modelVersion,
            language = draft.language,
            threadCount = draft.threadCount,
            outputFormat = draft.outputFormat.name,
            outputUri = draft.outputUri,
            exportDirectoryUri = draft.exportDirectoryUri,
            outputConflictPolicy = draft.outputConflictPolicy.name,
            postProcessEnabled = draft.postProcessEnabled,
            postProcessBaseUrl = draft.postProcessBaseUrl,
            postProcessModel = draft.postProcessModel,
            state = TranscriptionTaskState.QUEUED.name,
            progressMillis = 0L,
            totalDurationMillis = null,
            errorCode = null,
            userMessage = null,
            technicalDetail = null,
            attemptCount = 0,
            queuePosition = draft.queuePosition,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        dao.insert(task)
        return task
    }

    suspend fun transition(
        id: String,
        target: TranscriptionTaskState,
        progressMillis: Long? = null,
        totalDurationMillis: Long? = null,
        errorCode: String? = null,
        userMessage: String? = null,
        technicalDetail: String? = null,
        outputUri: String? = null,
    ): TaskMutationResult = database.withTransaction {
        val current = dao.findById(id)
            ?: return@withTransaction TaskMutationResult.NotFound(id)
        val currentState = runCatching { TranscriptionTaskState.valueOf(current.state) }
            .getOrElse {
                return@withTransaction TaskMutationResult.Rejected(
                    task = current,
                    reason = "任务状态损坏：${current.state}",
                )
            }
        if (!TaskTransitionPolicy.canTransition(currentState, target)) {
            return@withTransaction TaskMutationResult.Rejected(
                task = current,
                reason = "不允许从 ${currentState.name} 直接进入 ${target.name}",
            )
        }

        val updated = current.copy(
            state = target.name,
            progressMillis = progressMillis ?: current.progressMillis,
            totalDurationMillis = totalDurationMillis ?: current.totalDurationMillis,
            errorCode = errorCode,
            userMessage = userMessage,
            technicalDetail = technicalDetail,
            outputUri = outputUri ?: current.outputUri,
            attemptCount = if (target == TranscriptionTaskState.PREPARING) {
                current.attemptCount + 1
            } else {
                current.attemptCount
            },
            updatedAtMillis = nowMillis(),
        )
        check(dao.update(updated) == 1) { "任务更新数量异常" }
        TaskMutationResult.Updated(updated)
    }

    suspend fun updateProgress(
        id: String,
        expectedState: TranscriptionTaskState,
        progressMillis: Long,
        totalDurationMillis: Long? = null,
        userMessage: String? = null,
    ): TaskMutationResult = database.withTransaction {
        val current = dao.findById(id)
            ?: return@withTransaction TaskMutationResult.NotFound(id)
        if (current.state != expectedState.name) {
            return@withTransaction TaskMutationResult.Rejected(
                task = current,
                reason = "任务已不处于 ${expectedState.name}，忽略过期进度",
            )
        }
        val updated = current.copy(
            progressMillis = progressMillis.coerceAtLeast(0L),
            totalDurationMillis = totalDurationMillis ?: current.totalDurationMillis,
            userMessage = userMessage ?: current.userMessage,
            updatedAtMillis = nowMillis(),
        )
        check(dao.update(updated) == 1) { "任务进度更新数量异常" }
        TaskMutationResult.Updated(updated)
    }

    suspend fun resetForExecution(id: String): TaskMutationResult = database.withTransaction {
        var current = dao.findById(id)
            ?: return@withTransaction TaskMutationResult.NotFound(id)
        var state = runCatching { TranscriptionTaskState.valueOf(current.state) }
            .getOrElse {
                return@withTransaction TaskMutationResult.Rejected(
                    task = current,
                    reason = "任务状态损坏：${current.state}",
                )
            }
        if (state in INTERRUPTED_STATES) {
            current = current.copy(
                state = TranscriptionTaskState.RECOVERY_REQUIRED.name,
                errorCode = "PROCESS_INTERRUPTED",
                userMessage = "上次处理被系统中断，正在从原文件重新开始",
                technicalDetail = "恢复执行前任务处于 ${state.name}",
                updatedAtMillis = nowMillis(),
            )
            check(dao.update(current) == 1) { "任务恢复标记数量异常" }
            state = TranscriptionTaskState.RECOVERY_REQUIRED
        }
        if (state in setOf(
                TranscriptionTaskState.RECOVERY_REQUIRED,
                TranscriptionTaskState.NO_SPEECH,
                TranscriptionTaskState.FAILED,
                TranscriptionTaskState.WAITING_MODEL,
                TranscriptionTaskState.CANCELLED,
            )
        ) {
            if (!TaskTransitionPolicy.canTransition(state, TranscriptionTaskState.QUEUED)) {
                return@withTransaction TaskMutationResult.Rejected(
                    task = current,
                    reason = "任务当前不能重新开始：${state.name}",
                )
            }
            current = current.copy(
                state = TranscriptionTaskState.QUEUED.name,
                progressMillis = 0L,
                totalDurationMillis = null,
                errorCode = null,
                userMessage = "任务已重新排队",
                technicalDetail = null,
                updatedAtMillis = nowMillis(),
            )
            check(dao.update(current) == 1) { "任务重新排队数量异常" }
        }
        TaskMutationResult.Updated(current)
    }

    suspend fun markRecoveryRequired(
        id: String,
        errorCode: String,
        userMessage: String,
        technicalDetail: String? = null,
    ): TaskMutationResult = database.withTransaction {
        val current = dao.findById(id)
            ?: return@withTransaction TaskMutationResult.NotFound(id)
        val state = runCatching { TranscriptionTaskState.valueOf(current.state) }.getOrNull()
            ?: return@withTransaction TaskMutationResult.Rejected(
                task = current,
                reason = "任务状态损坏：${current.state}",
            )
        if (state !in INTERRUPTED_STATES) {
            return@withTransaction TaskMutationResult.Rejected(
                task = current,
                reason = "任务当前不需要恢复：${state.name}",
            )
        }
        val updated = current.copy(
            state = TranscriptionTaskState.RECOVERY_REQUIRED.name,
            errorCode = errorCode,
            userMessage = userMessage,
            technicalDetail = technicalDetail,
            updatedAtMillis = nowMillis(),
        )
        check(dao.update(updated) == 1) { "任务恢复状态更新数量异常" }
        TaskMutationResult.Updated(updated)
    }

    suspend fun recoverInterruptedTasks(): Int = database.withTransaction {
        val interrupted = dao.findByStates(INTERRUPTED_STATES.map { it.name })
        var updatedCount = 0
        interrupted.forEach { task ->
            val currentState = TranscriptionTaskState.valueOf(task.state)
            if (TaskTransitionPolicy.canTransition(
                    currentState,
                    TranscriptionTaskState.RECOVERY_REQUIRED,
                )
            ) {
                updatedCount += dao.update(
                    task.copy(
                        state = TranscriptionTaskState.RECOVERY_REQUIRED.name,
                        errorCode = "PROCESS_INTERRUPTED",
                        userMessage = "上次处理被系统中断，可重新开始",
                        technicalDetail = "进程重建时任务仍处于 ${currentState.name}",
                        updatedAtMillis = nowMillis(),
                    ),
                )
            }
        }
        updatedCount
    }

    private companion object {
        val INTERRUPTED_STATES = setOf(
            TranscriptionTaskState.PREPARING,
            TranscriptionTaskState.TRANSCRIBING,
            TranscriptionTaskState.EXPORTING,
        )
    }
}

sealed interface TaskMutationResult {
    data class Updated(val task: TranscriptionTaskEntity) : TaskMutationResult
    data class Rejected(val task: TranscriptionTaskEntity, val reason: String) : TaskMutationResult
    data class NotFound(val id: String) : TaskMutationResult
}
