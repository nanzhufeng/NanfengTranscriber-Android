package com.nanzhufeng.transcriber.domain.task

import org.junit.Assert.*
import org.junit.Test

class IslandDisplayControllerTest {
    @Test fun beginsWithOneLineAndOnlyATapExpands() {
        val controller = IslandDisplayController()
        controller.onTask("one")
        assertFalse(controller.isExpanded)
        controller.toggle()
        assertTrue(controller.isExpanded)
        controller.onTask("one") // A progress update retains the user's choice.
        assertTrue(controller.isExpanded)
        controller.toggle()
        assertFalse(controller.isExpanded)
    }
    @Test fun newTaskAndReturningFromTheAppUseCompactMode() {
        val controller = IslandDisplayController()
        controller.onTask("one")
        controller.toggle()
        controller.onTask("two")
        assertFalse(controller.isExpanded)
        controller.toggle()
        controller.collapse()
        controller.onTask("two")
        assertFalse(controller.isExpanded)
    }
}
