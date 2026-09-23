package dev.qtremors.osyster.core.usage.data

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

    override suspend fun report(period: DataPeriod, includeApps: Boolean): DataUsageReport = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val days = DayWindows.daysOf(period, now).map { start ->
            ensureActive()
            DayTraffic(start, deviceTotals(start, minOf(DayWindows.nextDay(start), now)))
        }
        val start = days.firstOrNull()?.dayStart ?: DayWindows.startOfDay(now)
        val totals = days.fold(TrafficTotals()) { sum, day -> sum + day.totals }
        DataUsageReport(
            period = period,
            totals = totals,
            days = days,
            apps = if (includeApps) appTotals(start, now) else emptyList()
        )
    }

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
        // NetworkStatsManager still takes the legacy ConnectivityManager network types, now
        // deprecated there; the values are fixed by the platform.
        const val TYPE_MOBILE = 0
        const val TYPE_WIFI = 1
    }
}
