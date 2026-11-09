package dev.qtremors.osyster.feature.quicktiles

import androidx.compose.material3.ButtonDefaults
import dev.qtremors.osyster.core.ui.components.ConnectedItem
import androidx.compose.foundation.lazy.itemsIndexed
import android.app.StatusBarManager
import android.content.ComponentName
import dev.qtremors.osyster.core.ui.R as UiR
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.QuickTilesRoute
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

/**
 * Every tile Osyster offers. Tiles live in their tools' modules, so they are named here by class
 * name rather than referenced directly; a missing class is simply skipped by Android.
 */
private data class TileEntry(val className: String, @param:StringRes val title: Int, @param:StringRes val summary: Int, @param:DrawableRes val icon: Int)

private val tiles = listOf(
    TileEntry("dev.qtremors.osyster.feature.stayawake.StayAwakeTimerTile", R.string.qt_stay_awake, R.string.qt_stay_awake_summary, R.drawable.ic_qt_stay_awake),
    TileEntry("dev.qtremors.osyster.feature.stayawake.StayAwakeChargingTile", R.string.qt_charging, R.string.qt_charging_summary, R.drawable.ic_qt_charging),
    TileEntry("dev.qtremors.osyster.feature.flashlight.FlashlightTile", R.string.qt_flashlight, R.string.qt_flashlight_summary, R.drawable.ic_qt_flashlight),
    TileEntry("dev.qtremors.osyster.feature.quicktiles.RamTile", R.string.tile_ram, R.string.qt_ram_summary, R.drawable.ic_qt_ram),
    TileEntry("dev.qtremors.osyster.feature.quicktiles.DataTodayTile", R.string.tile_data_today, R.string.qt_data_summary, R.drawable.ic_qt_data),
    TileEntry("dev.qtremors.osyster.feature.quicktiles.ColorPickerTile", R.string.qt_color, R.string.qt_color_summary, R.drawable.ic_qt_color),
    TileEntry("dev.qtremors.osyster.feature.powertools.PrivateDnsTile", R.string.qt_private_dns, R.string.qt_private_dns_summary, UiR.drawable.ic_qt_dns),
    TileEntry("dev.qtremors.osyster.feature.powertools.UsbDebuggingTile", R.string.qt_usb_debugging, R.string.qt_usb_debugging_summary, UiR.drawable.ic_qt_usb),
    TileEntry("dev.qtremors.osyster.feature.powertools.DeveloperOptionsTile", R.string.qt_developer_options, R.string.qt_developer_options_summary, UiR.drawable.ic_qt_developer),
    TileEntry("dev.qtremors.osyster.feature.powertools.BlurTile", R.string.qt_blur, R.string.qt_blur_summary, UiR.drawable.ic_qt_blur),
    TileEntry("dev.qtremors.osyster.feature.powertools.TapToWakeTile", R.string.qt_tap_to_wake, R.string.qt_tap_to_wake_summary, UiR.drawable.ic_qt_tap),
    TileEntry("dev.qtremors.osyster.feature.powertools.FreezeTile", R.string.qt_freeze, R.string.qt_freeze_summary, UiR.drawable.ic_qt_freeze),
    TileEntry("dev.qtremors.osyster.feature.powertools.RestartSystemUiTile", R.string.qt_restart_systemui, R.string.qt_restart_systemui_summary, UiR.drawable.ic_qt_restart)
)

fun NavGraphBuilder.registerQuickTilesRoute() {
    composable<QuickTilesRoute> { QuickTilesScreen() }
}

@Composable
private fun QuickTilesScreen() {
    val context = LocalContext.current
    val canRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    OsysterScaffold(title = stringResource(Tool.QUICK_TILES.label.title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "how") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    ConnectedGroup {
                        ConnectedRow(
                            title = stringResource(R.string.qt_how_title),
                            supporting = stringResource(if (canRequest) R.string.qt_how_request else R.string.qt_how_manual),
                            leading = { IconBadge(Icons.Rounded.Info, key = "qt_how") }
                        )
                    }
                }
            }
            item(key = "header") { SectionHeader(stringResource(R.string.qt_available)) }
            itemsIndexed(tiles, key = { _, it -> it.className }) { index, tile ->
                val title = stringResource(tile.title)
                ConnectedItem(index, tiles.size) {
                    ConnectedRow(
                        title = title,
                        supporting = stringResource(tile.summary),
                        leading = { Icon(painterResource(tile.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailing = {
                            if (canRequest) {
                                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { requestAdd(context, tile, title) }) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Text(stringResource(R.string.qt_add), modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Android 13+ shows its own "Add tile?" prompt; the tile is added only if the user agrees. */
private fun requestAdd(context: android.content.Context, tile: TileEntry, title: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val manager = context.getSystemService(StatusBarManager::class.java) ?: return
    runCatching {
        manager.requestAddTileService(
            ComponentName(context.packageName, tile.className),
            title,
            Icon.createWithResource(context, tile.icon),
            context.mainExecutor
        ) { }
    }
}
