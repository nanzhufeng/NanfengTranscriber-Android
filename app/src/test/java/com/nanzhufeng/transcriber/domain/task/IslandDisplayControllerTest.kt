package com.nanzhufeng.transcriber.domain.task

import org.junit.Assert.*
import org.junit.Test
import com.nanzhufeng.transcriber.service.IslandScreenGeometry
import com.nanzhufeng.transcriber.service.IslandWindowGeometry

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

    @Test fun ordinaryOverlayNeverPlacesTextUnderSystemStatusIcons() {
        val screen = IslandScreenGeometry(1140, 2616, 442f / 160, 107, 533, 607, 107)
        for (expanded in listOf(false, true)) {
            val placement = IslandWindowGeometry.resolve(screen, expanded)
            assertFalse("An ordinary overlay is below SystemUI; use its safe content area", placement.atStatusBar)
            assertEquals(16, placement.y)
            assertEquals(0, placement.cameraGapEnd)
        }
    }

    @Test fun outerScreenStartsAtStatusBarAndLeavesActualCameraClear() {
        // Find N5 outer display metadata: 1140x2616, 442dpi; camera x=533..607, bottom=107.
        val screen = IslandScreenGeometry(1140, 2616, 442f / 160, 107, 533, 607, 107)
        val placement = IslandWindowGeometry.resolve(screen, expanded = false, aboveStatusBar = true)
        assertTrue(placement.atStatusBar)
        assertEquals(5, placement.y)
        assertEquals(607, placement.width)
        assertEquals(129, placement.headerHeight)
        assertEquals(245, placement.cameraGapStart)
        assertEquals(363, placement.cameraGapEnd)
    }

    @Test fun expandedOuterHeaderStillReservesTheSamePhysicalCamera() {
        val placement = IslandWindowGeometry.resolve(
            IslandScreenGeometry(1140, 2616, 442f / 160, 107, 533, 607, 107), expanded = true, aboveStatusBar = true)
        assertTrue(placement.atStatusBar)
        assertEquals(698, placement.width)
        assertEquals(290, placement.cameraGapStart)
        assertEquals(408, placement.cameraGapEnd)
    }
    @Test fun innerScreenAndLandscapeRetainOrdinarySafeAreaLayout() {
        val inner = IslandWindowGeometry.resolve(IslandScreenGeometry(2248, 2480, 442f / 160, 107), false, aboveStatusBar = true)
        assertFalse(inner.atStatusBar)
        assertNull(inner.width)
        assertEquals(16, inner.y)
        assertEquals(884, IslandWindowGeometry.resolve(IslandScreenGeometry(2248, 2480, 442f / 160, 107), true, aboveStatusBar = true).width)
        val landscape = IslandWindowGeometry.resolve(IslandScreenGeometry(2616, 1140, 442f / 160, 107), false, aboveStatusBar = true)
        assertFalse(landscape.atStatusBar)
    }
    @Test fun missingCutoutMetadataUsesAConservativeCenterClearance() {
        val placement = IslandWindowGeometry.resolve(IslandScreenGeometry(1140, 2616, 442f / 160, 107), false, aboveStatusBar = true)
        assertTrue(placement.atStatusBar)
        assertEquals(205, placement.cameraGapStart)
        assertEquals(403, placement.cameraGapEnd)
    }
    @Test fun foldingWithUnchangedProgressRetainsUserDisplayMode() {
        val display = IslandDisplayController()
        display.onTask("same")
        display.toggle()
        assertTrue(IslandWindowGeometry.resolve(IslandScreenGeometry(1140, 2616, 442f / 160, 107), display.isExpanded, aboveStatusBar = true).atStatusBar)
        display.onTask("same")
        assertTrue(display.isExpanded)
        assertFalse(IslandWindowGeometry.resolve(IslandScreenGeometry(2248, 2480, 442f / 160, 107), display.isExpanded, aboveStatusBar = true).atStatusBar)
    }
}
