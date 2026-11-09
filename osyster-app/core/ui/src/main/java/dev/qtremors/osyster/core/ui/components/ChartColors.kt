package dev.qtremors.osyster.core.ui.components

import dev.qtremors.osyster.core.device.domain.LevelStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Fixed colors for chart series that must be told apart at a glance, whatever the accent. Theme
 * accents can land download and upload on two shades of one hue, so these use distant hues:
 * blue and amber for download and upload, violet and teal for mobile and Wi-Fi. Each has a
 * deeper tone for light themes and a brighter one for dark themes.
 */
@Immutable
data class ChartColors(
    val download: Color,
    val upload: Color,
    val mobile: Color,
    val wifi: Color
) {
    companion object {
        private val Light = ChartColors(
            download = Color(0xFF1565C0),
            upload = Color(0xFFE08600),
            mobile = Color(0xFF7B3FD0),
            wifi = Color(0xFF00897B)
        )
        private val Dark = ChartColors(
            download = Color(0xFF64B5F6),
            upload = Color(0xFFFFB74D),
            mobile = Color(0xFFB39DDB),
            wifi = Color(0xFF4DD0C4)
        )

        @Composable
        fun current(): ChartColors = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Dark else Light
    }
}

/** Green, amber, and red for a reading's status, deeper in light themes and brighter in dark ones. */
@Immutable
data class StatusColors(val good: Color, val warning: Color, val critical: Color) {
    fun of(status: LevelStatus): Color = when (status) {
        LevelStatus.GOOD -> good
        LevelStatus.WARNING -> warning
        LevelStatus.CRITICAL -> critical
    }

    companion object {
        private val Light = StatusColors(good = Color(0xFF2E7D32), warning = Color(0xFFE08600), critical = Color(0xFFC62828))
        private val Dark = StatusColors(good = Color(0xFF81C784), warning = Color(0xFFFFCA28), critical = Color(0xFFEF6C6C))

        @Composable
        fun current(): StatusColors = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Dark else Light
    }
}
