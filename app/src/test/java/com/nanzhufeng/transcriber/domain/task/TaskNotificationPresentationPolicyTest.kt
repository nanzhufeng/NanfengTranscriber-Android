package com.nanzhufeng.transcriber.domain.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskNotificationPresentationPolicyTest {
    @Test
    fun onlyOngoingTasksExposeProgress() {
        assertEquals(56, TaskNotificationPresentationPolicy.progressFor(ongoing = true, percentage = 56))
        assertNull(TaskNotificationPresentationPolicy.progressFor(ongoing = false, percentage = 100))
        assertNull(TaskNotificationPresentationPolicy.progressFor(ongoing = false, percentage = null))
    }
}
