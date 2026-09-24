package dev.qtremors.osyster.core.system.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StayAwakeTest {
    @Test
    fun tileStartsWithTheShortestDuration() {
        assertEquals(StayAwakeStep.Start(15), nextStayAwakeStep(StayAwakeTimer.Off))
    }

    @Test
    fun tileCyclesThroughDurationsThenStops() {
        assertEquals(StayAwakeStep.Start(30), nextStayAwakeStep(StayAwakeTimer.Running(1L, 15)))
        assertEquals(StayAwakeStep.Start(null), nextStayAwakeStep(StayAwakeTimer.Running(1L, 120)))
        assertEquals(StayAwakeStep.Stop, nextStayAwakeStep(StayAwakeTimer.Running(null, null)))
    }

    @Test
    fun unknownDurationStops() {
        assertEquals(StayAwakeStep.Stop, nextStayAwakeStep(StayAwakeTimer.Running(1L, 45)))
    }

    @Test
    fun activityShortNameDropsTheOwnPackage() {
        assertEquals(".ui.SettingsActivity", ActivityEntry("com.app", "com.app.ui.SettingsActivity", "Settings", true).shortName)
        assertEquals("org.other.Main", ActivityEntry("com.app", "org.other.Main", "Main", true).shortName)
    }
}
