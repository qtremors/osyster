package dev.qtremors.osyster.feature.batteryhistory

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.battery.domain.BatteryHealth
import dev.qtremors.osyster.core.battery.domain.BatterySample
import dev.qtremors.osyster.core.battery.domain.ChargeSession
import dev.qtremors.osyster.core.battery.domain.PlugType
import dev.qtremors.osyster.core.battery.domain.chargeSessions
import dev.qtremors.osyster.core.battery.domain.drainPerHour
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedItem
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import java.util.Locale

/** Battery level over time, charging sessions, drain, temperature, and health. */
@Composable
internal fun BatteryHistoryScreen(viewModel: BatteryHistoryViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val samples by viewModel.samples.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    OsysterScaffold(
        title = stringResource(R.string.battery_title),
        actions = {
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { settingsOpen = true }) {
                Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.battery_settings))
            }
        }
    ) { padding ->
        val loaded = settings
        when {
            loaded == null -> LoadingState(modifier = Modifier.padding(padding))
            !loaded.enabled -> Box(modifier = Modifier.padding(padding)) {
                EmptyState(
                    Icons.Rounded.BatteryStd,
                    stringResource(R.string.battery_off_title),
                    stringResource(R.string.battery_off_message),
                    actionLabel = stringResource(R.string.battery_turn_on),
                    onAction = { viewModel.setEnabled(true) }
                )
            }
            else -> HistoryContent(viewModel, padding, range, samples, now)
        }
    }

    if (settingsOpen) settings?.let { current ->
        BatterySettingsSheet(enabled = current.enabled, onEnabled = viewModel::setEnabled, onClear = viewModel::clear, onDismiss = { settingsOpen = false })
    }
}

@Composable
private fun HistoryContent(viewModel: BatteryHistoryViewModel, padding: PaddingValues, range: BatteryRange, samples: List<BatterySample>?, now: BatterySample?) {
    val context = LocalContext.current
    val list = samples
    val sessions = remember(list) { list?.let(::chargeSessions).orEmpty().reversed() }
    val drain = remember(list) { list?.let(::drainPerHour) }
    LazyColumn(contentPadding = padding) {
        item(key = "now") { NowCard(now ?: list?.lastOrNull(), viewModel.facts.cycleCount) }
        item(key = "range") {
            ConnectedChoices(
                options = BatteryRange.entries,
                selected = range,
                label = { stringResource(it.label) },
                onSelect = viewModel::range,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
        when {
            list == null -> item(key = "loading") { LoadingState() }
            list.size < 2 -> item(key = "collecting") {
                Text(
                    stringResource(R.string.battery_collecting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            else -> {
                item(key = "chart") { LevelChart(list, range) }
                item(key = "stats-header") { SectionHeader(stringResource(R.string.battery_stats)) }
                item(key = "stats") { Stats(list, sessions, drain, viewModel.facts.health) }
                item(key = "sessions-header") { SectionHeader(stringResource(R.string.battery_sessions)) }
                if (sessions.isEmpty()) {
                    item(key = "no-sessions") {
                        Text(stringResource(R.string.battery_no_sessions), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                } else {
                    itemsIndexed(sessions, key = { _, it -> it.start }) { index, session ->
                        ConnectedItem(index, sessions.size) { SessionRow(context, session) }
                    }
                }
            }
        }
    }
}

/** The battery right now: level, whether it charges and how fast, temperature, and cycles. */
@Composable
private fun NowCard(sample: BatterySample?, cycles: Int?) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.primaryContainer, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(24.dp)) {
            Icon(
                if (sample?.charging == true) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.height(48.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(sample?.let { "${it.level}%" } ?: "–", style = MaterialTheme.typography.displaySmall, color = colors.onPrimaryContainer)
                val status = when {
                    sample == null -> null
                    sample.charging && sample.level >= 100 -> stringResource(R.string.battery_now_full)
                    sample.charging -> stringResource(R.string.battery_now_charging)
                    else -> stringResource(R.string.battery_now_draining)
                }
                val line = listOfNotNull(
                    status,
                    sample?.currentMa?.let { stringResource(R.string.battery_current, kotlin.math.abs(it)) },
                    sample?.temperatureC?.let { stringResource(R.string.battery_temperature, oneDecimal(it)) },
                    cycles?.let { stringResource(R.string.battery_cycles, it) }
                ).joinToString(" · ")
                Text(line, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
            }
        }
    }
}

/**
 * Level from 0 to 100% over the range, as a line, with charging stretches shaded behind it.
 * Gaps longer than two hours (the phone was off, or readings were paused) break the line.
 */
@Composable
private fun LevelChart(samples: List<BatterySample>, range: BatteryRange) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.battery_level_chart)
    val end = samples.last().at
    val start = end - range.millis
    Surface(color = colors.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(20.dp).semantics { contentDescription = description }) {
            val x = { at: Long -> ((at - start).toFloat() / range.millis) * size.width }
            val y = { level: Int -> size.height - level / 100f * size.height }
            listOf(0, 50, 100).forEach { level ->
                drawLine(colors.outlineVariant, Offset(0f, y(level)), Offset(size.width, y(level)), strokeWidth = 1.dp.toPx())
            }
            for ((a, b) in samples.zipWithNext()) {
                if (a.charging && b.at - a.at < GAP_MS) {
                    drawRect(colors.tertiaryContainer, topLeft = Offset(x(a.at), 0f), size = Size((x(b.at) - x(a.at)).coerceAtLeast(1f), size.height))
                }
            }
            val path = Path()
            samples.forEachIndexed { i, sample ->
                val point = Offset(x(sample.at).coerceAtLeast(0f), y(sample.level))
                if (i == 0 || sample.at - samples[i - 1].at > GAP_MS) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, colors.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun Stats(samples: List<BatterySample>, sessions: List<ChargeSession>, drain: Double?, health: BatteryHealth) {
    val temps = samples.mapNotNull { it.temperatureC }
    ConnectedGroup {
        ConnectedRow(
            title = stringResource(R.string.battery_drain),
            supporting = drain?.let { stringResource(R.string.battery_drain_value, oneDecimal(it.toFloat())) } ?: stringResource(R.string.battery_drain_none),
            leading = { IconBadge(Icons.AutoMirrored.Rounded.TrendingDown, key = "battery-drain") }
        )
        if (temps.isNotEmpty()) {
            ConnectedRow(
                title = stringResource(R.string.battery_temp_range),
                supporting = if (oneDecimal(temps.min()) == oneDecimal(temps.max())) stringResource(R.string.battery_temperature, oneDecimal(temps.min()))
                else stringResource(R.string.battery_temp_range_value, oneDecimal(temps.min()), oneDecimal(temps.max())),
                leading = { IconBadge(Icons.Rounded.Thermostat, key = "battery-temp") }
            )
        }
        ConnectedRow(
            title = stringResource(R.string.battery_charges),
            supporting = pluralStringResource(R.plurals.battery_charges_value, sessions.size, sessions.size, sessions.sumOf { (it.toLevel - it.fromLevel).coerceAtLeast(0) }),
            leading = { IconBadge(Icons.Rounded.Bolt, key = "battery-charges") }
        )
        ConnectedRow(
            title = stringResource(R.string.battery_health),
            supporting = stringResource(health.label),
            leading = { IconBadge(Icons.Rounded.HealthAndSafety, key = "battery-health") }
        )
    }
}

@Composable
private fun SessionRow(context: Context, session: ChargeSession) {
    val whenText = DateUtils.formatDateRange(context, session.start, session.end, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    val duration = formatDuration(context, session.end - session.start)
    val parts = listOfNotNull(
        stringResource(session.plugged.label),
        session.averageCurrentMa?.let { stringResource(R.string.battery_current, it) },
        session.maxTemperatureC?.let { stringResource(R.string.battery_temperature, oneDecimal(it)) }
    ).joinToString(" · ")
    val levels = if (session.fromLevel == session.toLevel) stringResource(R.string.battery_session_at, session.toLevel)
    else stringResource(R.string.battery_session_title, session.fromLevel, session.toLevel)
    ConnectedRow(
        title = if (session.ongoing) stringResource(R.string.battery_session_now_line, levels) else levels,
        supporting = stringResource(R.string.battery_session_detail, "$whenText · $duration", parts),
        leading = { IconBadge(session.plugged.icon, key = "battery-plug-${session.plugged.name}") }
    )
}

@Composable
private fun BatterySettingsSheet(enabled: Boolean, onEnabled: (Boolean) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    var clearing by rememberSaveable { mutableStateOf(false) }
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.battery_settings)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            ConnectedGroup {
                ToggleRow(
                    title = stringResource(R.string.battery_enabled),
                    supporting = stringResource(R.string.battery_enabled_detail),
                    checked = enabled,
                    onCheckedChange = onEnabled
                )
            }
            Button(
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                onClick = { clearing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                Text(stringResource(R.string.battery_clear), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    if (clearing) {
        AlertDialog(
            onDismissRequest = { clearing = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.battery_clear_confirm)) },
            text = { Text(stringResource(R.string.battery_clear_body)) },
            confirmButton = {
                Button(shapes = ButtonDefaults.shapes(), onClick = {
                    onClear()
                    clearing = false
                }) { Text(stringResource(R.string.battery_clear)) }
            },
            dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = { clearing = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }
}

private fun formatDuration(context: Context, millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(1)
    return if (minutes < 60) context.resources.getQuantityString(R.plurals.battery_minutes, minutes.toInt(), minutes.toInt())
    else context.resources.getQuantityString(R.plurals.battery_hours, (minutes / 60).toInt(), (minutes / 60).toInt()) +
        if (minutes % 60 > 0) " " + context.resources.getQuantityString(R.plurals.battery_minutes, (minutes % 60).toInt(), (minutes % 60).toInt()) else ""
}

private fun oneDecimal(value: Float): String = String.format(Locale.getDefault(), "%.1f", value)

private val BatteryRange.label: Int
    get() = when (this) {
        BatteryRange.DAY -> R.string.battery_day
        BatteryRange.WEEK -> R.string.battery_week
        BatteryRange.MONTH -> R.string.battery_month
    }

private val PlugType.label: Int
    get() = when (this) {
        PlugType.AC -> R.string.battery_plug_ac
        PlugType.USB -> R.string.battery_plug_usb
        PlugType.WIRELESS -> R.string.battery_plug_wireless
        PlugType.DOCK -> R.string.battery_plug_dock
        PlugType.NONE -> R.string.battery_plug_unknown
    }

private val PlugType.icon
    get() = when (this) {
        PlugType.USB -> Icons.Rounded.Usb
        else -> Icons.Rounded.Bolt
    }

private val BatteryHealth.label: Int
    get() = when (this) {
        BatteryHealth.GOOD -> R.string.battery_health_good
        BatteryHealth.OVERHEAT -> R.string.battery_health_overheat
        BatteryHealth.DEAD -> R.string.battery_health_dead
        BatteryHealth.OVER_VOLTAGE -> R.string.battery_health_over_voltage
        BatteryHealth.COLD -> R.string.battery_health_cold
        BatteryHealth.FAILURE -> R.string.battery_health_failure
        BatteryHealth.UNKNOWN -> R.string.battery_health_unknown
    }

/** Readings further apart than this are not joined: the phone was off or readings paused. */
private const val GAP_MS = 2 * 60 * 60_000L
