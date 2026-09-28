package dev.qtremors.osyster.feature.home

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
    val onVisible: (DevicePage, Boolean) -> Unit
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.home_edit_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
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
