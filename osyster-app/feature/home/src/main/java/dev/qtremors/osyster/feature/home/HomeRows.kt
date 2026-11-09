package dev.qtremors.osyster.feature.home

import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.PinKey
import dev.qtremors.osyster.core.ui.catalog.DestinationLabel
import dev.qtremors.osyster.core.ui.R as UiR
import dev.qtremors.osyster.core.ui.catalog.help
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.HelpIcon
import dev.qtremors.osyster.core.ui.components.HelpSheet
import dev.qtremors.osyster.core.ui.components.NavigationRow

/** A device page row. Long-press opens pin, move, hide, and About this page. */
@Composable
internal fun PageRow(
    page: DevicePage,
    pinned: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    glance: String?,
    actions: HomeActions
) {
    var menuOpen by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    val label = page.label
    Box {
        NavigationRow(
            title = stringResource(label.title),
            supporting = glance ?: stringResource(label.summary),
            icon = label.icon,
            badgeKey = page.id,
            onClick = { actions.onOpenPage(page) },
            onLongClick = { menuOpen = true }
        )
        OsysterMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            groups = listOf(
                listOfNotNull(
                    MenuAction(
                        stringResource(if (pinned) R.string.home_unpin else R.string.home_pin),
                        if (pinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin
                    ) { actions.onTogglePin(PinKey.page(page.id)) },
                    if (canMoveUp) MenuAction(stringResource(R.string.home_move_up), Icons.Rounded.ArrowUpward) { actions.onMove(page, true) } else null,
                    if (canMoveDown) MenuAction(stringResource(R.string.home_move_down), Icons.Rounded.ArrowDownward) { actions.onMove(page, false) } else null
                ),
                listOf(MenuAction(stringResource(R.string.home_hide), Icons.Rounded.VisibilityOff) { actions.onHide(page) }),
                listOf(MenuAction(stringResource(UiR.string.help_about_page), HelpIcon) { helpOpen = true })
            )
        )
    }
    if (helpOpen) HelpSheet(label = label, help = page.help, badgeKey = page.id, onDismiss = { helpOpen = false })
}

/** A pinned shortcut. Long-press offers Unpin and About this page. */
@Composable
internal fun PinnedRow(
    label: DestinationLabel,
    @StringRes help: Int,
    glance: String? = null,
    badgeKey: String,
    onOpen: () -> Unit,
    onUnpin: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    Box {
        NavigationRow(
            title = stringResource(label.title),
            supporting = glance ?: stringResource(label.summary),
            icon = label.icon,
            badgeKey = badgeKey,
            onClick = onOpen,
            onLongClick = { menuOpen = true }
        )
        OsysterMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            groups = listOf(
                listOf(MenuAction(stringResource(R.string.home_unpin), Icons.Outlined.PushPin, onClick = onUnpin)),
                listOf(MenuAction(stringResource(UiR.string.help_about_page), HelpIcon) { helpOpen = true })
            )
        )
    }
    if (helpOpen) HelpSheet(label = label, help = help, badgeKey = badgeKey, onDismiss = { helpOpen = false })
}

