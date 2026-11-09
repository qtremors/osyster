package dev.qtremors.osyster.feature.btremote

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.TvOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.hid.domain.Connection
import dev.qtremors.osyster.core.hid.domain.DeviceKind
import dev.qtremors.osyster.core.hid.domain.HidSupport
import dev.qtremors.osyster.core.hid.domain.LayoutChoice
import dev.qtremors.osyster.core.hid.domain.layoutFor
import dev.qtremors.osyster.core.hid.domain.RemoteDevice
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

/** Nearby devices on Android 12+: connect, find, and make this phone visible for pairing from the TV. */
internal val NEARBY_PERMISSIONS: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        emptyArray()
    }

@Composable
internal fun RemoteScreen(viewModel: RemoteViewModel, onAppSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var scanOpen by rememberSaveable { mutableStateOf(false) }
    var pairOpen by rememberSaveable { mutableStateOf(false) }
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(Tool.BT_REMOTE.label.title)

    OsysterScaffold(
        title = title,
        actions = {
            if (state.connected) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { moreOpen = true }) {
                    Icon(Icons.Rounded.Apps, contentDescription = stringResource(R.string.remote_more_keys))
                }
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = viewModel::disconnect) {
                    Icon(Icons.Rounded.LinkOff, contentDescription = stringResource(R.string.remote_disconnect))
                }
            }
            if (state.support == HidSupport.READY) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { settingsOpen = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.remote_settings))
                }
            }
        }
    ) { padding ->
        when {
            state.support != HidSupport.READY -> SupportGate(state.support, padding, viewModel::refresh, onAppSettings)
            state.connected -> RemoteContent(viewModel = viewModel, settings = settings, padding = padding)
            else -> DevicesContent(
                viewModel = viewModel,
                padding = padding,
                onFind = { scanOpen = true },
                onPairFromTv = { pairOpen = true }
            )
        }
    }

    if (settingsOpen) {
        val layoutChoice by viewModel.layoutChoice.collectAsStateWithLifecycle()
        val device = state.device.takeIf { state.connected }
        RemoteSettingsSheet(
            settings = settings,
            onChange = viewModel::update,
            onDismiss = { settingsOpen = false },
            layoutChoice = layoutChoice.takeIf { device != null },
            onLayout = { choice -> device?.let { viewModel.setLayout(it, choice) } }
        )
    }
    if (scanOpen) {
        ScanSheet(
            viewModel = viewModel,
            onConnect = { device ->
                scanOpen = false
                viewModel.connect(device)
            },
            onDismiss = {
                viewModel.stopScan()
                scanOpen = false
            }
        )
    }
    if (moreOpen && state.connected) MoreKeysSheet(viewModel = viewModel, onDismiss = { moreOpen = false })
    if (pairOpen) PairFromTvSheet(phoneName = viewModel.localName(), onDismiss = { pairOpen = false })
}

/** Explains what is missing and offers the one step that fixes it. */
@Composable
private fun SupportGate(support: HidSupport, padding: PaddingValues, onRefresh: () -> Unit, onAppSettings: () -> Unit) {
    var asked by rememberSaveable { mutableStateOf(false) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        onRefresh()
    }
    val enable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onRefresh() }
    Box(modifier = Modifier.padding(padding)) {
        when (support) {
            HidSupport.UNSUPPORTED -> EmptyState(
                icon = Icons.Rounded.TvOff,
                title = stringResource(R.string.remote_unsupported_title),
                message = stringResource(R.string.remote_unsupported_message)
            )
            HidSupport.NEEDS_PERMISSION -> EmptyState(
                icon = Icons.Rounded.Bluetooth,
                title = stringResource(R.string.remote_permission_title),
                message = stringResource(R.string.remote_permission_message),
                // Once Android stops showing the prompt, the only way left is App info.
                actionLabel = stringResource(if (asked) R.string.remote_permission_settings else R.string.remote_permission_action),
                onAction = { if (asked) onAppSettings() else permissions.launch(NEARBY_PERMISSIONS) }
            )
            HidSupport.BLUETOOTH_OFF -> EmptyState(
                icon = Icons.Rounded.BluetoothDisabled,
                title = stringResource(R.string.remote_bluetooth_off_title),
                message = stringResource(R.string.remote_bluetooth_off_message),
                actionLabel = stringResource(R.string.remote_bluetooth_off_action),
                onAction = { runCatching { enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) } }
            )
            HidSupport.READY -> Unit
        }
    }
}

@Composable
private fun DevicesContent(viewModel: RemoteViewModel, padding: PaddingValues, onFind: () -> Unit, onPairFromTv: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val paired by viewModel.paired.collectAsStateWithLifecycle()
    val lastId by viewModel.lastId.collectAsStateWithLifecycle()
    val layouts by viewModel.deviceLayouts.collectAsStateWithLifecycle()
    val connectingId = state.device?.id.takeIf { state.connection == Connection.CONNECTING }

    LazyColumn(contentPadding = padding) {
        item(key = "paired-header") { SectionHeader(stringResource(R.string.remote_paired_header)) }
        item(key = "paired") {
            ConnectedGroup {
                when {
                    !state.registered -> ConnectedRow(
                        title = stringResource(R.string.remote_getting_ready),
                        supporting = stringResource(R.string.remote_getting_ready_summary),
                        leading = { LoadingIndicator(modifier = Modifier.size(40.dp)) }
                    )
                    paired.isEmpty() -> ConnectedRow(
                        title = stringResource(R.string.remote_no_paired),
                        supporting = stringResource(R.string.remote_no_paired_summary),
                        leading = { IconBadge(icon = Icons.Rounded.Bluetooth, key = "remote-none") }
                    )
                    else -> paired.forEach { device ->
                        val saved = layouts[device.id]
                        val status = when (device.id) {
                            connectingId -> stringResource(R.string.remote_connecting)
                            lastId -> stringResource(R.string.remote_last_used)
                            else -> stringResource(device.kind.label)
                        }
                        DeviceRow(
                            device = device,
                            // A layout picked for this device shows next to its status.
                            supporting = if (saved != null) stringResource(R.string.remote_connected_layout, status, stringResource(layoutFor(saved, device.kind).label)) else status,
                            connecting = device.id == connectingId,
                            onClick = { viewModel.connect(device) },
                            layoutChoice = saved,
                            onLayout = { choice -> viewModel.setLayout(device, choice) }
                        )
                    }
                }
            }
        }
        item(key = "add-header") { SectionHeader(stringResource(R.string.remote_add_header)) }
        item(key = "add") {
            ConnectedGroup {
                NavigationRow(
                    title = stringResource(R.string.remote_find),
                    supporting = stringResource(R.string.remote_find_summary),
                    icon = Icons.Rounded.Search,
                    badgeKey = "remote-find",
                    onClick = onFind
                )
                NavigationRow(
                    title = stringResource(R.string.remote_pair_from_tv),
                    supporting = stringResource(R.string.remote_pair_from_tv_summary),
                    icon = Icons.Rounded.Tv,
                    badgeKey = "remote-pair-tv",
                    onClick = onPairFromTv
                )
            }
        }
        item(key = "footnote") {
            Text(
                text = stringResource(R.string.remote_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

/** A paired or found device: its kind's icon and name. Its Bluetooth address is never shown. */
@Composable
internal fun DeviceRow(
    device: RemoteDevice,
    supporting: String,
    connecting: Boolean,
    onClick: () -> Unit,
    /** This device's saved layout; with [onLayout], a long-press picks another. */
    layoutChoice: LayoutChoice? = null,
    onLayout: ((LayoutChoice) -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        ConnectedRow(
            title = device.name.ifBlank { stringResource(R.string.remote_unnamed) },
            supporting = supporting,
            leading = { IconBadge(icon = device.kind.icon, key = "remote-${device.kind.name}") },
            trailing = {
                if (connecting) {
                    LoadingIndicator(modifier = Modifier.size(32.dp))
                } else {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            onClick = onClick,
            onLongClick = if (onLayout != null) ({ menuOpen = true }) else null,
            enabled = !connecting
        )
        if (onLayout != null) {
            val detected = stringResource(layoutFor(LayoutChoice.AUTO, device.kind).label)
            OsysterMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                groups = listOf(
                    LayoutChoice.entries.map { option ->
                        val label = if (option == LayoutChoice.AUTO) stringResource(R.string.remote_layout_auto_detected, detected) else stringResource(option.label)
                        MenuAction(label, selected = option == (layoutChoice ?: LayoutChoice.AUTO)) { onLayout(option) }
                    }
                )
            )
        }
    }
}

internal val DeviceKind.icon: ImageVector
    get() = when (this) {
        DeviceKind.TV -> Icons.Rounded.Tv
        DeviceKind.COMPUTER -> Icons.Rounded.Computer
        DeviceKind.PHONE -> Icons.Rounded.Smartphone
        DeviceKind.OTHER -> Icons.Rounded.Bluetooth
    }

internal val DeviceKind.label: Int
    get() = when (this) {
        DeviceKind.TV -> R.string.remote_kind_tv
        DeviceKind.COMPUTER -> R.string.remote_kind_computer
        DeviceKind.PHONE -> R.string.remote_kind_phone
        DeviceKind.OTHER -> R.string.remote_kind_other
    }

