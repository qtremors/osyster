package dev.qtremors.osyster.core.preferences.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StartScreenTest {
    @Test
    fun firstLaunchShowsWelcome() {
        assertEquals(StartScreen.WELCOME, startScreenFor(AppStartState(onboardingDone = false)))
    }

    @Test
    fun finishedWelcomeShowsNothing() {
        assertEquals(StartScreen.NONE, startScreenFor(AppStartState(onboardingDone = true)))
    }
}
