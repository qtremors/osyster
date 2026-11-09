package dev.qtremors.osyster.feature.stayawake

import androidx.compose.material3.ButtonDefaults
import android.content.ClipData
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.system.domain.StayAwakeDurationsMinutes
import dev.qtremors.osyster.core.system.domain.StayAwakeTimer
import dev.qtremors.osyster.core.system.domain.WhileChargingState
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import dev.qtremors.osyster.core.ui.theme.ValueTextStyle
import java.util.Date
import kotlinx.coroutines.launch

@Immutable
internal data class StayAwakeActions(
    val onSelect: (Int?) -> Unit,
    val onStart: () -> Unit,
    val onStop: () -> Unit,
    val onWhileCharging: (Boolean) -> Unit,
    val onOpenDeveloperOptions: () -> Unit,
    val onGrant: () -> Unit
)

@Composable
internal fun StayAwakeScreen(
    timer: StayAwakeTimer,
    selectedMinutes: Int?,
    whileCharging: WhileChargingState,
    adbCommand: String,
    canGrant: Boolean,
    actions: StayAwakeActions
) {
    OsysterScaffold(title = stringResource(Tool.STAY_AWAKE.label.title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "status") { Status(timer) }
            item(key = "timer") { TimerSection(timer, selectedMinutes, actions) }
            item(key = "charging") { WhileChargingSection(whileCharging, adbCommand, canGrant, actions) }
        }
    }
}

@Composable
private fun Status(timer: StayAwakeTimer) {
    val colors = MaterialTheme.colorScheme
    val running = timer is StayAwakeTimer.Running
    val context = LocalContext.current
    Surface(
        color = if (running) colors.primaryContainer else colors.surfaceBright,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(if (running) R.string.stay_awake_on else R.string.stay_awake_off),
                style = MaterialTheme.typography.headlineSmall,
                color = if (running) colors.onPrimaryContainer else colors.onSurface
            )
            Text(
                text = when (timer) {
                    StayAwakeTimer.Off -> stringResource(R.string.stay_awake_off_detail)
                    is StayAwakeTimer.Running -> timer.endsAt?.let {
                        stringResource(R.string.stay_awake_until, DateFormat.getTimeFormat(context).format(Date(it)))
                    } ?: stringResource(R.string.stay_awake_until_stopped)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (running) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerSection(timer: StayAwakeTimer, selectedMinutes: Int?, actions: StayAwakeActions) {
    Column {
        SectionHeader(stringResource(R.string.stay_awake_timer))
        ConnectedChoices(
            options = StayAwakeDurationsMinutes,
            selected = selectedMinutes,
            label = { minutes -> durationLabel(minutes) },
            onSelect = actions.onSelect
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(shapes = ButtonDefaults.shapes(), onClick = actions.onStart, modifier = Modifier.weight(1f)) {
                Text(stringResource(if (timer is StayAwakeTimer.Running) R.string.stay_awake_restart else R.string.stay_awake_start))
            }
            if (timer is StayAwakeTimer.Running) {
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = actions.onStop, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.stay_awake_stop)) }
            }
        }
    }
}

@Composable
private fun WhileChargingSection(state: WhileChargingState, adbCommand: String, canGrant: Boolean, actions: StayAwakeActions) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Column {
        SectionHeader(stringResource(R.string.stay_awake_while_charging))
        ConnectedGroup {
            if (state.canChange) {
                ToggleRow(
                    title = stringResource(R.string.stay_awake_while_charging_toggle),
                    supporting = stringResource(R.string.stay_awake_while_charging_summary),
                    icon = Icons.Rounded.Power,
                    checked = state.enabled,
                    onCheckedChange = actions.onWhileCharging
                )
            } else {
                if (canGrant) {
                    ConnectedRow(
                        title = stringResource(R.string.stay_awake_grant_now),
                        supporting = stringResource(R.string.stay_awake_grant_now_summary),
                        leading = { IconBadge(Icons.Rounded.Power, key = "grant_now") },
                        onClick = actions.onGrant
                    )
                }
                ConnectedRow(
                    title = stringResource(R.string.stay_awake_grant_title),
                    supporting = stringResource(R.string.stay_awake_grant_message),
                    leading = { IconBadge(Icons.Rounded.Terminal, key = "adb") }
                )
                Surface(color = MaterialTheme.colorScheme.surfaceBright) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(adbCommand, style = ValueTextStyle, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                    }
                }
                ConnectedRow(
                    title = stringResource(R.string.stay_awake_copy_command),
                    onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("adb", adbCommand))) } }
                )
                ConnectedRow(
                    title = stringResource(R.string.stay_awake_developer_options),
                    supporting = stringResource(R.string.stay_awake_developer_options_summary),
                    leading = { IconBadge(Icons.Rounded.DeveloperMode, key = "dev_options") },
                    onClick = actions.onOpenDeveloperOptions
                )
            }
        }
    }
}

@Composable
internal fun durationLabel(minutes: Int?): String = when {
    minutes == null -> stringResource(R.string.stay_awake_duration_forever)
    minutes < 60 -> stringResource(R.string.stay_awake_duration_minutes, minutes)
    else -> stringResource(R.string.stay_awake_duration_hours, minutes / 60)
}
