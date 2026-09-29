package dev.qtremors.osyster.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot

/** Osyster's electric cyan, used when wallpaper colors are off or unavailable (Android 8 to 11). */
val OsysterCyan = Color(0xFF00E6FF)

/**
 * Named seed colors. Each produces a full tonal scheme in light, dark, and OLED, so every preset
 * keeps proper contrast instead of shipping hand-picked colors.
 */
enum class ThemePreset(val seed: Color) {
    CYAN(OsysterCyan),
    AMBER(Color(0xFFFFB300)),
    CORAL(Color(0xFFFF5252)),
    VIOLET(Color(0xFF7C4DFF)),
    MINT(Color(0xFF26C6A0)),
    SLATE(Color(0xFF607D8B))
}

/** Builds a Material 3 scheme from one seed with Material Color Utilities' tonal spot style. */
fun seedColorScheme(seed: Color, isDark: Boolean): ColorScheme =
    SchemeTonalSpot(Hct.fromInt(seed.toArgb()), isDark, 0.0).toColorScheme(isDark)

/**
 * Pure black for OLED screens. Pages are drawn on surfaceContainer, so that is black too; rows
 * (surfaceBright) and raised parts step up in small neutral grays so they still read as separate.
 */
fun ColorScheme.toOled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceBright = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF262626),
    surfaceVariant = Color(0xFF1E1E1E)
)

private fun DynamicScheme.toColorScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(primary),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest)
    )
}
