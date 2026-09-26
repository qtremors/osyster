package dev.qtremors.osyster.feature.appstopper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.usage.domain.ManagedApp

@Immutable
internal data class AppStopperActions(
    val onStop: (String) -> Unit,
    val onStopAll: () -> Unit,
    val onRemove: (String) -> Unit,
    val onOpenPicker: () -> Unit,
    val onPickerSystem: (Boolean) -> Unit,
    val onAdd: (Collection<String>) -> Unit
)

@Composable
internal fun AppStopperScreen(apps: List<ManagedApp>?, picker: PickerState, oneTap: Boolean, actions: AppStopperActions) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    OsysterScaffold(title = stringResource(Tool.APP_STOPPER.label.title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "intro") { Intro(onAdd = { actions.onOpenPicker(); showPicker = true }) }
            when {
                apps == null -> item(key = "loading") { LoadingState() }
                apps.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.StopCircle,
                        title = stringResource(R.string.stopper_empty_title),
                        message = stringResource(R.string.stopper_empty_message)
                    )
                }
                else -> {
                    item(key = "header") {
                        SectionHeader(
                            text = stringResource(R.string.stopper_your_apps, apps.size),
                            trailing = if (oneTap && apps.any { it.installed && !it.stopped }) {
                                { TextButton(onClick = actions.onStopAll) { Text(stringResource(R.string.stopper_stop_all)) } }
                            } else null
                        )
                    }
                    items(apps, key = { it.packageName }) { app -> ManagedRow(app, actions) }
                }
            }
        }
    }
    if (showPicker) {
        AppPickerSheet(
            state = picker,
            alreadyAdded = apps.orEmpty().map(ManagedApp::packageName).toSet(),
            onIncludeSystem = actions.onPickerSystem,
            onConfirm = { chosen ->
                actions.onAdd(chosen)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun Intro(onAdd: () -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        ConnectedGroup {
            ConnectedRow(
                title = stringResource(R.string.stopper_how_title),
                supporting = stringResource(R.string.stopper_how_message),
                leading = { IconBadge(Icons.Rounded.Info, key = "stopper_info") }
            )
            ConnectedRow(
                title = stringResource(R.string.stopper_add),
                leading = { IconBadge(Icons.Rounded.Add, key = "stopper_add") },
                onClick = onAdd
            )
        }
    }
}

/** One managed app. Long-press to remove it from the list. */
@Composable
private fun ManagedRow(app: ManagedApp, actions: AppStopperActions) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(bottom = 2.dp)) {
        ConnectedGroup {
            ConnectedRow(
                title = app.label,
                supporting = stringResource(
                    when {
                        !app.installed -> R.string.stopper_status_uninstalled
                        app.stopped -> R.string.stopper_status_stopped
                        else -> R.string.stopper_status_can_run
                    }
                ),
                leading = { AppIcon(app.packageName) },
                trailing = {
                    if (app.installed) {
                        FilledTonalButton(onClick = { actions.onStop(app.packageName) }, enabled = !app.stopped) {
                            Text(stringResource(R.string.stopper_stop))
                        }
                    } else {
                        OutlinedButton(onClick = { actions.onRemove(app.packageName) }) {
                            Text(stringResource(R.string.stopper_remove))
                        }
                    }
                },
                onLongClick = { menuOpen = true }
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.stopper_remove_from_list)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuOpen = false
                    actions.onRemove(app.packageName)
                }
            )
        }
    }
}
