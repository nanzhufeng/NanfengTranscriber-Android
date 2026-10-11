package com.nanzhufeng.transcriber.domain.task

import org.junit.Assert.*
import org.junit.Test

class NanfengIslandStateTest {
    private fun state(stage: IslandStage = IslandStage.TRANSCRIBING, position: Long = 35_000) =
        NanfengIslandState("task-1", "音频.wav", "本地标准", stage, "转写中", position, 100_000)
    @Test fun progressComesFromCommittedAudioAndNeverPretendsToBeFinished() {
        assertEquals("转写中 · 35%", state().compactText)
        assertEquals("00:00:35 / 00:01:40", state().timeText)
        assertEquals(99, state(position = 100_000).percentage)
        assertNull(state(IslandStage.LOADING).percentage)
        assertNull(state(IslandStage.COMPLETED).percentage)
        assertFalse(state(IslandStage.COMPLETED).ongoing)
    }
    @Test fun unknownDurationRemainsUnknown() {
        assertNull(state().copy(totalMillis = null).percentage)
        assertEquals("转写中", state().copy(totalMillis = null).compactText)
    }
    @Test fun updatesAreThrottledButNewTasksStagesAndTerminalResultsAreImmediate() {
        val gate = TaskProgressUpdateGate()
        assertTrue(gate.shouldPublish(state(), 0))
        assertFalse(gate.shouldPublish(state(position = 36_000), 100))
        assertTrue(gate.shouldPublish(state(position = 36_000), 1_000))
        assertTrue(gate.shouldPublish(state(IslandStage.SAVING), 1_001))
        assertTrue(gate.shouldPublish(state(IslandStage.COMPLETED), 1_002))
        assertTrue(gate.shouldPublish(state().copy(taskId = "task-2"), 1_003))
    }
}
