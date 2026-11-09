package dev.qtremors.osyster.core.usage.data

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.qtremors.osyster.core.usage.domain.UsageBucket
import dev.qtremors.osyster.core.usage.domain.AppDataHistory
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.TrafficStats
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.usage.domain.AppTraffic
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageReport
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.DayTraffic
import dev.qtremors.osyster.core.usage.domain.LiveRate
import dev.qtremors.osyster.core.usage.domain.NetworkFilter
import dev.qtremors.osyster.core.usage.domain.TrafficTotals
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Traffic history from NetworkStatsManager. Mobile totals cover every SIM (Android 10+ accepts a
 * null subscriber for that). Per-app numbers come from the same counters Android's own data usage
 * screen uses.
 */
@Singleton
internal class DataUsageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : DataUsageSource {
    private val stats = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    private val labels = AppLabels(context)

    override fun accessState(): UsageAccessState = context.usageAccessState()

    override suspend fun report(period: DataPeriod, includeApps: Boolean, offset: Int): DataUsageReport = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val days = DayWindows.daysOf(period, now, offset).map { start ->
            ensureActive()
            DayTraffic(start, deviceTotals(start, minOf(DayWindows.nextDay(start), now)))
        }
        val start = days.firstOrNull()?.dayStart ?: DayWindows.startOfDay(now)
        val lastDay = days.lastOrNull()?.dayStart ?: start
        val end = minOf(DayWindows.nextDay(lastDay), now)
        val totals = days.fold(TrafficTotals()) { sum, day -> sum + day.totals }
        val hourly = days.size == 1
        val buckets = if (hourly) {
            DayWindows.hoursOf(start).map { hour -> UsageBucket(hour, deviceTotals(hour, minOf(hour + HOUR_MS, now))) }
        } else {
            days.map { UsageBucket(it.dayStart, it.totals) }
        }
        DataUsageReport(
            period = period,
            offset = offset,
            firstDay = start,
            lastDay = lastDay,
            totals = totals,
            days = days,
            apps = if (includeApps) appTotals(start, end) else emptyList(),
            buckets = buckets,
            hourly = hourly
        )
    }

    override suspend fun appHistory(packageName: String, period: DataPeriod, offset: Int): AppDataHistory = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val days = DayWindows.daysOf(period, now, offset)
        val hourly = days.size == 1
        val starts = if (hourly) DayWindows.hoursOf(days.first()) else days
        val step = if (hourly) HOUR_MS else null
        val end = minOf(if (hourly) starts.last() + HOUR_MS else DayWindows.nextDay(days.last()), now)
        val uid = labels.uidOf(packageName)
        val totals = if (uid == null || stats == null) emptyList() else {
            val mobile = uidBuckets(TYPE_MOBILE, uid, starts.first(), end)
            val wifi = uidBuckets(TYPE_WIFI, uid, starts.first(), end)
            starts.mapIndexed { index, start ->
                val windowEnd = step?.let { start + it } ?: starts.getOrNull(index + 1) ?: end
                fun sum(list: List<Triple<Long, Long, Long>>) = list.filter { it.first in start until windowEnd }
                    .fold(0L to 0L) { acc, b -> (acc.first + b.second) to (acc.second + b.third) }
                val m = sum(mobile)
                val w = sum(wifi)
                UsageBucket(start, TrafficTotals(m.first, m.second, w.first, w.second))
            }
        }
        AppDataHistory(
            packageName = packageName,
            label = labels.label(packageName),
            totals = totals.fold(TrafficTotals()) { acc, b -> acc + b.totals },
            buckets = totals.ifEmpty { starts.map { UsageBucket(it, TrafficTotals()) } },
            hourly = hourly
        )
    }

    /** Start time, received, and sent bytes of each history bucket Android kept for [uid]. */
    private suspend fun uidBuckets(type: Int, uid: Int, start: Long, end: Long): List<Triple<Long, Long, Long>> {
        val details = runCatchingCancellable { stats?.queryDetailsForUid(type, null, start, end, uid) }.getOrNull() ?: return emptyList()
        val result = ArrayList<Triple<Long, Long, Long>>()
        try {
            val bucket = NetworkStats.Bucket()
            while (details.hasNextBucket()) {
                details.getNextBucket(bucket)
                result += Triple(bucket.startTimeStamp, bucket.rxBytes, bucket.txBytes)
            }
        } finally {
            details.close()
        }
        return result
    }

    override fun activeNetwork(): NetworkFilter = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager?.getNetworkCapabilities(manager.activeNetwork)
        when {
            caps == null -> NetworkFilter.ALL
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkFilter.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkFilter.MOBILE
            else -> NetworkFilter.ALL
        }
    }.getOrDefault(NetworkFilter.ALL)

    override fun liveRate(): Flow<LiveRate> = flow {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastAt = SystemClock.elapsedRealtime()
        while (true) {
            delay(LIVE_TICK_MS)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val now = SystemClock.elapsedRealtime()
            val seconds = ((now - lastAt) / 1000.0).coerceAtLeast(0.001)
            if (rx >= 0 && tx >= 0 && lastRx >= 0 && lastTx >= 0) {
                emit(LiveRate(((rx - lastRx).coerceAtLeast(0) / seconds).toLong(), ((tx - lastTx).coerceAtLeast(0) / seconds).toLong()))
            }
            lastRx = rx
            lastTx = tx
            lastAt = now
        }
    }.flowOn(dispatchers.io)

    private fun deviceTotals(start: Long, end: Long): TrafficTotals {
        if (end <= start || stats == null) return TrafficTotals()
        val mobile = bucket { stats.querySummaryForDevice(TYPE_MOBILE, null, start, end) }
        val wifi = bucket { stats.querySummaryForDevice(TYPE_WIFI, null, start, end) }
        return TrafficTotals(mobile?.rxBytes ?: 0, mobile?.txBytes ?: 0, wifi?.rxBytes ?: 0, wifi?.txBytes ?: 0)
    }

    private suspend fun appTotals(start: Long, end: Long): List<AppTraffic> {
        if (end <= start || stats == null) return emptyList()
        val mobile = perUid(TYPE_MOBILE, start, end)
        val wifi = perUid(TYPE_WIFI, start, end)
        val byApp = HashMap<String, Pair<String, TrafficTotals>>()
        (mobile.keys + wifi.keys).forEach { uid ->
            val m = mobile[uid] ?: (0L to 0L)
            val w = wifi[uid] ?: (0L to 0L)
            val app = labels.forUid(uid)
            val totals = TrafficTotals(m.first, m.second, w.first, w.second)
            // Several uids can map to one package (shared uids); merge them.
            val existing = byApp[app.packageName]
            byApp[app.packageName] = app.label to ((existing?.second ?: TrafficTotals()) + totals)
        }
        return byApp.map { (pkg, entry) -> AppTraffic(pkg, entry.first, entry.second) }
            .filter { it.totals.total(NetworkFilter.ALL) > 0 }
    }

    /** Received and sent bytes per uid for one connection type. */
    private suspend fun perUid(type: Int, start: Long, end: Long): Map<Int, Pair<Long, Long>> {
        val summary = runCatchingCancellable { stats?.querySummary(type, null, start, end) }.getOrNull() ?: return emptyMap()
        val result = HashMap<Int, Pair<Long, Long>>()
        try {
            val bucket = NetworkStats.Bucket()
            while (summary.hasNextBucket()) {
                summary.getNextBucket(bucket)
                val previous = result[bucket.uid] ?: (0L to 0L)
                result[bucket.uid] = (previous.first + bucket.rxBytes) to (previous.second + bucket.txBytes)
            }
        } finally {
            summary.close()
        }
        return result
    }

    private inline fun bucket(block: () -> NetworkStats.Bucket?): NetworkStats.Bucket? =
        try { block() } catch (_: SecurityException) { null } catch (_: RuntimeException) { null }

    private companion object {
        const val LIVE_TICK_MS = 1_000L
        const val HOUR_MS = 3_600_000L
        // NetworkStatsManager still takes the legacy ConnectivityManager network types, now
        // deprecated there; the values are fixed by the platform.
        const val TYPE_MOBILE = 0
        const val TYPE_WIFI = 1
    }
}
