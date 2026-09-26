package dev.qtremors.osyster.feature.powertools

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.StandbyBucket
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.privilege.domain.parseStandbyBuckets
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.InlineSearchField
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StandbyEntry(val app: PowerApp, val bucket: StandbyBucket)

@HiltViewModel
class StandbyAppsViewModel @Inject constructor(
    shell: PrivilegedShell,
    settings: SystemSettings,
    private val apps: PowerApps
) : PowerViewModel(shell, settings) {
    private val _entries = MutableStateFlow<List<StandbyEntry>?>(null)
    /** Null while loading or without a shell; buckets can only be read as the shell user. */
    val entries = _entries.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (!access.value.shell) {
                _entries.value = emptyList()
                return@launch
            }
            val result = shell.run(PowerCommands.standbyBuckets)
            val buckets = if (result.ok) parseStandbyBuckets(result.output) else emptyMap()
            _entries.value = apps.apps()
                .filter { it.enabled && (!it.system || it.launchable) }
                .mapNotNull { app -> buckets[app.packageName]?.let { StandbyEntry(app, it) } }
        }
    }

    fun setBucket(entry: StandbyEntry, bucket: StandbyBucket) {
        viewModelScope.launch {
            val ok = shell.run(PowerCommands.setStandbyBucket(entry.app.packageName, bucket)).ok
            if (ok) {
                _entries.value = _entries.value?.map { if (it.app.packageName == entry.app.packageName) it.copy(bucket = bucket) else it }
            } else {
                say(PowerMessage.FAILED)
            }
        }
    }
}

@Composable
internal fun StandbyAppsRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    val viewModel: StandbyAppsViewModel = hiltViewModel()
    val access by viewModel.access.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<StandbyEntry?>(null) }
    LifecycleResumeEffect(access.shell) {
        viewModel.refresh()
        viewModel.load()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbar)
    OsysterScaffold(title = stringResource(R.string.standby_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "gate") { AccessCard(access, needsShell = true) { onNavigate(PowerToolsDestination.Setup) } }
            item(key = "intro") {
                Text(
                    stringResource(R.string.standby_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            val list = entries
            when {
                !access.shell -> Unit
                list == null -> item(key = "loading") { LoadingState() }
                else -> {
                    item(key = "search") {
                        InlineSearchField(query = query, onQueryChange = { query = it }, placeholder = stringResource(R.string.power_search_apps))
                    }
                    val filtered = list.filter { query.isBlank() || it.app.label.contains(query, ignoreCase = true) }
                    filtered.groupBy(StandbyEntry::bucket).toSortedMap().forEach { (bucket, group) ->
                        item(key = bucket.name) {
                            SectionHeader(stringResource(bucket.title))
                            ConnectedGroup {
                                group.forEach { entry ->
                                    AppRow(app = entry.app, supporting = stringResource(bucket.summary), onClick = if (bucket.settable) ({ editing = entry }) else null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { entry ->
        OsysterBottomSheet(onDismissRequest = { editing = null }, title = entry.app.label) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.standby_pick), style = MaterialTheme.typography.bodyMedium)
                ConnectedChoices(
                    options = StandbyBucket.Settable,
                    selected = entry.bucket,
                    label = { stringResource(it.short) },
                    onSelect = { bucket ->
                        viewModel.setBucket(entry, bucket)
                        editing = null
                    },
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Text(
                    stringResource(R.string.standby_reset_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

private val StandbyBucket.title: Int
    get() = when (this) {
        StandbyBucket.EXEMPTED -> R.string.standby_exempted
        StandbyBucket.ACTIVE -> R.string.standby_active
        StandbyBucket.WORKING_SET -> R.string.standby_working_set
        StandbyBucket.FREQUENT -> R.string.standby_frequent
        StandbyBucket.RARE -> R.string.standby_rare
        StandbyBucket.RESTRICTED -> R.string.standby_restricted
        StandbyBucket.NEVER -> R.string.standby_never
    }

private val StandbyBucket.short: Int
    get() = when (this) {
        StandbyBucket.ACTIVE -> R.string.standby_active_short
        StandbyBucket.WORKING_SET -> R.string.standby_working_set_short
        StandbyBucket.FREQUENT -> R.string.standby_frequent_short
        StandbyBucket.RARE -> R.string.standby_rare_short
        StandbyBucket.RESTRICTED -> R.string.standby_restricted_short
        StandbyBucket.EXEMPTED, StandbyBucket.NEVER -> title
    }

private val StandbyBucket.summary: Int
    get() = when (this) {
        StandbyBucket.EXEMPTED -> R.string.standby_exempted_summary
        StandbyBucket.ACTIVE -> R.string.standby_active_summary
        StandbyBucket.WORKING_SET -> R.string.standby_working_set_summary
        StandbyBucket.FREQUENT -> R.string.standby_frequent_summary
        StandbyBucket.RARE -> R.string.standby_rare_summary
        StandbyBucket.RESTRICTED -> R.string.standby_restricted_summary
        StandbyBucket.NEVER -> R.string.standby_never_summary
    }
