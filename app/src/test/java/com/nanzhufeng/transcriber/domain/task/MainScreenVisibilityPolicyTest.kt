package com.nanzhufeng.transcriber.domain.task

import org.junit.Assert.*
import org.junit.Test

class MainScreenVisibilityPolicyTest {
    @Test fun roomBindingDoesNotPretendTheMainScreenIsVisible() {
        assertFalse(MainScreenVisibilityPolicy.isVisible(125))
        assertFalse(MainScreenVisibilityPolicy.isVisible(230))
        assertFalse(MainScreenVisibilityPolicy.isVisible(400))
    }
    @Test fun foregroundAndMultiWindowActivitiesAreVisible() {
        assertTrue(MainScreenVisibilityPolicy.isVisible(100))
        assertTrue(MainScreenVisibilityPolicy.isVisible(200))
    }
}
