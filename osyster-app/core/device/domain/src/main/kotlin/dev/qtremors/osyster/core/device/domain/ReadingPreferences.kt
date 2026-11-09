package dev.qtremors.osyster.core.device.domain

import java.util.Locale
import kotlinx.coroutines.flow.StateFlow

enum class TemperatureUnit { CELSIUS, FAHRENHEIT, KELVIN }

/** How often live numbers refresh. [factor] multiplies each page's normal interval. */
enum class RefreshSpeed(val factor: Double) {
    FAST(0.5),
    NORMAL(1.0),
    BATTERY_SAVER(2.0)
}

data class ReadingPrefs(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val refresh: RefreshSpeed = RefreshSpeed.NORMAL
)

/** How readings are shown and how often they refresh, chosen in Settings. */
interface ReadingPreferences {
    val prefs: StateFlow<ReadingPrefs>
    fun setTemperatureUnit(unit: TemperatureUnit)
    fun setRefresh(speed: RefreshSpeed)
}

/** "31.2 °C", "88.2 °F", or "304.4 K". */
fun formatTemperature(celsius: Float, unit: TemperatureUnit, locale: Locale = Locale.getDefault()): String = when (unit) {
    TemperatureUnit.CELSIUS -> String.format(locale, "%.1f °C", celsius)
    TemperatureUnit.FAHRENHEIT -> String.format(locale, "%.1f °F", celsius * 9f / 5f + 32f)
    TemperatureUnit.KELVIN -> String.format(locale, "%.1f K", celsius + 273.15f)
}

/** Scales a base interval, never below a quarter second so a page cannot spin. */
fun RefreshSpeed.scale(baseMillis: Long): Long = (baseMillis * factor).toLong().coerceAtLeast(MIN_INTERVAL_MS)

private const val MIN_INTERVAL_MS = 250L
