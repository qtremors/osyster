package dev.qtremors.osyster.core.usage.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import dev.qtremors.osyster.core.usage.domain.AppScreenTime
import dev.qtremors.osyster.core.usage.domain.ScreenDay
import dev.qtremors.osyster.core.usage.domain.ScreenDayTotal
import dev.qtremors.osyster.core.usage.domain.ScreenTimeCalculator
import dev.qtremors.osyster.core.usage.domain.ScreenUsageSource
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import dev.qtremors.osyster.core.usage.domain.UsageEvent
import dev.qtremors.osyster.core.usage.domain.UsageEventKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Screen time from UsageStatsManager events. Android keeps detailed events for about a week, so
 * history stops there; older days would only be guesses from coarser stats.
 */
@Singleton
internal class ScreenUsageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : ScreenUsageSource {
    private val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val labels = AppLabels(context)
    private val calculator = ScreenTimeCalculator()

    override fun accessState(): UsageAccessState = context.usageAccessState()

    override suspend fun day(dayStart: Long): ScreenDay = withContext(dispatchers.io) {
        val end = minOf(DayWindows.nextDay(dayStart), System.currentTimeMillis())
        val totals = calculator.calculate(events(dayStart, end), dayStart, end)
        val apps = totals.perApp
            .map { (pkg, millis) -> AppScreenTime(pkg, labels.label(pkg), millis, totals.opens[pkg] ?: 0) }
            .sortedByDescending(AppScreenTime::foregroundMillis)
        ScreenDay(dayStart, totals.total, totals.hourly, apps)
    }

    override suspend fun history(days: Int): List<ScreenDayTotal> = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        DayWindows.lastDays(days, now).map { start ->
            ensureActive()
            val end = minOf(DayWindows.nextDay(start), now)
            ScreenDayTotal(start, calculator.calculate(events(start, end), start, end).total)
        }
    }

    /**
     * Events from a little before [start], so an app already open at midnight is credited from
     * midnight rather than lost.
     */
    private fun events(start: Long, end: Long): Sequence<UsageEvent> {
        val manager = usage ?: return emptySequence()
        val result = runCatching { manager.queryEvents((start - LOOKBACK_MS).coerceAtLeast(0), end) }.getOrNull()
            ?: return emptySequence()
        return sequence {
            val event = UsageEvents.Event()
            while (result.hasNextEvent()) {
                result.getNextEvent(event)
                val kind = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventKind.APP_FOREGROUND
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> UsageEventKind.APP_BACKGROUND
                    UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventKind.SCREEN_ON
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventKind.SCREEN_OFF
                    else -> null
                } ?: continue
                yield(UsageEvent(kind, event.packageName, event.timeStamp))
            }
        }
    }

    private companion object {
        const val LOOKBACK_MS = 3 * 60 * 60 * 1000L
    }
}
