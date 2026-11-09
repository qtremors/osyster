package dev.qtremors.osyster.feature.datausage

import dev.qtremors.osyster.core.usage.domain.PeriodButtons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.navigation.AppUsageRoute
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ChartPart
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.PillBarChart
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.usage.domain.AppDataHistory
import dev.qtremors.osyster.core.usage.domain.AppScreenDay
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.NetworkFilter
import dev.qtremors.osyster.core.usage.domain.ScreenUsageSource
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUsageState(
    val period: DataPeriod = DataPeriod.WEEK,
    val data: AppDataHistory? = null,
    val screen: List<AppScreenDay>? = null,
    val allowed: Boolean = true
)

/** One app's data and screen time, opened from either usage tool. */
@HiltViewModel
class AppUsageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val data: DataUsageSource,
    private val screen: ScreenUsageSource
) : ViewModel() {
    private val route = savedState.toRoute<AppUsageRoute>()
    val packageName: String = route.packageName
    /** Opened from Screen usage: only screen time is shown and loaded. */
    val screenTime: Boolean = route.screenTime

    private val _state = MutableStateFlow(AppUsageState())
    val state = _state.asStateFlow()

    fun load() {
        if (data.accessState() != UsageAccessState.GRANTED) {
            _state.update { it.copy(allowed = false) }
            return
        }
        if (!screenTime) {
            loadData(_state.value.period)
            return
        }
        viewModelScope.launch {
            val days = runCatchingCancellable { screen.appHistory(packageName, SCREEN_DAYS) }.getOrNull()
            _state.update { it.copy(screen = days.orEmpty()) }
        }
    }

    fun setPeriod(period: DataPeriod) {
        if (period == _state.value.period) return
        _state.update { it.copy(period = period) }
        loadData(period)
    }

    private fun loadData(period: DataPeriod) {
        viewModelScope.launch {
            val history = runCatchingCancellable { data.appHistory(packageName, period) }.getOrNull()
            _state.update { current -> if (current.period == period) current.copy(data = history) else current }
        }
    }

    private companion object {
        const val SCREEN_DAYS = 7
    }
}

@Composable
internal fun AppUsageScreen(packageName: String, screenTime: Boolean, state: AppUsageState, onPeriod: (DataPeriod) -> Unit, onAppInfo: () -> Unit) {
    val appLabel = rememberAppLabel(packageName)
    OsysterScaffold(title = state.data?.label ?: appLabel ?: stringResource(if (screenTime) R.string.app_usage_screen_title else R.string.app_usage_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "header") { Header(packageName, onAppInfo) }
            if (!state.allowed) {
                item(key = "denied") { Text(stringResource(R.string.data_usage_access_message), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                return@LazyColumn
            }
            if (screenTime) {
                val days = state.screen
                item(key = "screen") { if (days == null) LoadingState() else ScreenTimeSection(days) }
                return@LazyColumn
            }
            item(key = "period") {
                ConnectedChoices(
                    options = PeriodButtons,
                    selected = state.period,
                    label = { stringResource(it.labelRes()) },
                    onSelect = onPeriod,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            val data = state.data
            item(key = "data_header") { SectionHeader(stringResource(R.string.app_usage_data)) }
            if (data == null) {
                item(key = "data_loading") { LoadingState() }
            } else {
                item(key = "data_totals") { TotalsCard(data.totals, NetworkFilter.ALL) }
                item(key = "data_chart") {
                    Column(modifier = Modifier.padding(top = 8.dp)) { BucketChart(data.buckets, data.hourly, NetworkFilter.ALL) }
                }
            }
        }
    }
}

/** The app's icon and package; tapping the row opens its App info, as everywhere else. */
@Composable
private fun Header(packageName: String, onAppInfo: () -> Unit) {
    Surface(
        onClick = onAppInfo,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppIcon(packageName, size = 52.dp)
            Text(packageName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
    }
}

/** The app's name as the launcher shows it, or null when it is not installed. */
@Composable
private fun rememberAppLabel(packageName: String): String? {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(packageName) {
        runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString() }.getOrNull()
    }
}

/** Today, the daily average, and opens, above the seven-day chart. */
@Composable
private fun ScreenTimeSection(days: List<AppScreenDay>) {
    val week = days.sumOf { it.foregroundMillis }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.app_usage_screen))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(stringResource(R.string.app_usage_today), UsageFormat.duration(days.lastOrNull()?.foregroundMillis ?: 0L), Modifier.weight(1f))
            StatCard(stringResource(R.string.app_usage_average), UsageFormat.duration(if (days.isEmpty()) 0L else week / days.size), Modifier.weight(1f))
            StatCard(stringResource(R.string.app_usage_opens), days.sumOf { it.opens }.toString(), Modifier.weight(1f))
        }
        ScreenCard(days)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.large, modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

/** Seven days of this app's screen time as bars, with the week's total and opens above. */
@Composable
private fun ScreenCard(days: List<AppScreenDay>) {
    val colors = MaterialTheme.colorScheme
    var selected by remember(days) { mutableStateOf<Int?>(null) }
    val picked = selected?.let(days::getOrNull)
    Surface(color = colors.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = picked?.let { stringResource(R.string.app_usage_screen_day, UsageFormat.day(it.dayStart), UsageFormat.duration(it.foregroundMillis), it.opens) }
                    ?: stringResource(R.string.app_usage_screen_week, UsageFormat.duration(days.sumOf { it.foregroundMillis }), days.sumOf { it.opens }),
                style = MaterialTheme.typography.titleSmall
            )
            PillBarChart(
                bars = days.map { listOf(ChartPart(it.foregroundMillis.toFloat(), colors.primary)) },
                selected = selected,
                onSelect = { index -> selected = if (selected == index) null else index },
                description = stringResource(R.string.app_usage_screen_week, UsageFormat.duration(days.sumOf { it.foregroundMillis }), days.sumOf { it.opens }),
                height = 110.dp
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                val zone = java.time.ZoneId.systemDefault()
                val locale = java.util.Locale.getDefault()
                days.forEachIndexed { index, day ->
                    Text(
                        text = java.time.Instant.ofEpochMilli(day.dayStart).atZone(zone).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == selected) colors.primary else colors.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
