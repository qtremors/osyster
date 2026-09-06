package dev.qtremors.osyster.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

internal fun ColorScheme.withAccessibleAccent(accent: Color, isDark: Boolean): ColorScheme {
    val destination = if (isDark) Color.White else Color.Black
    val surfaces = listOf(surface, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest)
    var primary = accent
    for (step in 0..100) {
        primary = lerp(accent, destination, step / 100f)
        if (surfaces.all { contrastRatio(primary, it) >= 4.5f }) break
    }
    val onPrimary = if (contrastRatio(primary, Color.Black) >= contrastRatio(primary, Color.White)) Color.Black else Color.White
    val container = lerp(surface, primary, if (isDark) 0.25f else 0.12f)
    val onContainer = if (contrastRatio(container, Color.Black) >= contrastRatio(container, Color.White)) Color.Black else Color.White
    return copy(primary = primary, onPrimary = onPrimary,
        primaryContainer = container, onPrimaryContainer = onContainer)
}
