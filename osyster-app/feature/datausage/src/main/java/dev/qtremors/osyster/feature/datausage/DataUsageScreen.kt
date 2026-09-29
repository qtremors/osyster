package dev.qtremors.osyster.feature.datausage

import dev.qtremors.osyster.core.ui.components.ConnectedItem
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.IconButton
import dev.qtremors.osyster.core.usage.domain.maxOffset
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import dev.qtremors.osyster.core.usage.domain.NetworkButtons
import dev.qtremors.osyster.core.usage.domain.PeriodButtons
import dev.qtremors.osyster.core.usage.domain.earliestDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.qtremors.osyster.core.ui.components.FilterTop
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.UsageAccessGate
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.ui.theme.MetricTextStyle
import dev.qtremors.osyster.core.usage.domain.AppTraffic
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageReport
import dev.qtremors.osyster.core.usage.domain.LiveRate
import dev.qtremors.osyster.core.usage.domain.NetworkFilter
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.navigation.Tool

@Immutable
internal data class DataUsageActions(
    val onPeriod: (DataPeriod) -> Unit,
    val onJump: (LocalDate) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onQuery: (String) -> Unit,
    val onFilter: (NetworkFilter) -> Unit,
    val onToggleShowAll: () -> Unit,
    val onGrantAccess: () -> Unit,
    val onOpenApp: (String) -> Unit,
    val onOpenUsage: (String) -> Unit
)

@Composable
internal fun DataUsageScreen(state: DataUsageState, live: LiveRate?, actions: DataUsageActions) {
    // While the top-bar search is open, the page shows only the app list.
    var searching by rememberSaveable { mutableStateOf(false) }
    OsysterScaffold(
        title = stringResource(Tool.DATA_USAGE.label.title),
        filter = if (state.access == UsageAccessState.GRANTED) {
            FilterTop(state.query, actions.onQuery, stringResource(R.string.data_search), onOpenChange = { searching = it })
        } else null
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            when (state.access) {
                null -> item(key = "loading") { LoadingState() }
                UsageAccessState.MISSING -> item(key = "gate") {
                    UsageAccessGate(stringResource(R.string.data_usage_access_message), actions.onGrantAccess)
                }
                UsageAccessState.GRANTED -> {
                    if (!searching) {
                        item(key = "choices") { Choices(state, actions) }
                        item(key = "live") { LiveRow(live) }
                    }
                    val report = state.report
                    if (report == null) {
                        item(key = "report_loading") { LoadingState() }
                    } else {
                        if (!searching) item(key = "summary") {
                            Column {
                                SectionHeader(stringResource(R.string.data_total))
                                TotalsCard(report.totals, state.filter)
                            }
                        }
                        if (!searching) item(key = "chart") {
                            Column(modifier = Modifier.padding(top = 8.dp)) { BucketChart(report.buckets, report.hourly, state.filter) }
                        }
                        appRows(report, state, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun Choices(state: DataUsageState, actions: DataUsageActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        var picking by rememberSaveable { mutableStateOf(false) }
        ConnectedChoices(
            options = PeriodButtons,
            selected = state.period,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onPeriod,
            onReselect = { picking = true }
        )
        PeriodLabel(state, actions, onPick = { picking = true })
        if (picking) {
            PeriodDatePicker(state.period, onPick = { date ->
                picking = false
                actions.onJump(date)
            }, onDismiss = { picking = false })
        }
        ConnectedChoices(
            options = NetworkButtons,
            selected = state.filter,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onFilter
        )
    }
}


@Composable
private fun LiveRow(live: LiveRate?) {
    Column {
        SectionHeader(stringResource(R.string.data_live))
        ConnectedGroup {
            ConnectedRow(
                title = live?.let { stringResource(R.string.data_live_value, UsageFormat.rate(it.downBytesPerSecond), UsageFormat.rate(it.upBytesPerSecond)) }
                    ?: stringResource(R.string.data_live_waiting),
                leading = { IconBadge(Icons.Rounded.Speed, key = "live_speed") }
            )
        }
    }
}

/**
 * Previous and next step one period at a time; the dates in between are a button that opens a
 * date picker to jump anywhere. Tapping the selected period again opens the picker too.
 */
@Composable
private fun PeriodLabel(state: DataUsageState, actions: DataUsageActions, onPick: () -> Unit) {
    val report = state.report
    val label = when {
        report == null || state.offset == 0 -> stringResource(state.period.labelRes())
        report.firstDay == report.lastDay -> UsageFormat.day(report.firstDay)
        else -> stringResource(R.string.data_range, UsageFormat.day(report.firstDay), UsageFormat.day(report.lastDay))
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = actions.onPrevious, enabled = state.offset < maxOffset(state.period)) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.data_previous))
        }
        TextButton(onClick = onPick) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
        }
        IconButton(onClick = actions.onNext, enabled = state.offset > 0) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.data_next))
        }
    }
}

/** Android's date picker, limited to the days Android still keeps history for. */
@Composable
private fun PeriodDatePicker(period: DataPeriod, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val zone = ZoneOffset.UTC
    val today = LocalDate.now()
    val earliest = earliestDay(period, today)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = today.atStartOfDay(zone).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                return !day.isAfter(today) && !day.isBefore(earliest)
            }

            override fun isSelectableYear(year: Int): Boolean = year in earliest.year..today.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) } ?: onDismiss()
            }) { Text(stringResource(R.string.data_pick_go)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.data_pick_cancel)) } }
    ) {
        DatePicker(state = pickerState)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appRows(report: DataUsageReport, state: DataUsageState, actions: DataUsageActions) {
    val all = report.appsFor(state.filter)
    val apps = if (state.query.isBlank()) all else all.filter { it.label.contains(state.query, ignoreCase = true) || it.packageName.contains(state.query, ignoreCase = true) }
    val shown = if (state.showAllApps || state.query.isNotBlank()) apps else apps.take(COLLAPSED_APPS)
    item(key = "apps_header") { SectionHeader(stringResource(R.string.data_apps)) }
    if (apps.isEmpty()) {
        item(key = "apps_empty") {
            ConnectedGroup { ConnectedRow(title = stringResource(R.string.data_no_apps)) }
        }
        return
    }
    itemsIndexed(shown, key = { _, it -> "app_" + it.packageName }) { index, app ->
        ConnectedItem(index, shown.size) { AppRow(app, state.filter, actions.onOpenUsage) }
    }
    if (apps.size > COLLAPSED_APPS && state.query.isBlank()) {
        item(key = "apps_toggle") {
            ConnectedGroup(modifier = Modifier.padding(top = 8.dp)) {
                ConnectedRow(
                    title = if (state.showAllApps) stringResource(R.string.data_show_fewer) else stringResource(R.string.data_show_all, apps.size),
                    onClick = actions.onToggleShowAll
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: AppTraffic, filter: NetworkFilter, onOpen: (String) -> Unit) {
    ConnectedRow(
        title = app.label,
        supporting = if (filter == NetworkFilter.ALL) {
            stringResource(R.string.data_app_split, UsageFormat.bytes(app.totals.mobile), UsageFormat.bytes(app.totals.wifi))
        } else null,
        leading = { AppIcon(app.packageName) },
        trailing = {
            Text(UsageFormat.bytes(app.totals.total(filter)), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        },
        onClick = if (app.packageName.contains(':')) null else ({ onOpen(app.packageName) })
    )
}

internal fun DataPeriod.labelRes() = when (this) {
    DataPeriod.TODAY -> R.string.data_period_today
    DataPeriod.WEEK -> R.string.data_period_week
    DataPeriod.MONTH -> R.string.data_period_month
}

private fun NetworkFilter.labelRes() = when (this) {
    NetworkFilter.ALL -> R.string.data_filter_all
    NetworkFilter.MOBILE -> R.string.data_filter_mobile
    NetworkFilter.WIFI -> R.string.data_filter_wifi
}

private const val COLLAPSED_APPS = 12
