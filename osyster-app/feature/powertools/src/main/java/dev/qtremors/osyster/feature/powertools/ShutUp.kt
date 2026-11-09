package dev.qtremors.osyster.feature.powertools

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.privilege.domain.PowerApp
import dev.qtremors.osyster.core.privilege.domain.PowerApps
import dev.qtremors.osyster.core.privilege.domain.PowerToolsStore
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.QuietLaunch
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ShutUpViewModel @Inject constructor(
    shell: PrivilegedShell,
    settings: SystemSettings,
    private val store: PowerToolsStore,
    private val apps: PowerApps,
    private val quiet: QuietLaunch,
    @param:ApplicationContext private val context: Context
) : PowerViewModel(shell, settings) {
    val quietApps = store.quietApps
    val snapshot = store.debugSnapshot

    private val _installed = MutableStateFlow<List<PowerApp>>(emptyList())
    val installed = _installed.asStateFlow()

    fun load() {
        viewModelScope.launch { _installed.value = apps.apps() }
    }

    fun setApps(selected: Set<String>) = store.setQuietApps(selected)

    fun remove(packageName: String) = store.setQuietApps(store.quietApps.value - packageName)

    fun openQuietly(app: PowerApp) {
        viewModelScope.launch {
            if (!quiet.hide()) say(PowerMessage.NO_ACCESS)
            QuietNotification.show(context)
            if (!apps.launch(app.packageName)) say(PowerMessage.LAUNCH_FAILED)
        }
    }

    fun restore() {
        viewModelScope.launch {
            when {
                snapshot.value == null -> say(PowerMessage.NOTHING_TO_RESTORE)
                quiet.restore() -> {
                    QuietNotification.cancel(context)
                    say(PowerMessage.RESTORED)
                }
                else -> say(PowerMessage.NO_ACCESS)
            }
        }
    }

    fun pin(app: PowerApp) {
        say(if (pinQuietShortcut(context, app.packageName, app.label)) PowerMessage.PINNED else PowerMessage.PIN_UNSUPPORTED)
    }
}

@Composable
internal fun ShutUpRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    val viewModel: ShutUpViewModel = hiltViewModel()
    val access by viewModel.access.collectAsStateWithLifecycle()
    val chosen by viewModel.quietApps.collectAsStateWithLifecycle()
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        viewModel.load()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbar)
    val rows = installed.filter { it.packageName in chosen }
    OsysterScaffold(title = stringResource(R.string.shut_up_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "gate") { AccessCard(access, needsShell = false) { onNavigate(PowerToolsDestination.Setup) } }
            item(key = "intro") {
                Text(
                    stringResource(R.string.shut_up_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            if (snapshot != null) {
                item(key = "restore") { RestoreCard(viewModel::restore) }
            }
            item(key = "apps") {
                // The header names a list; with nothing added yet the empty state speaks for itself.
                if (rows.isNotEmpty()) SectionHeader(stringResource(R.string.shut_up_apps))
                if (rows.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.DoNotDisturbOn,
                        title = stringResource(R.string.shut_up_empty_title),
                        message = stringResource(R.string.shut_up_empty_message),
                        actionLabel = stringResource(R.string.shut_up_add),
                        onAction = { picking = true }
                    )
                } else {
                    ConnectedGroup {
                        rows.forEach { app ->
                            QuietAppRow(app, onOpen = { viewModel.openQuietly(app) }, onPin = { viewModel.pin(app) }, onRemove = { viewModel.remove(app.packageName) })
                        }
                    }
                    ActionButtons {
                        OutlinedButton(onClick = { picking = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text(stringResource(R.string.shut_up_add), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
    if (picking) {
        AppPickerSheet(
            title = stringResource(R.string.shut_up_pick),
            apps = installed,
            selected = chosen,
            onDone = {
                viewModel.setApps(it)
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
}

@Composable
private fun RestoreCard(onRestore: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.shut_up_hidden_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.shut_up_hidden_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(onClick = onRestore) {
                Icon(Icons.Rounded.Restore, contentDescription = null)
                Text(stringResource(R.string.shut_up_restore), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun QuietAppRow(app: PowerApp, onOpen: () -> Unit, onPin: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        AppRow(
            app = app,
            supporting = stringResource(R.string.shut_up_row_summary),
            onClick = onOpen,
            onLongClick = { menu = true }
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.shut_up_pin)) },
                leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                onClick = {
                    menu = false
                    onPin()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.shut_up_remove)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    menu = false
                    onRemove()
                }
            )
        }
    }
}
