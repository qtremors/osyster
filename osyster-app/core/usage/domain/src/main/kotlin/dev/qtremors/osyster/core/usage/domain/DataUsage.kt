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

data class DataUsageReport(
    val period: DataPeriod,
    val totals: TrafficTotals,
    val days: List<DayTraffic>,
    val apps: List<AppTraffic>
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
    suspend fun report(period: DataPeriod, includeApps: Boolean = true): DataUsageReport
    fun liveRate(): Flow<LiveRate>
}

enum class UsageAccessState { GRANTED, MISSING }
