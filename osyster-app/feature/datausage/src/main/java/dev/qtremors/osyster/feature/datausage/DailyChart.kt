package dev.qtremors.osyster.feature.datausage

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.ui.components.BarChart
import dev.qtremors.osyster.core.ui.components.ChartBar
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageReport
import dev.qtremors.osyster.core.usage.domain.NetworkFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Mobile under Wi-Fi for each day. Week bars are labeled by weekday, month bars by date. */
@Composable
internal fun DailyChart(report: DataUsageReport, filter: NetworkFilter) {
    val colors = MaterialTheme.colorScheme
    val zone = ZoneId.systemDefault()
    val locale = Locale.getDefault()
    val bars = report.days.map { day ->
        val date = Instant.ofEpochMilli(day.dayStart).atZone(zone).toLocalDate()
        val label = if (report.period == DataPeriod.WEEK) date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) else date.dayOfMonth.toString()
        val segments = when (filter) {
            NetworkFilter.ALL -> listOf(day.totals.mobile.toFloat(), day.totals.wifi.toFloat())
            NetworkFilter.MOBILE -> listOf(day.totals.mobile.toFloat(), 0f)
            NetworkFilter.WIFI -> listOf(0f, day.totals.wifi.toFloat())
        }
        ChartBar(segments, label)
    }
    val dayTemplate = stringResource(R.string.data_chart_day)
    val spoken = report.days.joinToString(". ") { day ->
        val date = Instant.ofEpochMilli(day.dayStart).atZone(zone).toLocalDate()
        String.format(locale, dayTemplate, date.dayOfMonth.toString(), UsageFormat.bytes(day.totals.total(filter)))
    }
    Column {
        SectionHeader(stringResource(R.string.data_by_day))
        ConnectedGroup {
            BarChart(
                bars = bars,
                segmentColors = listOf(colors.primary, colors.tertiary),
                description = stringResource(R.string.data_chart_description, spoken),
                selectedIndex = bars.lastIndex,
                labelEvery = if (bars.size > 10) 5 else 1
            )
        }
    }
}
