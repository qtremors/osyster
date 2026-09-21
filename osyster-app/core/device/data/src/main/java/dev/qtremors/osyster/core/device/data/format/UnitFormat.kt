package dev.qtremors.osyster.core.device.data.format

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Number formatting for device facts. Locale-aware, but units stay in their usual short form. */
internal object UnitFormat {
    private val byteUnits = listOf("B", "KB", "MB", "GB", "TB")

    /** Storage and RAM in binary steps with one decimal: 7.6 GB, 512 MB. */
    fun bytes(value: Long, locale: Locale = Locale.getDefault()): String {
        if (value < 1024) return "$value B"
        var size = value.toDouble()
        var unit = 0
        while (size >= 1024 && unit < byteUnits.lastIndex) {
            size /= 1024
            unit++
        }
        val pattern = if (size >= 100) "%.0f %s" else "%.1f %s"
        return String.format(locale, pattern, size, byteUnits[unit])
    }

    fun usedOfTotal(used: Long, total: Long, locale: Locale = Locale.getDefault()): String {
        val percent = if (total > 0) (used * 100 / total).toInt() else 0
        return "${bytes(used, locale)} of ${bytes(total, locale)} ($percent%)"
    }

    /** sysfs reports kHz. Under 1 GHz reads "787 MHz", above reads "2.50 GHz". */
    fun frequencyKhz(khz: Long, locale: Locale = Locale.getDefault()): String {
        val mhz = khz / 1000.0
        return if (mhz >= 1000) String.format(locale, "%.2f GHz", mhz / 1000) else String.format(locale, "%.0f MHz", mhz)
    }

    fun celsius(value: Float, locale: Locale = Locale.getDefault()): String = String.format(locale, "%.1f °C", value)

    fun millivolts(mv: Int, locale: Locale = Locale.getDefault()): String = String.format(locale, "%.3f V", mv / 1000.0)

    fun milliamps(ma: Int): String = "$ma mA"

    fun watts(value: Double, locale: Locale = Locale.getDefault()): String = String.format(locale, "%.2f W", abs(value))

    /** "3d 4h 12m", "4h 12m", or "12m". Seconds are dropped; uptime does not need them. */
    fun duration(millis: Long): String {
        val minutes = (millis / 60_000L).coerceAtLeast(0L)
        val days = minutes / 1440
        val hours = (minutes % 1440) / 60
        val mins = minutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${mins}m"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    /** Security patch strings are "2026-08-01"; show them as a long local date. */
    fun patchDate(raw: String, locale: Locale = Locale.getDefault()): String = runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(raw)
        DateFormat.getDateInstance(DateFormat.LONG, locale).format(parsed!!)
    }.getOrDefault(raw)

    fun dateTime(epochMillis: Long, locale: Locale = Locale.getDefault()): String =
        DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, locale).format(Date(epochMillis))
}
