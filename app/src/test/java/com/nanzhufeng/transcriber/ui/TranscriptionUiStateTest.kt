package com.nanzhufeng.transcriber.ui

import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionUiStateTest {
    @Test
    fun selectedSourceCanStartBeforeModelToProbeEmbeddedSubtitles() {
        val state = TranscriptionUiState(
            nativeStatus = "可用",
            modelDisplayName = "均衡",
            modelExpectedBytes = 1L,
            modelState = ModelInstallState.NOT_INSTALLED,
            selectedSourceName = "带字幕视频.mp4",
            stage = WorkflowStage.IDLE,
        )

        assertTrue(state.canStart)
    }

    @Test
    fun busyWorkflowStillPreventsStartingAnotherSource() {
        val state = TranscriptionUiState(
            nativeStatus = "可用",
            modelDisplayName = "均衡",
            modelExpectedBytes = 1L,
            modelState = ModelInstallState.NOT_INSTALLED,
            selectedSourceName = "带字幕视频.mp4",
            stage = WorkflowStage.TRANSCRIBING,
        )

        assertFalse(state.canStart)
    }
}
