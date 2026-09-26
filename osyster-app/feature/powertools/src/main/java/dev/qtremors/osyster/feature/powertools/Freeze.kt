package dev.qtremors.osyster.feature.powertools

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.privilege.domain.PowerApp
import dev.qtremors.osyster.core.privilege.domain.PowerApps
import dev.qtremors.osyster.core.privilege.domain.PowerCommands
import dev.qtremors.osyster.core.privilege.domain.PowerToolsStore
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.ExpressiveSwitch
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FreezeViewModel @Inject constructor(
    shell: PrivilegedShell,
    settings: SystemSettings,
    private val store: PowerToolsStore,
    private val apps: PowerApps
) : PowerViewModel(shell, settings) {
    val chosen = store.freezeList

    private val _installed = MutableStateFlow<List<PowerApp>>(emptyList())
    val installed = _installed.asStateFlow()

    fun load() {
        viewModelScope.launch { _installed.value = apps.apps() }
    }

    fun setList(selected: Set<String>) = store.setFreezeList(selected)

    fun remove(packageName: String) = store.setFreezeList(store.freezeList.value - packageName)

    fun setFrozen(app: PowerApp, frozen: Boolean) = run(listOf(app.packageName), frozen)

    fun setAll(frozen: Boolean) = run(chosen.value.toList(), frozen)

    private fun run(packages: List<String>, frozen: Boolean) {
        viewModelScope.launch {
            if (!access.value.shell) {
                say(PowerMessage.NEEDS_SHELL)
                return@launch
            }
            val failed = packages.count { pkg ->
                !shell.run(if (frozen) PowerCommands.freeze(pkg) else PowerCommands.unfreeze(pkg)).ok
            }
            if (failed > 0) say(PowerMessage.FAILED)
            _installed.value = apps.apps()
        }
    }
}

@Composable
internal fun FreezeRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    val viewModel: FreezeViewModel = hiltViewModel()
    val access by viewModel.access.collectAsStateWithLifecycle()
    val chosen by viewModel.chosen.collectAsStateWithLifecycle()
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<PowerApp?>(null) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        viewModel.load()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbar)
    val rows = installed.filter { it.packageName in chosen }
    OsysterScaffold(title = stringResource(R.string.freeze_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "gate") { AccessCard(access, needsShell = true) { onNavigate(PowerToolsDestination.Setup) } }
            item(key = "intro") {
                Text(
                    stringResource(R.string.freeze_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            item(key = "list") {
                SectionHeader(stringResource(R.string.freeze_apps))
                if (rows.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.AcUnit,
                        title = stringResource(R.string.freeze_empty_title),
                        message = stringResource(R.string.freeze_empty_message),
                        actionLabel = stringResource(R.string.freeze_add),
                        onAction = { picking = true }
                    )
                } else {
                    ConnectedGroup {
                        rows.forEach { app ->
                            AppRow(
                                app = app,
                                supporting = stringResource(if (app.enabled) R.string.freeze_running else R.string.freeze_frozen),
                                onClick = { viewModel.setFrozen(app, app.enabled) },
                                onLongClick = { removing = app },
                                trailing = { ExpressiveSwitch(checked = !app.enabled, onCheckedChange = null, enabled = access.shell) }
                            )
                        }
                    }
                    ActionButtons {
                        OutlinedButton(onClick = { picking = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text(stringResource(R.string.freeze_add), modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = { viewModel.setAll(false) }, enabled = access.shell) { Text(stringResource(R.string.freeze_unfreeze_all)) }
                        Button(onClick = { viewModel.setAll(true) }, enabled = access.shell) { Text(stringResource(R.string.freeze_freeze_all)) }
                    }
                }
            }
        }
    }
    if (picking) {
        AppPickerSheet(
            title = stringResource(R.string.freeze_pick),
            apps = installed,
            selected = chosen,
            onDone = {
                viewModel.setList(it)
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
    removing?.let { app ->
        OsysterBottomSheet(onDismissRequest = { removing = null }, title = app.label) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    stringResource(if (app.enabled) R.string.freeze_remove_message else R.string.freeze_remove_frozen_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                ActionButtons {
                    TextButton(onClick = { removing = null }) { Text(stringResource(R.string.power_cancel)) }
                    Button(onClick = {
                        if (!app.enabled) viewModel.setFrozen(app, false)
                        viewModel.remove(app.packageName)
                        removing = null
                    }) { Text(stringResource(R.string.freeze_remove)) }
                }
            }
        }
    }
}
