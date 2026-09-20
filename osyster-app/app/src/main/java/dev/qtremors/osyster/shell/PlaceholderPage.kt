package dev.qtremors.osyster.shell

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.ui.R as UiR
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold

/**
 * Stands in for device pages and tools until their feature modules land (v0.2.2 onward). It
 * keeps every row on Home and Tools tappable, so navigation and the toolbar can be tried now.
 */
@Composable
internal fun PlaceholderPage(title: String) {
    OsysterScaffold(title = title) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Construction,
                    title = stringResource(UiR.string.not_built_title),
                    message = stringResource(UiR.string.not_built_message)
                )
            }
        }
    }
}
