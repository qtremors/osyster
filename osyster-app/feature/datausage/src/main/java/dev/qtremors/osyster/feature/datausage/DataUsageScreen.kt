package dev.qtremors.osyster.feature.datausage

import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import dev.qtremors.osyster.core.ui.components.InlineSearchField
import dev.qtremors.osyster.core.usage.domain.maxOffset
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
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onQuery: (String) -> Unit,
    val onFilter: (NetworkFilter) -> Unit,
    val onToggleShowAll: () -> Unit,
    val onGrantAccess: () -> Unit,
    val onOpenApp: (String) -> Unit
)

@Composable
internal fun DataUsageScreen(state: DataUsageState, live: LiveRate?, actions: DataUsageActions) {
    OsysterScaffold(title = stringResource(Tool.DATA_USAGE.label.title)) { padding ->
        LazyColumn(contentPadding = padding) {
            when (state.access) {
                null -> item(key = "loading") { LoadingState() }
                UsageAccessState.MISSING -> item(key = "gate") {
                    UsageAccessGate(stringResource(R.string.data_usage_access_message), actions.onGrantAccess)
                }
                UsageAccessState.GRANTED -> {
                    item(key = "choices") { Choices(state, actions) }
                    item(key = "live") { LiveRow(live) }
                    val report = state.report
                    if (report == null) {
                        item(key = "report_loading") { LoadingState() }
                    } else {
                        item(key = "summary") { Summary(report, state.filter) }
                        if (report.days.size > 1) item(key = "chart") { DailyChart(report, state.filter) }
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
        ConnectedChoices(
            options = DataPeriod.entries,
            selected = state.period,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onPeriod
        )
        PeriodStepper(state, actions)
        ConnectedChoices(
            options = NetworkFilter.entries,
            selected = state.filter,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onFilter
        )
    }
}

@Composable
private fun Summary(report: DataUsageReport, filter: NetworkFilter) {
    val colors = MaterialTheme.colorScheme
    val totals = report.totals
    Column {
        SectionHeader(stringResource(R.string.data_total))
        Surface(color = colors.primaryContainer, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(UsageFormat.bytes(totals.total(filter)), style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize), color = colors.onPrimaryContainer)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Labeled(stringResource(R.string.data_downloaded), UsageFormat.bytes(totals.received(filter)))
                    Labeled(stringResource(R.string.data_uploaded), UsageFormat.bytes(totals.sent(filter)))
                }
                if (filter == NetworkFilter.ALL) {
                    val all = totals.total(NetworkFilter.ALL).coerceAtLeast(1)
                    LinearWavyProgressIndicator(
                        progress = { totals.mobile.toFloat() / all },
                        color = colors.primary,
                        trackColor = colors.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Labeled(stringResource(R.string.data_filter_mobile), UsageFormat.bytes(totals.mobile))
                        Labeled(stringResource(R.string.data_filter_wifi), UsageFormat.bytes(totals.wifi))
                    }
                }
            }
        }
    }
}

@Composable
private fun Labeled(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
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

/** Previous and next period, with the dates in between, as the old Network page had. */
@Composable
private fun PeriodStepper(state: DataUsageState, actions: DataUsageActions) {
    val report = state.report
    val label = when {
        report == null -> ""
        state.offset == 0 -> stringResource(state.period.labelRes())
        report.firstDay == report.lastDay -> UsageFormat.day(report.firstDay)
        else -> stringResource(R.string.data_range, UsageFormat.day(report.firstDay), UsageFormat.day(report.lastDay))
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = actions.onPrevious, enabled = state.offset < maxOffset(state.period)) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.data_previous))
        }
        Text(label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = actions.onNext, enabled = state.offset > 0) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.data_next))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appRows(report: DataUsageReport, state: DataUsageState, actions: DataUsageActions) {
    val all = report.appsFor(state.filter)
    val apps = if (state.query.isBlank()) all else all.filter { it.label.contains(state.query, ignoreCase = true) || it.packageName.contains(state.query, ignoreCase = true) }
    val shown = if (state.showAllApps || state.query.isNotBlank()) apps else apps.take(COLLAPSED_APPS)
    item(key = "apps_header") { SectionHeader(stringResource(R.string.data_apps)) }
    if (all.size > COLLAPSED_APPS) {
        item(key = "apps_search") {
            InlineSearchField(query = state.query, onQueryChange = actions.onQuery, placeholder = stringResource(R.string.data_search), modifier = Modifier.padding(bottom = 8.dp))
        }
    }
    if (apps.isEmpty()) {
        item(key = "apps_empty") {
            ConnectedGroup { ConnectedRow(title = stringResource(R.string.data_no_apps)) }
        }
        return
    }
    items(shown, key = { "app_" + it.packageName }) { app -> AppRow(app, state.filter, actions.onOpenApp) }
    if (apps.size > COLLAPSED_APPS && state.query.isBlank()) {
        item(key = "apps_toggle") {
            ConnectedGroup(modifier = Modifier.padding(top = 2.dp)) {
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
    // Each app is its own rounded row with a hairline gap, matching connected groups.
    ConnectedGroup(modifier = Modifier.padding(bottom = 2.dp)) {
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
}

private fun DataPeriod.labelRes() = when (this) {
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
