package dev.qtremors.osyster

import dev.qtremors.osyster.settings.AccentPalette
import dev.qtremors.osyster.settings.DiagnosticsInterval
import dev.qtremors.osyster.settings.OsysterPreferencesState
import dev.qtremors.osyster.settings.TemperatureUnit
import dev.qtremors.osyster.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsysterPreferencesTest {

    @Test
    fun defaultPreferencesState_hasExpectedDefaults() {
        val state = OsysterPreferencesState()
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(AccentPalette.CYAN, state.accentPalette)
        assertFalse(state.dynamicColor)
        assertTrue(state.hapticFeedback)
        assertEquals(DiagnosticsInterval.INTERVAL_1000MS, state.diagnosticsInterval)
        assertEquals(TemperatureUnit.CELSIUS, state.temperatureUnit)
        assertFalse(state.showKernelThreads)
        assertFalse(state.isOnboardingCompleted)
    }

    @Test
    fun temperatureUnit_celsiusConversion() {
        val unit = TemperatureUnit.CELSIUS
        assertEquals(25.0f, unit.convertFromCelsius(25.0f), 0.001f)
        assertEquals("25.0 °C", unit.format(25.0f))
    }

    @Test
    fun temperatureUnit_fahrenheitConversion() {
        val unit = TemperatureUnit.FAHRENHEIT
        assertEquals(77.0f, unit.convertFromCelsius(25.0f), 0.001f)
        assertEquals("77.0 °F", unit.format(25.0f))
    }

    @Test
    fun accentPalette_hasValidEntries() {
        assertTrue(AccentPalette.entries.contains(AccentPalette.CYAN))
        assertTrue(AccentPalette.entries.contains(AccentPalette.AMBER))
        assertTrue(AccentPalette.entries.contains(AccentPalette.ROSE))
        assertTrue(AccentPalette.entries.contains(AccentPalette.EMERALD))
        assertTrue(AccentPalette.entries.contains(AccentPalette.PURPLE))
        assertTrue(AccentPalette.entries.contains(AccentPalette.BLUE))
        assertTrue(AccentPalette.entries.contains(AccentPalette.DYNAMIC))
        assertTrue(AccentPalette.entries.contains(AccentPalette.MONOCHROME))
    }

    @Test
    fun diagnosticsInterval_valuesMatchExpectedMillis() {
        assertEquals(500L, DiagnosticsInterval.INTERVAL_500MS.millis)
        assertEquals(1000L, DiagnosticsInterval.INTERVAL_1000MS.millis)
        assertEquals(2000L, DiagnosticsInterval.INTERVAL_2000MS.millis)
        assertEquals(3000L, DiagnosticsInterval.INTERVAL_3000MS.millis)
    }
}
