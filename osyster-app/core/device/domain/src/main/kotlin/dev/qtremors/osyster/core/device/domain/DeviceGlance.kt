package dev.qtremors.osyster.core.device.domain

import kotlinx.coroutines.flow.Flow

/**
 * One short live fact per device page for Home's rows, such as "82% · Charging" for Battery.
 * Pages whose facts are expensive to gather (codecs, certificates) are left out and keep their
 * fixed summary. Values are already localized.
 */
interface DeviceGlanceSource {
    /** Emits now, then refreshes every few seconds while collected. */
    fun glances(): Flow<Map<DevicePage, String>>
}

/** Keeps the most recent [capacity] readings, oldest first, for a sparkline. */
fun List<Float>.pushTrend(value: Float?, capacity: Int = TREND_POINTS): List<Float> =
    if (value == null || value.isNaN()) this else (this + value.coerceIn(0f, 1f)).takeLast(capacity)

const val TREND_POINTS = 30
