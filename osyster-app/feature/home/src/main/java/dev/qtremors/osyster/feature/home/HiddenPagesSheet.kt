package dev.qtremors.osyster.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet

/** Lists hidden pages; tapping one puts it back on Home. Closes itself when none are left. */
@Composable
internal fun HiddenPagesSheet(
    hidden: Set<DevicePage>,
    onShow: (DevicePage) -> Unit,
    onDismiss: () -> Unit
) {
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.home_hidden_title)) {
        ConnectedGroup {
            DevicePage.entries.filter(hidden::contains).forEach { page ->
                val label = page.label
                ConnectedRow(
                    title = stringResource(label.title),
                    supporting = stringResource(R.string.home_tap_to_show),
                    leading = { IconBadge(icon = label.icon, key = page.id) },
                    trailing = {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = { onShow(page) }
                )
            }
        }
    }
}
