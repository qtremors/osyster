package dev.qtremors.osyster.feature.datausage

import dev.qtremors.osyster.core.ui.components.ChartColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.ChartPart
import dev.qtremors.osyster.core.ui.components.PillBarChart
import dev.qtremors.osyster.core.ui.components.UsageRing
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.usage.domain.NetworkFilter
import dev.qtremors.osyster.core.usage.domain.TrafficTotals
import dev.qtremors.osyster.core.usage.domain.UsageBucket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The summary card: a download and upload ring with the total inside, and four numbers beside
 * it. Shared by Data usage and each app's own page.
 */
@Composable
internal fun TotalsCard(totals: TrafficTotals, filter: NetworkFilter) {
    val colors = MaterialTheme.colorScheme
    val chart = ChartColors.current()
    val down = totals.received(filter)
    val up = totals.sent(filter)
    val total = down + up
    Surface(color = colors.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            UsageRing(
                parts = listOf(ChartPart(down.toFloat(), chart.download), ChartPart(up.toFloat(), chart.upload)),
                centerValue = UsageFormat.bytes(total),
                centerLabel = stringResource(R.string.data_total),
                description = stringResource(R.string.data_ring_description, UsageFormat.bytes(down), UsageFormat.bytes(up))
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat(stringResource(R.string.data_downloaded), UsageFormat.bytes(down), chart.download, Modifier.weight(1f))
                    Stat(stringResource(R.string.data_uploaded), UsageFormat.bytes(up), chart.upload, Modifier.weight(1f))
                }
                if (filter == NetworkFilter.ALL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stat(stringResource(R.string.data_filter_mobile), UsageFormat.bytes(totals.mobile), chart.mobile, Modifier.weight(1f))
                        Stat(stringResource(R.string.data_filter_wifi), UsageFormat.bytes(totals.wifi), chart.wifi, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, dot: Color?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (dot != null) Box(Modifier.size(8.dp).background(dot, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Stacked pill bars of download and upload per hour or day. Tapping a bar shows its time and
 * amount above the chart; nothing is selected at first, so the line shows the whole period.
 */
@Composable
internal fun BucketChart(buckets: List<UsageBucket>, hourly: Boolean, filter: NetworkFilter) {
    val colors = MaterialTheme.colorScheme
    val chart = ChartColors.current()
    var selected by remember(buckets) { mutableStateOf<Int?>(null) }
    val picked = selected?.let(buckets::getOrNull)
    Surface(color = colors.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = picked?.let { stringResource(R.string.data_chart_day, bucketLabel(it.start, hourly), UsageFormat.bytes(it.totals.total(filter))) }
                    ?: stringResource(if (hourly) R.string.data_by_hour else R.string.data_by_day),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface
            )
            PillBarChart(
                // Download at the bottom of each bar, upload stacked on top, as the legend reads.
                bars = buckets.map { listOf(ChartPart(it.totals.sent(filter).toFloat(), chart.upload), ChartPart(it.totals.received(filter).toFloat(), chart.download)) },
                selected = selected,
                onSelect = { index -> selected = if (selected == index) null else index },
                description = stringResource(R.string.data_chart_description, UsageFormat.bytes(buckets.sumOf { it.totals.total(filter) }))
            )
            // Time labels sit right under the bars they name; the legend comes after.
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                buckets.firstOrNull()?.let { Text(bucketLabel(it.start, hourly), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant) }
                buckets.lastOrNull()?.let { Text(bucketLabel(it.start, hourly), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(stringResource(R.string.data_downloaded), chart.download)
                Legend(stringResource(R.string.data_uploaded), chart.upload)
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "3 PM" style hours and "3 Sep" style days, in the device's locale. */
internal fun bucketLabel(start: Long, hourly: Boolean, locale: Locale = Locale.getDefault()): String =
    if (hourly) SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "j"), locale).format(Date(start))
    else UsageFormat.day(start, locale)
