package dev.qtremors.osyster.feature.powertools

import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.privilege.domain.IconBlacklist
import dev.qtremors.osyster.core.privilege.domain.Keys
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.StatusIcon
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.privilege.domain.readFlag
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import javax.inject.Inject

data class StatusIconsState(val blacklist: IconBlacklist = IconBlacklist(emptySet()), val batteryPercent: Boolean = false)

@HiltViewModel
class StatusBarIconsViewModel @Inject constructor(
    shell: PrivilegedShell,
    settings: SystemSettings
) : PowerViewModel(shell, settings) {
    val state = settingsState(setOf(Keys.IconBlacklist, Keys.BatteryPercent), StatusIconsState()) {
        StatusIconsState(
            blacklist = IconBlacklist.parse(settings.read(Keys.IconBlacklist)),
            batteryPercent = settings.readFlag(Keys.BatteryPercent)
        )
    }

    fun setShown(icon: StatusIcon, shown: Boolean) =
        write(Keys.IconBlacklist, state.value.blacklist.with(icon, hide = !shown).format())

    fun setBatteryPercent(on: Boolean) = write(Keys.BatteryPercent, if (on) "1" else "0")

    fun showAll() = write(Keys.IconBlacklist, null)
}

@Composable
internal fun StatusBarIconsRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    val viewModel: StatusBarIconsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbar)
    val enabled = access.canWriteSettings
    OsysterScaffold(title = stringResource(R.string.status_icons_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "gate") { AccessCard(access, needsShell = false) { onNavigate(PowerToolsDestination.Setup) } }
            item(key = "battery") {
                ConnectedGroup {
                    ToggleRow(
                        title = stringResource(R.string.status_icons_battery_percent),
                        supporting = stringResource(R.string.status_icons_battery_percent_summary),
                        checked = state.batteryPercent,
                        onCheckedChange = viewModel::setBatteryPercent,
                        enabled = enabled
                    )
                }
            }
            item(key = "icons") {
                SectionHeader(stringResource(R.string.status_icons_shown))
                ConnectedGroup {
                    StatusIcon.entries.forEach { icon ->
                        ToggleRow(
                            title = stringResource(icon.title),
                            checked = !state.blacklist.isHidden(icon),
                            onCheckedChange = { viewModel.setShown(icon, it) },
                            enabled = enabled
                        )
                    }
                }
            }
            item(key = "reset") {
                ActionButtons {
                    OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = viewModel::showAll, enabled = enabled && state.blacklist.hidden.isNotEmpty()) {
                        Text(stringResource(R.string.status_icons_show_all))
                    }
                }
            }
        }
    }
}

private val StatusIcon.title: Int
    get() = when (this) {
        StatusIcon.ALARM -> R.string.status_icon_alarm
        StatusIcon.BLUETOOTH -> R.string.status_icon_bluetooth
        StatusIcon.CAST -> R.string.status_icon_cast
        StatusIcon.HOTSPOT -> R.string.status_icon_hotspot
        StatusIcon.VPN -> R.string.status_icon_vpn
        StatusIcon.HEADSET -> R.string.status_icon_headset
        StatusIcon.ROTATE -> R.string.status_icon_rotate
        StatusIcon.DATA_SAVER -> R.string.status_icon_data_saver
        StatusIcon.AIRPLANE -> R.string.status_icon_airplane
        StatusIcon.MOBILE -> R.string.status_icon_mobile
        StatusIcon.WIFI -> R.string.status_icon_wifi
        StatusIcon.ETHERNET -> R.string.status_icon_ethernet
        StatusIcon.ZEN -> R.string.status_icon_zen
        StatusIcon.VOLUME -> R.string.status_icon_volume
        StatusIcon.NFC -> R.string.status_icon_nfc
        StatusIcon.LOCATION -> R.string.status_icon_location
        StatusIcon.CLOCK -> R.string.status_icon_clock
    }
