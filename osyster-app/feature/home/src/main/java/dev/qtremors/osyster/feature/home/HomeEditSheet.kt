package dev.qtremors.osyster.feature.home

import dev.qtremors.osyster.core.device.domain.HeroTile
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.HomeLayout
import dev.qtremors.osyster.core.device.domain.PinKey
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.DestinationLabel
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.catalog.title
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.ExpressiveSwitch
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.ReorderableColumn
import dev.qtremors.osyster.core.ui.components.SectionHeader

/** What the edit sheet changes. Each change applies at once, so there is nothing to save. */
internal data class HomeEditActions(
    val onReorderPinned: (List<PinKey>) -> Unit,
    val onReorderGroup: (List<DevicePage>) -> Unit,
    val onVisible: (DevicePage, Boolean) -> Unit,
    val onReorderTiles: (List<HeroTile>) -> Unit,
    val onTileVisible: (HeroTile, Boolean) -> Unit
)

/**
 * Edit Home: drag pinned items and pages into order, and switch pages on or off. Pages move
 * within their own group, since Home lists them under group headers.
 */
@Composable
internal fun HomeEditSheet(layout: HomeLayout, actions: HomeEditActions, onDismiss: () -> Unit) {
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.home_edit_title)) {
        Column(
            modifier = Modifier
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.home_edit_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            SectionHeader(stringResource(R.string.home_edit_dashboard))
            ReorderableColumn(
                items = layout.tileOrder,
                key = { it.id },
                onMove = { from, to -> actions.onReorderTiles(layout.tileOrder.moved(from, to)) }
            ) { tile, handle ->
                val visible = tile !in layout.hiddenTiles
                ConnectedRow(
                    title = stringResource(tile.titleRes()),
                    leading = { IconBadge(tile.icon(), key = "tile_" + tile.id) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExpressiveSwitch(checked = visible, onCheckedChange = { actions.onTileVisible(tile, it) })
                            handle()
                        }
                    }
                )
            }
            if (layout.pinned.isNotEmpty()) {
                SectionHeader(stringResource(R.string.home_pinned))
                ReorderableColumn(
                    items = layout.pinned,
                    key = { it.raw },
                    onMove = { from, to -> actions.onReorderPinned(layout.pinned.moved(from, to)) }
                ) { key, handle ->
                    val label = key.label() ?: return@ReorderableColumn
                    EditRow(label, key.id, trailing = { handle() })
                }
            }
            layout.pageOrder.groupBy(DevicePage::group).forEach { (group, pages) ->
                SectionHeader(stringResource(group.title))
                ReorderableColumn(
                    items = pages,
                    key = { it.id },
                    onMove = { from, to -> actions.onReorderGroup(pages.moved(from, to)) }
                ) { page, handle ->
                    val visible = page !in layout.hiddenPages
                    EditRow(page.label, page.id) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExpressiveSwitch(checked = visible, onCheckedChange = { actions.onVisible(page, it) })
                            handle()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRow(label: DestinationLabel, badgeKey: String, trailing: @Composable () -> Unit) {
    ConnectedRow(
        title = stringResource(label.title),
        leading = { IconBadge(label.icon, key = badgeKey) },
        trailing = { trailing() }
    )
}

private fun PinKey.label(): DestinationLabel? = when (kind) {
    PinKey.Kind.PAGE -> DevicePage.fromId(id)?.label
    PinKey.Kind.TOOL -> Tool.fromId(id)?.label
}

private fun <T> List<T>.moved(from: Int, to: Int): List<T> =
    toMutableList().apply { add(to, removeAt(from)) }

private fun HeroTile.titleRes(): Int = when (this) {
    HeroTile.CPU -> R.string.home_metric_cpu
    HeroTile.RAM -> R.string.home_metric_ram
    HeroTile.STORAGE -> R.string.home_metric_storage
    HeroTile.BATTERY -> R.string.home_metric_battery
    HeroTile.SCREEN_TIME -> R.string.home_metric_screen
    HeroTile.DATA_TODAY -> R.string.home_metric_data
}

private fun HeroTile.icon() = when (this) {
    HeroTile.CPU -> androidx.compose.material.icons.Icons.Rounded.DeveloperBoard
    HeroTile.RAM -> androidx.compose.material.icons.Icons.Rounded.Memory
    HeroTile.STORAGE -> androidx.compose.material.icons.Icons.Rounded.Storage
    HeroTile.BATTERY -> androidx.compose.material.icons.Icons.Rounded.BatteryStd
    HeroTile.SCREEN_TIME -> androidx.compose.material.icons.Icons.Rounded.HourglassBottom
    HeroTile.DATA_TODAY -> androidx.compose.material.icons.Icons.Rounded.DataUsage
}
