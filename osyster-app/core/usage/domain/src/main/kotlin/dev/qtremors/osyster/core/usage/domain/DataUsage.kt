package dev.qtremors.osyster.core.usage.domain

import kotlinx.coroutines.flow.Flow

/** The window a data usage report covers. Week means the last seven days including today. */
enum class DataPeriod { TODAY, WEEK, MONTH }

/** Which connection type a report counts. */
enum class NetworkFilter { ALL, MOBILE, WIFI }

/** Bytes received (rx) and sent (tx) on mobile and Wi-Fi. */
data class TrafficTotals(
    val mobileRx: Long = 0,
    val mobileTx: Long = 0,
    val wifiRx: Long = 0,
    val wifiTx: Long = 0
) {
    val mobile: Long get() = mobileRx + mobileTx
    val wifi: Long get() = wifiRx + wifiTx

    fun received(filter: NetworkFilter): Long = when (filter) {
        NetworkFilter.ALL -> mobileRx + wifiRx
        NetworkFilter.MOBILE -> mobileRx
        NetworkFilter.WIFI -> wifiRx
    }

    fun sent(filter: NetworkFilter): Long = when (filter) {
        NetworkFilter.ALL -> mobileTx + wifiTx
        NetworkFilter.MOBILE -> mobileTx
        NetworkFilter.WIFI -> wifiTx
    }

    fun total(filter: NetworkFilter): Long = received(filter) + sent(filter)

    operator fun plus(other: TrafficTotals) = TrafficTotals(
        mobileRx + other.mobileRx, mobileTx + other.mobileTx, wifiRx + other.wifiRx, wifiTx + other.wifiTx
    )
}

/** One app's traffic in the period. System traffic without an app is grouped under its own key. */
data class AppTraffic(
    val packageName: String,
    val label: String,
    val totals: TrafficTotals
)

/** One calendar day of traffic, for the daily chart. [dayStart] is local midnight in epoch millis. */
data class DayTraffic(val dayStart: Long, val totals: TrafficTotals)

/** One bar of a usage chart: an hour for single-day reports, otherwise a day. */
data class UsageBucket(val start: Long, val totals: TrafficTotals)

/** One app's traffic over a period, with the same buckets as the device report. */
data class AppDataHistory(
    val packageName: String,
    val label: String,
    val totals: TrafficTotals,
    val buckets: List<UsageBucket>,
    val hourly: Boolean
)

data class DataUsageReport(
    val period: DataPeriod,
    /** 0 is the current period, 1 the one before it, and so on. */
    val offset: Int = 0,
    /** First and last local day covered, as epoch millis at midnight. */
    val firstDay: Long = 0,
    val lastDay: Long = 0,
    val totals: TrafficTotals,
    val days: List<DayTraffic>,
    val apps: List<AppTraffic>,
    /** Hourly for a single day, daily otherwise; what the bar chart draws. */
    val buckets: List<UsageBucket> = days.map { UsageBucket(it.dayStart, it.totals) },
    val hourly: Boolean = false
) {
    fun appsFor(filter: NetworkFilter): List<AppTraffic> =
        apps.filter { it.totals.total(filter) > 0 }.sortedByDescending { it.totals.total(filter) }
}

/** Live device-wide rates in bytes per second. */
data class LiveRate(val downBytesPerSecond: Long, val upBytesPerSecond: Long)

/**
 * Historical traffic from Android's network statistics. Needs Usage access; without it every
 * call reports [UsageAccessState.MISSING] instead of zeros.
 */
interface DataUsageSource {
    fun accessState(): UsageAccessState
    /** [offset] steps back whole periods: yesterday, the seven days before, last month. */
    suspend fun report(period: DataPeriod, includeApps: Boolean = true, offset: Int = 0): DataUsageReport

    /** One app's traffic over the same windows as [report]. Empty when the app has no data. */
    suspend fun appHistory(packageName: String, period: DataPeriod, offset: Int = 0): AppDataHistory
    fun liveRate(): Flow<LiveRate>

    /** MOBILE or WIFI for the network in use now, ALL when offline or on something else. */
    fun activeNetwork(): NetworkFilter
}

enum class UsageAccessState { GRANTED, MISSING }

/** Android keeps roughly a year of network history; going further back only shows zeros. */
fun maxOffset(period: DataPeriod): Int = when (period) {
    DataPeriod.TODAY -> 89
    DataPeriod.WEEK -> 12
    DataPeriod.MONTH -> 11
}

/** The order the period and network buttons appear in: the everyday choice in the middle. */
val PeriodButtons: List<DataPeriod> = listOf(DataPeriod.WEEK, DataPeriod.TODAY, DataPeriod.MONTH)
val NetworkButtons: List<NetworkFilter> = listOf(NetworkFilter.MOBILE, NetworkFilter.ALL, NetworkFilter.WIFI)

/**
 * The offset whose window contains [picked]: days back for Today, whole weeks back for 7 days
 * (counted from today), calendar months back for This month. Future dates count as now; very
 * old dates stop at [maxOffset].
 */
fun offsetFor(period: DataPeriod, today: java.time.LocalDate, picked: java.time.LocalDate): Int {
    if (!picked.isBefore(today)) return 0
    val days = java.time.temporal.ChronoUnit.DAYS.between(picked, today).toInt()
    val offset = when (period) {
        DataPeriod.TODAY -> days
        DataPeriod.WEEK -> days / 7
        DataPeriod.MONTH -> java.time.temporal.ChronoUnit.MONTHS.between(picked.withDayOfMonth(1), today.withDayOfMonth(1)).toInt()
    }
    return offset.coerceIn(0, maxOffset(period))
}

/** The oldest day the date picker offers for [period]. */
fun earliestDay(period: DataPeriod, today: java.time.LocalDate): java.time.LocalDate = when (period) {
    DataPeriod.TODAY -> today.minusDays(maxOffset(period).toLong())
    DataPeriod.WEEK -> today.minusDays(7L * maxOffset(period) + 6)
    DataPeriod.MONTH -> today.withDayOfMonth(1).minusMonths(maxOffset(period).toLong())
}
