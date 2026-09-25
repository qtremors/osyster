package dev.qtremors.osyster.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.R
import dev.qtremors.osyster.core.ui.catalog.DestinationLabel

/**
 * "About this page": what a page or tool shows, where its numbers come from, and its limits.
 * Opened from a row's long-press menu, never automatically.
 */
@Composable
fun HelpSheet(
    label: DestinationLabel,
    @StringRes help: Int,
    badgeKey: String,
    onDismiss: () -> Unit,
    needs: String? = null
) {
    OsysterBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBadge(label.icon, key = badgeKey, size = 48.dp, iconSize = 26.dp)
                Column {
                    Text(stringResource(label.title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(label.summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(stringResource(help), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.help_needs) + ": " + (needs ?: stringResource(R.string.help_needs_nothing)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
            }
        }
    }
}

/** The menu icon used for "About this page" entries. */
val HelpIcon = Icons.AutoMirrored.Rounded.HelpOutline
