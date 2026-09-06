package dev.qtremors.osyster.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.qtremors.osyster.settings.AccentPalette
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentContrastTest {
    @Test
    fun everyAccentKeepsReadableTextAndControlsAcrossThemes() {
        val schemes = listOf(
            LightColorScheme to false,
            DarkColorScheme to true,
            OledColorScheme to true
        )
        for ((base, dark) in schemes) {
            for (accent in AccentPalette.entries.mapNotNull { it.primaryColor }) {
                val scheme = base.withAccessibleAccent(accent, dark)
                assertTrue(contrastRatio(scheme.primary, scheme.onPrimary) >= 4.5f)
                assertTrue(contrastRatio(scheme.primaryContainer, scheme.onPrimaryContainer) >= 4.5f)
                for (surface in listOf(scheme.surface, scheme.surfaceContainer, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest)) {
                    assertTrue(contrastRatio(scheme.primary, surface) >= 4.5f)
                }
            }
        }
    }
}
