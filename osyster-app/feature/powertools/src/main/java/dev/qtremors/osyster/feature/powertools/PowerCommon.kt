package dev.qtremors.osyster.feature.powertools

import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.privilege.domain.PowerApp
import dev.qtremors.osyster.core.privilege.domain.PrivilegeStatus
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.InlineSearchField
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import kotlinx.coroutines.flow.Flow

/** Short results the power tool pages report in a snackbar. */
enum class PowerMessage(val text: Int) {
    NO_ACCESS(R.string.power_message_no_access),
    NEEDS_SHELL(R.string.power_message_needs_shell),
    FAILED(R.string.power_message_failed),
    GRANTED(R.string.power_message_granted),
    ROOT_ON(R.string.power_message_root_on),
    ROOT_FAILED(R.string.power_message_root_failed),
    RESTORED(R.string.power_message_restored),
    NOTHING_TO_RESTORE(R.string.power_message_nothing_to_restore),
    LAUNCH_FAILED(R.string.power_message_launch_failed),
    INVALID_HOST(R.string.power_message_invalid_host),
    PINNED(R.string.power_message_pinned),
    PIN_UNSUPPORTED(R.string.power_message_pin_unsupported)
}

/** Shows each message from [messages] once in [host]. */
@Composable
internal fun MessageEffect(messages: Flow<PowerMessage>, host: SnackbarHostState) {
    val resources = LocalResources.current
    LaunchedEffect(messages) {
        messages.collect { message -> host.showSnackbar(resources.getString(message.text)) }
    }
}

/**
 * Sits at the top of a power tool when its access is missing. The controls below stay visible
 * but disabled, so people can see what the tool does before setting anything up.
 */
@Composable
internal fun AccessCard(status: PrivilegeStatus, needsShell: Boolean, onSetup: () -> Unit) {
    val ready = if (needsShell) status.shell else status.canWriteSettings
    if (ready) return
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    stringResource(if (needsShell) R.string.power_gate_shell_title else R.string.power_gate_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Text(
                stringResource(if (needsShell) R.string.power_gate_shell_message else R.string.power_gate_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Button(shapes = ButtonDefaults.shapes(), onClick = onSetup) { Text(stringResource(R.string.power_gate_action)) }
        }
    }
}

/** An app row with its icon; used by Shut-Up!, Standby apps, and Freeze. */
@Composable
internal fun AppRow(
    app: PowerApp,
    supporting: String?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    ConnectedRow(
        title = app.label,
        supporting = supporting,
        leading = { AppIcon(app.packageName) },
        trailing = trailing?.let { content -> { content() } },
        onClick = onClick,
        onLongClick = onLongClick
    )
}

/**
 * Choose apps for a list. Starts with [selected] ticked; system apps without a launcher screen are
 * hidden unless already chosen, since they are rarely what anyone means.
 */
@Composable
internal fun AppPickerSheet(
    title: String,
    apps: List<PowerApp>,
    selected: Set<String>,
    onDone: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var chosen by remember(selected) { mutableStateOf(selected) }
    val visible = remember(apps, query, selected) {
        apps.filter { it.packageName in selected || !it.system || it.launchable }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    }
    OsysterBottomSheet(onDismissRequest = onDismiss, title = title) {
        Column {
            InlineSearchField(query = query, onQueryChange = { query = it }, placeholder = stringResource(R.string.power_search_apps))
            LazyColumn(modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 460.dp).padding(top = 12.dp)) {
                item {
                    ConnectedGroup {
                        visible.forEach { app ->
                            val checked = app.packageName in chosen
                            AppRow(
                                app = app,
                                supporting = app.packageName,
                                onClick = { chosen = if (checked) chosen - app.packageName else chosen + app.packageName },
                                trailing = { Checkbox(checked = checked, onCheckedChange = null) }
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.power_cancel)) }
                Button(shapes = ButtonDefaults.shapes(), onClick = { onDone(chosen) }) { Text(stringResource(R.string.power_done, chosen.size)) }
            }
        }
    }
}

/** A full-width row of buttons under a group, such as Reset or Freeze all. */
@Composable
internal fun ActionButtons(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
    ) { content() }
}

