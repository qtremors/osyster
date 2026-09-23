package dev.qtremors.osyster.feature.screenusage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.BarChart
import dev.qtremors.osyster.core.ui.components.ChartBar
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.UsageAccessGate
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.ui.theme.MetricTextStyle
import dev.qtremors.osyster.core.usage.domain.AppScreenTime
import dev.qtremors.osyster.core.usage.domain.ScreenDay
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun ScreenUsageScreen(
    state: ScreenUsageState,
    onSelectDay: (Int) -> Unit,
    onToggleShowAll: () -> Unit,
    onGrantAccess: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    OsysterScaffold(title = stringResource(Tool.SCREEN_USAGE.label.title)) { padding ->
        LazyColumn(contentPadding = padding) {
            when (state.access) {
                null -> item(key = "loading") { LoadingState() }
                UsageAccessState.MISSING -> item(key = "gate") {
                    UsageAccessGate(stringResource(R.string.screen_access_message), onGrantAccess)
                }
                UsageAccessState.GRANTED -> {
                    if (state.history.isEmpty()) {
                        item(key = "history_loading") { LoadingState() }
                        return@LazyColumn
                    }
                    item(key = "hero") { DayHero(state) }
                    item(key = "history") { HistoryChart(state, onSelectDay) }
                    val day = state.day
                    if (day == null) {
                        item(key = "day_loading") { LoadingState() }
                    } else {
                        item(key = "hourly") { HourlyChart(day) }
                        appRows(day, state.showAllApps, onToggleShowAll, onOpenApp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHero(state: ScreenUsageState) {
    val colors = MaterialTheme.colorScheme
    val selected = state.history.getOrNull(state.selectedIndex)
    val total = state.day?.totalMillis ?: selected?.totalMillis ?: 0L
    Surface(color = colors.primaryContainer, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(dayName(selected?.dayStart, state.selectedIndex == state.history.lastIndex), style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer.copy(alpha = 0.8f))
            Text(UsageFormat.duration(total), style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize), color = colors.onPrimaryContainer)
            state.day?.let { day ->
                val opens = day.apps.sumOf(AppScreenTime::opens)
                Text(
                    text = pluralStringResource(R.plurals.screen_apps_and_opens, day.apps.size, day.apps.size, opens),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun HistoryChart(state: ScreenUsageState, onSelect: (Int) -> Unit) {
    val locale = Locale.getDefault()
    val zone = ZoneId.systemDefault()
    val bars = state.history.map { day ->
        val date = Instant.ofEpochMilli(day.dayStart).atZone(zone).toLocalDate()
        ChartBar(listOf(day.totalMillis.toFloat()), date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale))
    }
    val spoken = state.history.joinToString(". ") { day ->
        val date = Instant.ofEpochMilli(day.dayStart).atZone(zone).toLocalDate()
        "${date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${UsageFormat.duration(day.totalMillis)}"
    }
    Column {
        SectionHeader(stringResource(R.string.screen_last_week))
        ConnectedGroup {
            BarChart(
                bars = bars,
                segmentColors = listOf(MaterialTheme.colorScheme.primary),
                description = stringResource(R.string.screen_history_description, spoken),
                selectedIndex = state.selectedIndex,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun HourlyChart(day: ScreenDay) {
    val bars = day.hourlyMillis.mapIndexed { hour, millis -> ChartBar(listOf(millis.toFloat()), hour.toString()) }
    val busiest = day.hourlyMillis.withIndex().maxByOrNull { it.value }
    Column {
        SectionHeader(stringResource(R.string.screen_by_hour))
        ConnectedGroup {
            BarChart(
                bars = bars,
                segmentColors = listOf(MaterialTheme.colorScheme.tertiary),
                description = if (busiest != null && busiest.value > 0) {
                    stringResource(R.string.screen_hourly_description, busiest.index, UsageFormat.duration(busiest.value))
                } else stringResource(R.string.screen_hourly_empty),
                height = 96.dp,
                labelEvery = 6
            )
        }
    }
}

private fun LazyListScope.appRows(day: ScreenDay, showAll: Boolean, onToggle: () -> Unit, onOpen: (String) -> Unit) {
    val shown = if (showAll) day.apps else day.apps.take(COLLAPSED_APPS)
    item(key = "apps_header") { SectionHeader(stringResource(R.string.screen_apps)) }
    if (day.apps.isEmpty()) {
        item(key = "apps_empty") { ConnectedGroup { ConnectedRow(title = stringResource(R.string.screen_no_apps)) } }
        return
    }
    items(shown, key = { "app_" + it.packageName }) { app ->
        ConnectedGroup(modifier = Modifier.padding(bottom = 2.dp)) {
            ConnectedRow(
                title = app.label,
                supporting = pluralStringResource(R.plurals.screen_opens, app.opens, app.opens),
                leading = { AppIcon(app.packageName) },
                trailing = { Text(UsageFormat.duration(app.foregroundMillis), style = MaterialTheme.typography.titleSmall) },
                onClick = { onOpen(app.packageName) }
            )
        }
    }
    if (day.apps.size > COLLAPSED_APPS) {
        item(key = "apps_toggle") {
            ConnectedGroup {
                ConnectedRow(
                    title = if (showAll) stringResource(R.string.screen_show_fewer) else stringResource(R.string.screen_show_all, day.apps.size),
                    onClick = onToggle
                )
            }
        }
    }
}

@Composable
private fun dayName(dayStart: Long?, isToday: Boolean): String {
    if (isToday || dayStart == null) return stringResource(R.string.screen_today)
    val date: LocalDate = Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()))
}

private const val COLLAPSED_APPS = 15
