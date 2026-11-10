package dev.qtremors.osyster.core.battery.domain

import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

/** What the battery is plugged into. */
enum class PlugType { NONE, AC, USB, WIRELESS, DOCK }

/**
 * One reading of the battery as Android reports it. [currentMa] is positive while charging and
 * negative while draining; null when the phone does not report it.
 */
data class BatterySample(
    val at: Long,
    val level: Int,
    val charging: Boolean,
    val plugged: PlugType,
    val temperatureC: Float?,
    val voltageMv: Int?,
    val currentMa: Int?
)

/** A stretch of charging, from the readings around it. [ongoing] while it is still plugged in. */
data class ChargeSession(
    val start: Long,
    val end: Long,
    val fromLevel: Int,
    val toLevel: Int,
    val plugged: PlugType,
    val averageCurrentMa: Int?,
    val maxTemperatureC: Float?,
    val ongoing: Boolean
)

/** The battery's lasting facts, where the phone reports them (cycle count from Android 14). */
data class BatteryFacts(val cycleCount: Int?, val health: BatteryHealth)

enum class BatteryHealth { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, FAILURE, UNKNOWN }

/** On by default: a reading every half hour costs next to nothing. */
data class BatteryHistorySettings(val enabled: Boolean = true)

interface BatteryHistorySettingsStore {
    val settings: Flow<BatteryHistorySettings>
    suspend fun update(transform: (BatteryHistorySettings) -> BatteryHistorySettings)
}

interface BatteryHistory {
    /** Readings since [since], oldest first. */
    fun samples(since: Long): Flow<List<BatterySample>>

    /** Takes a reading now, as the scheduled job does, and saves it. */
    suspend fun recordNow(): BatterySample?

    fun facts(): BatteryFacts

    suspend fun clear()
}

/** Keeps the periodic reading scheduled while history is on, and cancels it when it is off. */
interface BatterySampling {
    fun schedule(enabled: Boolean)
}

/**
 * Charge sessions in [samples] (oldest first). A session starts at the reading before the plug
 * went in, when there is one, so its starting level is the one the phone had, and ends at the
 * first reading after it came out.
 */
fun chargeSessions(samples: List<BatterySample>): List<ChargeSession> {
    val sessions = mutableListOf<ChargeSession>()
    var i = 0
    while (i < samples.size) {
        if (!samples[i].charging) {
            i++
            continue
        }
        val first = i
        while (i < samples.size && samples[i].charging) i++
        val charging = samples.subList(first, i)
        val before = samples.getOrNull(first - 1)
        val after = samples.getOrNull(i)
        val currents = charging.mapNotNull { it.currentMa }.filter { it > 0 }
        sessions += ChargeSession(
            start = before?.at ?: charging.first().at,
            end = after?.at ?: charging.last().at,
            fromLevel = before?.level ?: charging.first().level,
            toLevel = (after ?: charging.last()).level,
            plugged = charging.firstOrNull { it.plugged != PlugType.NONE }?.plugged ?: PlugType.NONE,
            averageCurrentMa = currents.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            maxTemperatureC = charging.mapNotNull { it.temperatureC }.maxOrNull(),
            ongoing = after == null
        )
    }
    return sessions
}

/**
 * Average drain in percent an hour, over the stretches between two readings where the phone was
 * on battery. Null until there is at least half an hour of such readings.
 */
fun drainPerHour(samples: List<BatterySample>): Double? {
    var dropped = 0
    var millis = 0L
    for ((a, b) in samples.zipWithNext()) {
        if (a.charging || b.charging || b.level > a.level) continue
        dropped += a.level - b.level
        millis += b.at - a.at
    }
    if (millis < MIN_DRAIN_SPAN_MS) return null
    return dropped / (millis / HOUR_MS.toDouble())
}

/**
 * Android reports the battery current in microamps on most phones, milliamps on some, and with
 * either sign. Returns milliamps, positive while charging.
 */
fun normalizeCurrent(raw: Int?, charging: Boolean): Int? {
    if (raw == null || raw == 0 || raw == Int.MIN_VALUE) return null
    val ma = if (abs(raw) > MICROAMP_THRESHOLD) abs(raw) / 1000 else abs(raw)
    return if (charging) ma else -ma
}

/**
 * Sent to Osyster itself (never to other apps) after a reading is saved, so the battery widget
 * redraws without a schedule of its own.
 */
const val ACTION_BATTERY_HISTORY_UPDATED = "dev.qtremors.osyster.BATTERY_HISTORY_UPDATED"

const val HOUR_MS = 3_600_000L
const val SAMPLE_EVERY_MS = 30 * 60_000L

/** Readings are kept this long. */
const val KEEP_MS = 30L * 24 * HOUR_MS

private const val MIN_DRAIN_SPAN_MS = 30 * 60_000L
private const val MICROAMP_THRESHOLD = 20_000
