package dev.qtremors.osyster.core.ui.format

import java.util.Locale

/** Short, glanceable numbers for usage screens: "1.4 GB", "820 KB/s", "3h 12m". */
object UsageFormat {
    private val units = listOf("B", "KB", "MB", "GB", "TB")

    fun bytes(value: Long, locale: Locale = Locale.getDefault()): String {
        if (value < 1024) return "${value.coerceAtLeast(0)} B"
        var size = value.toDouble()
        var unit = 0
        while (size >= 1024 && unit < units.lastIndex) {
            size /= 1024
            unit++
        }
        return String.format(locale, if (size >= 100) "%.0f %s" else "%.1f %s", size, units[unit])
    }

    fun rate(bytesPerSecond: Long, locale: Locale = Locale.getDefault()): String = bytes(bytesPerSecond, locale) + "/s"

    /** "3h 12m", "12m", or "<1m" for anything shorter than a minute but above zero. */
    fun duration(millis: Long): String {
        val minutes = millis / 60_000
        return when {
            millis <= 0 -> "0m"
            minutes == 0L -> "<1m"
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}
