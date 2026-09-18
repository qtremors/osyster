package dev.qtremors.osyster.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.util.SparseArray
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

object ScreenUsageHelper {

    data class UsageResult(
        val appUsageMap: Map<String, Long> = emptyMap(),
        val hourlyUsageMap: Map<Int, Map<String, Long>> = emptyMap(),
        val sessionCounts: Map<String, Int> = emptyMap(),
        val totalGlobalUsage: Long = 0L
    )

    data class DailyUsageSummary(
        val dateMillis: Long,
        val totalTimeMillis: Long,
        val isToday: Boolean = false,
        val appUsageMap: Map<String, Long> = emptyMap()
    )

    private const val MIDNIGHT_LOOKBACK_MS = 600000L
    private const val SESSION_MIN_DURATION = 4000L
    private const val MIN_SEGMENT_DURATION = 100L

    @Volatile
    private var lastResult: UsageResult? = null
    @Volatile
    private var lastQueryTime = 0L
    private const val CACHE_DURATION_MS = 30000L
    private val refreshLock = Any()

    fun getDayStartTime(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getDayEndTime(dayStart: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun fetchDetailedUsageToday(
        usageStatsManager: UsageStatsManager,
        includeHourly: Boolean = true,
        forceRefresh: Boolean = false
    ): UsageResult {
        val currentTime = System.currentTimeMillis()
        val todayStart = getDayStartTime(currentTime)

        val cached = lastResult
        if (!forceRefresh && cached != null && currentTime - lastQueryTime < CACHE_DURATION_MS) {
            return cached
        }

        synchronized(refreshLock) {
            val reCached = lastResult
            if (!forceRefresh && reCached != null && currentTime - lastQueryTime < CACHE_DURATION_MS) {
                return reCached
            }

            val result = queryUsageEventsForRange(
                usageStatsManager = usageStatsManager,
                startTime = todayStart,
                endTime = currentTime,
                includeHourly = includeHourly
            )

            lastResult = result
            lastQueryTime = currentTime
            return result
        }
    }

    fun fetchUsageForRange(
        usageStatsManager: UsageStatsManager,
        startTime: Long,
        endTime: Long,
        includeHourly: Boolean = true
    ): UsageResult {
        return queryUsageEventsForRange(
            usageStatsManager = usageStatsManager,
            startTime = startTime,
            endTime = endTime,
            includeHourly = includeHourly
        )
    }

    fun fetchDailySummaries(
        usageStatsManager: UsageStatsManager,
        daysCount: Int = 7,
        preferSystemHistory: Boolean = true
    ): List<DailyUsageSummary> {
        val now = System.currentTimeMillis()
        val todayStart = getDayStartTime(now)
        val summaries = mutableListOf<DailyUsageSummary>()

        for (i in (daysCount - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dayStart = cal.timeInMillis
            val isToday = (i == 0)
            val dayEnd = if (isToday) now else getDayEndTime(dayStart)

            val usageResult = if (isToday) {
                fetchDetailedUsageToday(usageStatsManager, includeHourly = false)
            } else if (preferSystemHistory) {
                fetchUsageForRange(usageStatsManager, dayStart, dayEnd, includeHourly = false)
            } else {
                UsageResult()
            }

            summaries.add(
                DailyUsageSummary(
                    dateMillis = dayStart,
                    totalTimeMillis = usageResult.totalGlobalUsage,
                    isToday = isToday,
                    appUsageMap = usageResult.appUsageMap
                )
            )
        }

        return summaries
    }

    private fun queryUsageEventsForRange(
        usageStatsManager: UsageStatsManager,
        startTime: Long,
        endTime: Long,
        includeHourly: Boolean
    ): UsageResult {
        if (startTime >= endTime) return UsageResult()

        val zoneId = ZoneId.systemDefault()
        val startQuery = startTime - MIDNIGHT_LOOKBACK_MS
        val events = runCatching {
            usageStatsManager.queryEvents(startQuery, endTime)
        }.getOrNull() ?: return UsageResult()

        val accumulatedUsageMap = mutableMapOf<String, Long>()
        val accumulatedHourlyMap = SparseArray<HashMap<String, Long>>()
        val accumulatedSessionCounts = mutableMapOf<String, Int>()
        var lastTotalGlobalUsage = 0L

        var currentActivePkg: String? = null
        var currentActiveStartTime = 0L
        var currentIsScreenOn = true

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val time = event.timeStamp.coerceAtMost(endTime)
            if (time < startQuery) continue

            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    currentIsScreenOn = true
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    currentIsScreenOn = false
                    currentActivePkg?.let { p ->
                        val duration = commitSegment(
                            pkg = p,
                            startTime = currentActiveStartTime,
                            endTime = time,
                            rangeStart = startTime,
                            rangeEnd = endTime,
                            zoneId = zoneId,
                            includeHourly = includeHourly,
                            accumulatedUsageMap = accumulatedUsageMap,
                            accumulatedHourlyMap = accumulatedHourlyMap,
                            accumulatedSessionCounts = accumulatedSessionCounts
                        )
                        lastTotalGlobalUsage += duration
                    }
                    currentActivePkg = null
                    currentActiveStartTime = 0L
                }
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (currentIsScreenOn) {
                        val className = event.className
                        if (className != null && (
                                className.contains("Notification", ignoreCase = true) ||
                                className.contains("Toast", ignoreCase = true)
                            )) {
                            continue
                        }

                        if (currentActivePkg != null) {
                            val duration = commitSegment(
                                pkg = currentActivePkg,
                                startTime = currentActiveStartTime,
                                endTime = time,
                                rangeStart = startTime,
                                rangeEnd = endTime,
                                zoneId = zoneId,
                                includeHourly = includeHourly,
                                accumulatedUsageMap = accumulatedUsageMap,
                                accumulatedHourlyMap = accumulatedHourlyMap,
                                accumulatedSessionCounts = accumulatedSessionCounts
                            )
                            lastTotalGlobalUsage += duration
                        }
                        currentActivePkg = pkg
                        currentActiveStartTime = time
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (currentActivePkg == pkg) {
                        val duration = commitSegment(
                            pkg = pkg,
                            startTime = currentActiveStartTime,
                            endTime = time,
                            rangeStart = startTime,
                            rangeEnd = endTime,
                            zoneId = zoneId,
                            includeHourly = includeHourly,
                            accumulatedUsageMap = accumulatedUsageMap,
                            accumulatedHourlyMap = accumulatedHourlyMap,
                            accumulatedSessionCounts = accumulatedSessionCounts
                        )
                        lastTotalGlobalUsage += duration
                        currentActivePkg = null
                        currentActiveStartTime = 0L
                    }
                }
            }
        }

        // Handle open session extending to endTime
        if (currentIsScreenOn && currentActivePkg != null) {
            val duration = commitSegment(
                pkg = currentActivePkg,
                startTime = currentActiveStartTime,
                endTime = endTime,
                rangeStart = startTime,
                rangeEnd = endTime,
                zoneId = zoneId,
                includeHourly = includeHourly,
                accumulatedUsageMap = accumulatedUsageMap,
                accumulatedHourlyMap = accumulatedHourlyMap,
                accumulatedSessionCounts = accumulatedSessionCounts
            )
            lastTotalGlobalUsage += duration
        }

        val resultHourlyMap = mutableMapOf<Int, Map<String, Long>>()
        if (includeHourly) {
            for (i in 0 until 24) {
                val pkgMap = accumulatedHourlyMap.get(i) ?: HashMap()
                resultHourlyMap[i] = pkgMap
            }
        }

        return UsageResult(
            appUsageMap = accumulatedUsageMap,
            hourlyUsageMap = resultHourlyMap,
            sessionCounts = accumulatedSessionCounts,
            totalGlobalUsage = lastTotalGlobalUsage
        )
    }

    private fun commitSegment(
        pkg: String,
        startTime: Long,
        endTime: Long,
        rangeStart: Long,
        rangeEnd: Long,
        zoneId: ZoneId,
        includeHourly: Boolean,
        accumulatedUsageMap: MutableMap<String, Long>,
        accumulatedHourlyMap: SparseArray<HashMap<String, Long>>,
        accumulatedSessionCounts: MutableMap<String, Int>
    ): Long {
        val segmentStart = maxOf(startTime, rangeStart)
        val segmentEnd = minOf(endTime, rangeEnd)
        if (segmentStart >= segmentEnd) return 0L

        val duration = segmentEnd - segmentStart
        if (duration < MIN_SEGMENT_DURATION) return 0L

        accumulatedUsageMap[pkg] = (accumulatedUsageMap[pkg] ?: 0L) + duration
        if (duration >= SESSION_MIN_DURATION) {
            accumulatedSessionCounts[pkg] = (accumulatedSessionCounts[pkg] ?: 0) + 1
        }

        if (includeHourly) {
            var currentMillis = segmentStart
            var currentZdt = Instant.ofEpochMilli(currentMillis).atZone(zoneId)
            var nextHourZdt = currentZdt.plusHours(1).withMinute(0).withSecond(0).withNano(0)
            var nextHourMillis = nextHourZdt.toInstant().toEpochMilli()

            while (currentMillis < segmentEnd) {
                val hour = currentZdt.hour
                val segEnd = minOf(nextHourMillis, segmentEnd)
                val hDuration = segEnd - currentMillis
                if (hDuration > 0) {
                    var pkgMap = accumulatedHourlyMap.get(hour)
                    if (pkgMap == null) {
                        pkgMap = HashMap()
                        accumulatedHourlyMap.put(hour, pkgMap)
                    }
                    pkgMap[pkg] = (pkgMap[pkg] ?: 0L) + hDuration
                }
                currentMillis = segEnd
                if (currentMillis < segmentEnd) {
                    currentZdt = nextHourZdt
                    nextHourZdt = currentZdt.plusHours(1)
                    nextHourMillis = nextHourZdt.toInstant().toEpochMilli()
                }
            }
        }

        return duration
    }

    fun formatDuration(millis: Long): String {
        if (millis <= 0L) return "0m"
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return when {
            hours > 0 -> {
                if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            }
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    fun clearCache() {
        synchronized(refreshLock) {
            lastResult = null
            lastQueryTime = 0L
        }
    }
}
