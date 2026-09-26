package dev.qtremors.osyster.feature.powertools

import android.content.ClipData
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.privilege.domain.PrivilegeStatus
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.ShellRoute
import dev.qtremors.osyster.core.privilege.domain.ShizukuState
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PowerSetupViewModel @Inject constructor(
    shell: PrivilegedShell,
    settings: SystemSettings
) : PowerViewModel(shell, settings) {
    fun requestShizuku() = shell.requestShizukuPermission()

    fun setRoot(on: Boolean) {
        if (!on) {
            shell.disableRoot()
            return
        }
        viewModelScope.launch { say(if (shell.enableRoot()) PowerMessage.ROOT_ON else PowerMessage.ROOT_FAILED) }
    }

    fun grant() {
        viewModelScope.launch {
            when {
                !access.value.shell -> say(PowerMessage.NEEDS_SHELL)
                shell.grantSecureSettings() -> say(PowerMessage.GRANTED)
                else -> say(PowerMessage.FAILED)
            }
        }
    }
}

@Composable
internal fun PowerSetupRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    val viewModel: PowerSetupViewModel = hiltViewModel()
    val status by viewModel.access.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbar)
    OsysterScaffold(title = stringResource(R.string.power_setup_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.power_setup_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            item(key = "status") { StatusSection(status) }
            item(key = "shizuku") { ShizukuSection(status.shizuku, viewModel::requestShizuku, onNavigate) }
            item(key = "root") { RootSection(status.root, viewModel::setRoot) }
            item(key = "grant") { GrantSection(status, viewModel::grant) }
        }
    }
}

@Composable
private fun StatusSection(status: PrivilegeStatus) {
    SectionHeader(stringResource(R.string.power_setup_status))
    ConnectedGroup {
        ConnectedRow(
            title = stringResource(R.string.power_setup_settings_access),
            supporting = stringResource(if (status.secureSettings) R.string.power_setup_settings_granted else R.string.power_setup_settings_missing),
            leading = { IconBadge(Icons.Rounded.Key, key = "secure") },
            trailing = { if (status.secureSettings) GrantedIcon() }
        )
        ConnectedRow(
            title = stringResource(R.string.power_setup_shell),
            supporting = stringResource(
                when (status.shellRoute) {
                    ShellRoute.SHIZUKU -> R.string.power_setup_shell_shizuku
                    ShellRoute.ROOT -> R.string.power_setup_shell_root
                    ShellRoute.NONE -> R.string.power_setup_shell_none
                }
            ),
            leading = { IconBadge(Icons.Rounded.Terminal, key = "shell") },
            trailing = { if (status.shell) GrantedIcon() }
        )
    }
}

@Composable
private fun ShizukuSection(state: ShizukuState, onRequest: () -> Unit, onNavigate: (PowerToolsDestination) -> Unit) {
    SectionHeader(stringResource(R.string.power_setup_shizuku))
    ConnectedGroup {
        val openGuide = { onNavigate(PowerToolsDestination.OpenUrl(SHIZUKU_URL)) }
        val summary = when (state) {
            ShizukuState.NOT_RUNNING -> R.string.power_setup_shizuku_not_running
            ShizukuState.UNSUPPORTED -> R.string.power_setup_shizuku_old
            ShizukuState.NEEDS_PERMISSION -> R.string.power_setup_shizuku_allow
            ShizukuState.READY -> R.string.power_setup_shizuku_ready
        }
        val action: (() -> Unit)? = when (state) {
            ShizukuState.NOT_RUNNING, ShizukuState.UNSUPPORTED -> openGuide
            ShizukuState.NEEDS_PERMISSION -> onRequest
            ShizukuState.READY -> null
        }
        ConnectedRow(
            title = stringResource(R.string.power_setup_shizuku_title),
            supporting = stringResource(summary),
            leading = { IconBadge(Icons.Rounded.Tag, key = "shizuku") },
            trailing = {
                when {
                    state == ShizukuState.READY -> GrantedIcon()
                    state != ShizukuState.NEEDS_PERMISSION -> Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                }
            },
            onClick = action
        )
    }
}

@Composable
private fun RootSection(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    SectionHeader(stringResource(R.string.power_setup_root))
    ConnectedGroup {
        ToggleRow(
            title = stringResource(R.string.power_setup_root_title),
            supporting = stringResource(R.string.power_setup_root_summary),
            checked = enabled,
            onCheckedChange = onToggle,
            icon = Icons.Rounded.Security,
            badgeKey = "root"
        )
    }
}

@Composable
private fun GrantSection(status: PrivilegeStatus, onGrant: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val command = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    SectionHeader(stringResource(R.string.power_setup_grant))
    ConnectedGroup {
        if (status.shell && !status.secureSettings) {
            ConnectedRow(
                title = stringResource(R.string.power_setup_grant_now),
                supporting = stringResource(R.string.power_setup_grant_now_summary),
                leading = { IconBadge(Icons.Rounded.Key, key = "grant") },
                onClick = onGrant
            )
        }
        ConnectedRow(
            title = stringResource(R.string.power_setup_adb),
            supporting = stringResource(R.string.power_setup_adb_summary),
            leading = { IconBadge(Icons.Rounded.Terminal, key = "adb") }
        )
        ConnectedRow(
            title = command,
            supporting = stringResource(R.string.power_setup_copy),
            trailing = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
            onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("adb", command))) } }
        )
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp)) {
        Text(
            stringResource(R.string.power_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun GrantedIcon() {
    Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.power_granted), tint = MaterialTheme.colorScheme.primary)
}

private const val SHIZUKU_URL = "https://shizuku.rikka.app/guide/setup/"
