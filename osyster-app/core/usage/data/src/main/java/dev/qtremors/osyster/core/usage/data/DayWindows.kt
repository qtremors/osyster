package dev.qtremors.osyster.core.usage.data

import dev.qtremors.osyster.core.usage.domain.DataPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Local-calendar day boundaries in epoch millis. */
internal object DayWindows {
    fun startOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun nextDay(dayStart: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(dayStart).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * Day starts covered by [period], oldest first. With [offset] 0 the window ends today; each
     * step back moves one whole period: the day before, the seven days before, the month before.
     */
    fun daysOf(period: DataPeriod, now: Long, offset: Int = 0, zone: ZoneId = ZoneId.systemDefault()): List<Long> {
        val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val steps = offset.toLong().coerceAtLeast(0)
        val (first, last) = when (period) {
            DataPeriod.TODAY -> today.minusDays(steps).let { it to it }
            DataPeriod.WEEK -> today.minusDays(7 * steps).let { end -> end.minusDays(6) to end }
            DataPeriod.MONTH -> today.minusMonths(steps).withDayOfMonth(1).let { start ->
                start to if (steps == 0L) today else start.plusMonths(1).minusDays(1)
            }
        }
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(last) }
            .map { it.atStartOfDay(zone).toInstant().toEpochMilli() }
            .toList()
    }

    /** The last [count] day starts, oldest first, ending with today. */
    fun lastDays(count: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Long> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (count - 1 downTo 0).map { today.minusDays(it.toLong()).atStartOfDay(zone).toInstant().toEpochMilli() }
    }
}
