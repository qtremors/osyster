package dev.qtremors.osyster.core.preferences.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StartScreenTest {
    @Test
    fun firstLaunchShowsWelcome() {
        assertEquals(StartScreen.WELCOME, startScreenFor(AppStartState(onboardingDone = false, lastSeenVersion = null), "0.2.6"))
    }

    @Test
    fun updateShowsWhatsNewOnce() {
        assertEquals(StartScreen.WHATS_NEW, startScreenFor(AppStartState(true, "0.2.5"), "0.2.6"))
        assertEquals(StartScreen.NONE, startScreenFor(AppStartState(true, "0.2.6"), "0.2.6"))
    }

    @Test
    fun welcomeWinsOverWhatsNew() {
        assertEquals(StartScreen.WELCOME, startScreenFor(AppStartState(false, "0.2.5"), "0.2.6"))
    }
}
