package dev.qtremors.osyster.feature.btremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.hid.domain.PcAction
import dev.qtremors.osyster.core.hid.domain.RemoteKey
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.SectionHeader

/**
 * Keys beyond the remote: computer actions (what PC Remote Control did with a server on the PC,
 * here with nothing installed), app keys, and media and brightness keys.
 */
@Composable
internal fun MoreKeysSheet(viewModel: RemoteViewModel, onDismiss: () -> Unit) {
    var confirming by remember { mutableStateOf<PcAction?>(null) }
    val keys = KeyActions(onPress = viewModel::hold, onRelease = viewModel::release)

    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.remote_more_keys)) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            SectionHeader(stringResource(R.string.remote_more_computer))
            ConnectedGroup {
                PcAction.entries.forEach { action ->
                    val (icon, label) = action.display
                    ConnectedRow(
                        title = stringResource(label),
                        supporting = if (action.windowsOnly) stringResource(R.string.remote_more_windows) else null,
                        leading = { IconBadge(icon = icon, key = "pc-${action.name}") },
                        trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { if (action.confirm) confirming = action else viewModel.run(action) }
                    )
                }
            }
            SectionHeader(stringResource(R.string.remote_more_apps))
            KeyTiles(
                keys,
                listOf(
                    Triple(RemoteKey.BROWSER, Icons.Rounded.Language, R.string.remote_key_browser),
                    Triple(RemoteKey.CALCULATOR, Icons.Rounded.Calculate, R.string.remote_key_calculator),
                    Triple(RemoteKey.FILES, Icons.Rounded.Folder, R.string.remote_key_files),
                    Triple(RemoteKey.EMAIL, Icons.Rounded.Email, R.string.remote_key_email),
                    Triple(RemoteKey.SEARCH, Icons.Rounded.Search, R.string.remote_key_search)
                )
            )
            SectionHeader(stringResource(R.string.remote_more_media))
            KeyTiles(
                keys,
                listOf(
                    Triple(RemoteKey.PLAY, Icons.Rounded.PlayArrow, R.string.remote_key_play),
                    Triple(RemoteKey.PAUSE, Icons.Rounded.Pause, R.string.remote_key_pause),
                    Triple(RemoteKey.STOP, Icons.Rounded.Stop, R.string.remote_key_stop),
                    Triple(RemoteKey.REPEAT, Icons.Rounded.Repeat, R.string.remote_key_repeat),
                    Triple(RemoteKey.CAPTIONS, Icons.Rounded.ClosedCaption, R.string.remote_captions),
                    Triple(RemoteKey.BRIGHTNESS_DOWN, Icons.Rounded.BrightnessLow, R.string.remote_key_brightness_down),
                    Triple(RemoteKey.BRIGHTNESS_UP, Icons.Rounded.BrightnessHigh, R.string.remote_key_brightness_up)
                )
            )
        }
    }

    confirming?.let { action ->
        val label = stringResource(action.display.second)
        AlertDialog(
            onDismissRequest = { confirming = null },
            icon = { Icon(action.display.first, contentDescription = null) },
            title = { Text(stringResource(R.string.remote_more_confirm_title, label)) },
            text = { Text(stringResource(if (action.windowsOnly) R.string.remote_more_confirm_windows else R.string.remote_more_confirm_body)) },
            confirmButton = {
                Button(shapes = ButtonDefaults.shapes(), onClick = {
                    viewModel.run(action)
                    confirming = null
                }) { Text(label) }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirming = null }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }
}

/** Keys as labeled tiles, four to a row. */
@Composable
private fun KeyTiles(keys: KeyActions, items: List<Triple<RemoteKey, ImageVector, Int>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (key, icon, label) ->
                    val text = stringResource(label)
                    PressKey(text, { keys.onPress(key) }, keys.onRelease, Modifier.weight(1f).height(72.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                            Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }
                    }
                }
                repeat(4 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val PcAction.display: Pair<ImageVector, Int>
    get() = when (this) {
        PcAction.LOCK -> Icons.Rounded.Lock to R.string.pc_lock
        PcAction.SLEEP -> Icons.Rounded.Bedtime to R.string.pc_sleep
        PcAction.POWER_KEY -> Icons.Rounded.PowerSettingsNew to R.string.pc_power_key
        PcAction.SHUT_DOWN -> Icons.Rounded.PowerOff to R.string.pc_shut_down
        PcAction.RESTART -> Icons.Rounded.RestartAlt to R.string.pc_restart
        PcAction.SCREENSHOT -> Icons.Rounded.Screenshot to R.string.pc_screenshot
        PcAction.TASK_MANAGER -> Icons.Rounded.ViewList to R.string.pc_task_manager
        PcAction.SHOW_DESKTOP -> Icons.Rounded.DesktopWindows to R.string.pc_show_desktop
    }
