package dev.qtremors.osyster.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeSettingsTest {
    @Test
    fun unknownEnumNameFallsBackToDefault() {
        assertEquals(ThemePreset.CYAN, enumOrDefault("REMOVED_PRESET", ThemePreset.CYAN))
        assertEquals(ThemeMode.SYSTEM, enumOrDefault(null, ThemeMode.SYSTEM))
    }

    @Test
    fun knownEnumNameIsRestored() {
        assertEquals(AccentSource.PRESET, enumOrDefault("PRESET", AccentSource.WALLPAPER))
    }

    @Test
    fun defaultsMatchTheDecidedFirstLaunchLook() {
        val defaults = ThemeOptions()
        assertEquals(AccentSource.WALLPAPER, defaults.accentSource)
        assertEquals(ThemePreset.CYAN, defaults.preset)
        assertEquals(Color(0xFF00E6FF), defaults.customAccent)
        assertEquals(true, defaults.hapticsEnabled)
    }
}
