package dev.qtremors.osyster.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    actions: HomeActions
) {
    var menuOpen by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    val label = page.label
    Box {
        NavigationRow(
            title = stringResource(label.title),
            supporting = stringResource(label.summary),
            icon = label.icon,
            badgeKey = page.id,
            onClick = { actions.onOpenPage(page) },
            onLongClick = { menuOpen = true }
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            MenuItem(
                text = stringResource(if (pinned) R.string.home_unpin else R.string.home_pin),
                icon = if (pinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin
            ) {
                menuOpen = false
                actions.onTogglePin(PinKey.page(page.id))
            }
            if (canMoveUp) {
                MenuItem(stringResource(R.string.home_move_up), Icons.Rounded.ArrowUpward) {
                    menuOpen = false
                    actions.onMove(page, true)
                }
            }
            if (canMoveDown) {
                MenuItem(stringResource(R.string.home_move_down), Icons.Rounded.ArrowDownward) {
                    menuOpen = false
                    actions.onMove(page, false)
                }
            }
            MenuItem(stringResource(R.string.home_hide), Icons.Rounded.VisibilityOff) {
                menuOpen = false
                actions.onHide(page)
            }
            MenuItem(stringResource(UiR.string.help_about_page), HelpIcon) {
                menuOpen = false
                helpOpen = true
            }
        }
    }
    if (helpOpen) HelpSheet(label = label, help = page.help, badgeKey = page.id, onDismiss = { helpOpen = false })
}

/** A pinned shortcut. Long-press offers Unpin and About this page. */
@Composable
internal fun PinnedRow(
    label: DestinationLabel,
    @StringRes help: Int,
    badgeKey: String,
    onOpen: () -> Unit,
    onUnpin: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    Box {
        NavigationRow(
            title = stringResource(label.title),
            supporting = stringResource(label.summary),
            icon = label.icon,
            badgeKey = badgeKey,
            onClick = onOpen,
            onLongClick = { menuOpen = true }
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            MenuItem(stringResource(R.string.home_unpin), Icons.Outlined.PushPin) {
                menuOpen = false
                onUnpin()
            }
            MenuItem(stringResource(UiR.string.help_about_page), HelpIcon) {
                menuOpen = false
                helpOpen = true
            }
        }
    }
    if (helpOpen) HelpSheet(label = label, help = help, badgeKey = badgeKey, onDismiss = { helpOpen = false })
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}
