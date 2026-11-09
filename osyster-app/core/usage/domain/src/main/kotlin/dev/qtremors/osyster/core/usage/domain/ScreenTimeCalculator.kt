package dev.qtremors.osyster.core.usage.domain

import java.time.Instant
import java.time.ZoneId

/** The usage events that matter for screen time, already mapped from Android's event codes. */
enum class UsageEventKind { APP_FOREGROUND, APP_BACKGROUND, SCREEN_ON, SCREEN_OFF }

data class UsageEvent(val kind: UsageEventKind, val packageName: String?, val time: Long)

/** Per-app totals before labels are attached. */
data class ScreenTimeTotals(
    val perApp: Map<String, Long>,
    val opens: Map<String, Int>,
    val hourly: List<Long>,
    val total: Long
)

/**
 * Turns a stream of usage events into foreground time per app, per hour, and in total, for the
 * window [rangeStart, rangeEnd). Pure, so it is unit tested with hand-written event sequences.
 *
 * Rules: an app is in use from its foreground event until it goes to the background, another app
 * comes forward, or the screen turns off. Time outside the window is clipped. Very short
 * segments (under [minSegmentMs]) are transition noise and ignored; an open counts only when the
 * app stays at least [minOpenMs].
 */
class ScreenTimeCalculator(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val minSegmentMs: Long = 500,
    private val minOpenMs: Long = 2_000
) {
    fun calculate(events: Sequence<UsageEvent>, rangeStart: Long, rangeEnd: Long): ScreenTimeTotals {
        val perApp = HashMap<String, Long>()
        val opens = HashMap<String, Int>()
        val hourly = LongArray(24)
        var total = 0L
        var activePackage: String? = null
        var activeSince = 0L
        var screenOn = true

        fun commit(endTime: Long) {
            val pkg = activePackage ?: return
            val start = maxOf(activeSince, rangeStart)
            val end = minOf(endTime, rangeEnd)
            activePackage = null
            if (end - start < minSegmentMs) return
            perApp[pkg] = (perApp[pkg] ?: 0L) + (end - start)
            if (end - start >= minOpenMs) opens[pkg] = (opens[pkg] ?: 0) + 1
            total += end - start
            spreadOverHours(start, end, hourly)
        }

        for (event in events) {
            val time = minOf(event.time, rangeEnd)
            when (event.kind) {
                UsageEventKind.SCREEN_ON -> screenOn = true
                UsageEventKind.SCREEN_OFF -> {
                    commit(time)
                    screenOn = false
                }
                UsageEventKind.APP_FOREGROUND -> {
                    val pkg = event.packageName ?: continue
                    if (!screenOn) continue
                    if (pkg == activePackage) continue
                    commit(time)
                    activePackage = pkg
                    activeSince = time
                }
                UsageEventKind.APP_BACKGROUND -> {
                    if (event.packageName != null && event.packageName == activePackage) commit(time)
                }
            }
        }
        if (screenOn) commit(rangeEnd)

        return ScreenTimeTotals(perApp, opens, hourly.toList(), total.coerceIn(0L, (rangeEnd - rangeStart).coerceAtLeast(0L)))
    }

    /** Adds [start, end) to each local clock hour it touches. */
    private fun spreadOverHours(start: Long, end: Long, hourly: LongArray) {
        var cursor = start
        while (cursor < end) {
            val here = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextHour = here.plusHours(1).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
            val sliceEnd = minOf(nextHour, end)
            hourly[here.hour] += sliceEnd - cursor
            cursor = sliceEnd
        }
    }
}
