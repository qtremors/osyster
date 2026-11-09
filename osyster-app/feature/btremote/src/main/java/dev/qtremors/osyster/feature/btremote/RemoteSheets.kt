package dev.qtremors.osyster.feature.btremote

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.hid.domain.KeyCodes
import dev.qtremors.osyster.core.hid.domain.LayoutChoice
import dev.qtremors.osyster.core.hid.domain.RemoteDevice
import dev.qtremors.osyster.core.hid.domain.RemoteNavigation
import dev.qtremors.osyster.core.hid.domain.RemoteSettings
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.ToggleRow

@Composable
internal fun RemoteSettingsSheet(
    settings: RemoteSettings,
    onChange: ((RemoteSettings) -> RemoteSettings) -> Unit,
    onDismiss: () -> Unit,
    /** The connected device's layout choice, or null with nothing connected. */
    layoutChoice: LayoutChoice? = null,
    onLayout: (LayoutChoice) -> Unit = {}
) {
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.remote_settings)) {
        ConnectedGroup {
            if (layoutChoice != null) {
                ChoiceRow(
                    title = stringResource(R.string.remote_settings_layout),
                    supporting = stringResource(R.string.remote_settings_layout_summary),
                    options = LayoutChoice.entries,
                    selected = layoutChoice,
                    label = { stringResource(it.label) },
                    onSelect = onLayout
                )
            }
            ChoiceRow(
                title = stringResource(R.string.remote_settings_navigation),
                supporting = stringResource(R.string.remote_settings_navigation_summary),
                options = RemoteNavigation.entries,
                selected = settings.navigation,
                label = { stringResource(if (it == RemoteNavigation.DPAD) R.string.remote_nav_dpad else R.string.remote_nav_swipe) },
                onSelect = { choice -> onChange { it.copy(navigation = choice) } }
            )
            ToggleRow(
                title = stringResource(R.string.remote_settings_minimal),
                supporting = stringResource(R.string.remote_settings_minimal_summary),
                checked = settings.minimal,
                onCheckedChange = { on -> onChange { it.copy(minimal = on) } }
            )
            ToggleRow(
                title = stringResource(R.string.remote_settings_enter),
                supporting = stringResource(R.string.remote_settings_enter_summary),
                checked = settings.enterSelects,
                onCheckedChange = { on -> onChange { it.copy(enterSelects = on) } }
            )
            ToggleRow(
                title = stringResource(R.string.remote_settings_notification),
                supporting = stringResource(R.string.remote_settings_notification_summary),
                checked = settings.notificationButtons,
                onCheckedChange = { on -> onChange { it.copy(notificationButtons = on) } }
            )
        }
    }
}

/** What discovery needs: Nearby devices on Android 12+, location on Android 11 and older. */
private val SCAN_PERMISSIONS: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) NEARBY_PERMISSIONS else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

@Composable
internal fun ScanSheet(viewModel: RemoteViewModel, onConnect: (RemoteDevice) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val found by viewModel.found.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    fun granted() = SCAN_PERMISSIONS.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (granted()) viewModel.startScan()
    }
    LaunchedEffect(Unit) {
        if (granted()) viewModel.startScan() else permissions.launch(SCAN_PERMISSIONS)
    }

    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.remote_scan_title)) {
        Text(
            text = stringResource(R.string.remote_scan_pair_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        ConnectedGroup(modifier = Modifier.weight(1f, fill = false)) {
            when {
                !granted() -> ConnectedRow(
                    title = stringResource(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.string.remote_scan_permission else R.string.remote_scan_permission_old
                    ),
                    onClick = { permissions.launch(SCAN_PERMISSIONS) }
                )
                else -> {
                    found.forEach { device ->
                        DeviceRow(device, stringResource(device.kind.label), connecting = false, onClick = { onConnect(device) })
                    }
                    if (scanning) {
                        ConnectedRow(
                            title = stringResource(R.string.remote_scan_looking),
                            leading = { LoadingIndicator(modifier = Modifier.size(40.dp)) }
                        )
                    } else if (found.isEmpty()) {
                        ConnectedRow(
                            title = stringResource(R.string.remote_scan_none),
                            supporting = stringResource(R.string.remote_scan_none_summary),
                            leading = { IconBadge(icon = Icons.Rounded.Search, key = "remote-scan-none") }
                        )
                    }
                }
            }
        }
        if (!scanning && granted()) {
            FilledTonalButton(
                shapes = ButtonDefaults.shapes(),
                onClick = { viewModel.startScan() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text(stringResource(R.string.remote_scan_again)) }
        }
    }
}

/** For TVs that never show up in a scan: make this phone visible and pair from the TV's side. */
@Composable
internal fun PairFromTvSheet(phoneName: String?, onDismiss: () -> Unit) {
    val visible = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.remote_pair_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Step(stringResource(R.string.remote_pair_step1_title), stringResource(R.string.remote_pair_step1))
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    runCatching {
                        visible.launch(
                            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, VISIBLE_SECONDS)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.remote_pair_visible)) }
            Step(
                stringResource(R.string.remote_pair_step2_title),
                stringResource(R.string.remote_pair_step2, phoneName?.ifBlank { null } ?: stringResource(R.string.remote_pair_this_phone))
            )
            Step(stringResource(R.string.remote_pair_step3_title), stringResource(R.string.remote_pair_step3))
        }
    }
}

@Composable
private fun Step(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Digits for live TV, sent as the keyboard's number keys, which TVs read like a remote's number pad. */
@Composable
internal fun ChannelPadSheet(onKey: (Int) -> Unit, onDismiss: () -> Unit) {
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.remote_channel_pad)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { digit -> DigitKey(digit.toString(), Modifier.weight(1f)) { onKey(KeyCodes.digit(digit)) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DigitKey("0", Modifier.weight(1f)) { onKey(KeyCodes.digit(0)) }
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { onKey(KeyCodes.ENTER) },
                    modifier = Modifier.weight(2f).height(64.dp)
                ) { Text(stringResource(R.string.remote_digit_enter), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun DigitKey(text: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = onClick, modifier = modifier.height(64.dp)) {
        Text(text, style = MaterialTheme.typography.headlineSmall)
    }
}

internal val LayoutChoice.label: Int
    get() = when (this) {
        LayoutChoice.AUTO -> R.string.remote_layout_auto
        LayoutChoice.TV -> R.string.remote_kind_tv
        LayoutChoice.COMPUTER -> R.string.remote_kind_computer
        LayoutChoice.TABLET -> R.string.remote_kind_phone
    }

private const val VISIBLE_SECONDS = 300
