package com.nanzhufeng.transcriber.domain.task

enum class TranscriptionTaskState {
    QUEUED,
    WAITING_INPUT,
    WAITING_MODEL,
    PREPARING,
    TRANSCRIBING,
    EXPORTING,
    RECOVERY_REQUIRED,
    NO_SPEECH,
    COMPLETED,
    FAILED,
    CANCELLED,
}

object TaskTransitionPolicy {
    private val allowed = mapOf(
        TranscriptionTaskState.QUEUED to setOf(
            TranscriptionTaskState.WAITING_INPUT,
            TranscriptionTaskState.WAITING_MODEL,
            TranscriptionTaskState.PREPARING,
            TranscriptionTaskState.CANCELLED,
            TranscriptionTaskState.FAILED,
        ),
        TranscriptionTaskState.WAITING_INPUT to setOf(
            TranscriptionTaskState.QUEUED,
            TranscriptionTaskState.CANCELLED,
            TranscriptionTaskState.FAILED,
        ),
        TranscriptionTaskState.WAITING_MODEL to setOf(
            TranscriptionTaskState.QUEUED,
            TranscriptionTaskState.CANCELLED,
            TranscriptionTaskState.FAILED,
        ),
        TranscriptionTaskState.PREPARING to setOf(
            TranscriptionTaskState.WAITING_MODEL,
            TranscriptionTaskState.TRANSCRIBING,
            TranscriptionTaskState.RECOVERY_REQUIRED,
            TranscriptionTaskState.CANCELLED,
            TranscriptionTaskState.FAILED,
        ),
        TranscriptionTaskState.TRANSCRIBING to setOf(
            TranscriptionTaskState.EXPORTING,
            TranscriptionTaskState.NO_SPEECH,
            TranscriptionTaskState.RECOVERY_REQUIRED,
            TranscriptionTaskState.CANCELLED,
            TranscriptionTaskState.FAILED,
        ),
        TranscriptionTaskState.EXPORTING to setOf(
            TranscriptionTaskState.COMPLETED,
            TranscriptionTaskState.RECOVERY_REQUIRED,
            TranscriptionTaskState.CANCELLED,
            TranscriptionTaskState.FAILED,
        ),
        TranscriptionTaskState.RECOVERY_REQUIRED to setOf(
            TranscriptionTaskState.QUEUED,
            TranscriptionTaskState.WAITING_INPUT,
            TranscriptionTaskState.WAITING_MODEL,
            TranscriptionTaskState.CANCELLED,
            TranscriptionTaskState.FAILED,
        ),
        TranscriptionTaskState.NO_SPEECH to setOf(
            TranscriptionTaskState.QUEUED,
            TranscriptionTaskState.CANCELLED,
        ),
        TranscriptionTaskState.FAILED to setOf(
            TranscriptionTaskState.QUEUED,
            TranscriptionTaskState.CANCELLED,
        ),
        TranscriptionTaskState.CANCELLED to setOf(
            TranscriptionTaskState.QUEUED,
        ),
    )

    fun canTransition(from: TranscriptionTaskState, to: TranscriptionTaskState): Boolean =
        to in allowed[from].orEmpty()
}

object TaskSelectionPolicy {
    fun canSelectForStart(state: TranscriptionTaskState): Boolean = state in setOf(
        TranscriptionTaskState.WAITING_MODEL,
        TranscriptionTaskState.RECOVERY_REQUIRED,
        TranscriptionTaskState.NO_SPEECH,
        TranscriptionTaskState.FAILED,
        TranscriptionTaskState.CANCELLED,
    )
}
