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

    /** Day starts covered by [period], oldest first, ending with today. */
    fun daysOf(period: DataPeriod, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Long> {
        val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val first = when (period) {
            DataPeriod.TODAY -> today
            DataPeriod.WEEK -> today.minusDays(6)
            DataPeriod.MONTH -> today.withDayOfMonth(1)
        }
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .map { it.atStartOfDay(zone).toInstant().toEpochMilli() }
            .toList()
    }

    /** The last [count] day starts, oldest first, ending with today. */
    fun lastDays(count: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Long> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (count - 1 downTo 0).map { today.minusDays(it.toLong()).atStartOfDay(zone).toInstant().toEpochMilli() }
    }
}
