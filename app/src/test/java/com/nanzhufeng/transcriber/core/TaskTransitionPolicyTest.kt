package com.nanzhufeng.transcriber.core

import com.nanzhufeng.transcriber.domain.task.TaskTransitionPolicy
import com.nanzhufeng.transcriber.domain.task.TaskSelectionPolicy
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTransitionPolicyTest {
    @Test
    fun normalPipelineRequiresExportBeforeCompleted() {
        assertTrue(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.TRANSCRIBING,
                TranscriptionTaskState.EXPORTING,
            ),
        )
        assertFalse(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.TRANSCRIBING,
                TranscriptionTaskState.COMPLETED,
            ),
        )
    }

    @Test
    fun terminalCompletedTaskCannotBeReopenedSilently() {
        assertFalse(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.COMPLETED,
                TranscriptionTaskState.QUEUED,
            ),
        )
    }

    @Test
    fun interruptedTaskMustEnterExplicitRecovery() {
        assertTrue(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.TRANSCRIBING,
                TranscriptionTaskState.RECOVERY_REQUIRED,
            ),
        )
        assertTrue(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.RECOVERY_REQUIRED,
                TranscriptionTaskState.QUEUED,
            ),
        )
    }

    @Test
    fun preparingCanWaitForModelAfterSubtitleProbeMisses() {
        assertTrue(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.PREPARING,
                TranscriptionTaskState.WAITING_MODEL,
            ),
        )
    }

    @Test
    fun noSpeechIsRetryableButNeverCompleted() {
        assertTrue(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.TRANSCRIBING,
                TranscriptionTaskState.NO_SPEECH,
            ),
        )
        assertTrue(TaskSelectionPolicy.canSelectForStart(TranscriptionTaskState.NO_SPEECH))
        assertTrue(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.NO_SPEECH,
                TranscriptionTaskState.QUEUED,
            ),
        )
        assertFalse(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.NO_SPEECH,
                TranscriptionTaskState.COMPLETED,
            ),
        )
    }

    @Test
    fun cancelledAndFailedTasksCanBeSelectedAndRequeued() {
        assertTrue(TaskSelectionPolicy.canSelectForStart(TranscriptionTaskState.CANCELLED))
        assertTrue(TaskSelectionPolicy.canSelectForStart(TranscriptionTaskState.FAILED))
        assertTrue(
            TaskTransitionPolicy.canTransition(
                TranscriptionTaskState.CANCELLED,
                TranscriptionTaskState.QUEUED,
            ),
        )
        assertFalse(TaskSelectionPolicy.canSelectForStart(TranscriptionTaskState.COMPLETED))
        assertFalse(TaskSelectionPolicy.canSelectForStart(TranscriptionTaskState.TRANSCRIBING))
    }
}
