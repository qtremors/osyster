package dev.qtremors.osyster.core.usage.domain

/** One app's foreground time and opens on a given day. */
data class AppScreenTime(
    val packageName: String,
    val label: String,
    val foregroundMillis: Long,
    val opens: Int
)

/** A day of screen use: total, per hour (24 entries), and per app sorted by time. */
data class ScreenDay(
    val dayStart: Long,
    val totalMillis: Long,
    val hourlyMillis: List<Long>,
    val apps: List<AppScreenTime>
)

/** Total screen time of one day, for the history chart. */
data class ScreenDayTotal(val dayStart: Long, val totalMillis: Long)

/**
 * Screen time from Android's usage events. Needs Usage access; without it the access state says
 * so rather than reporting zero minutes.
 */
interface ScreenUsageSource {
    fun accessState(): UsageAccessState
    suspend fun day(dayStart: Long): ScreenDay
    suspend fun history(days: Int): List<ScreenDayTotal>
}
