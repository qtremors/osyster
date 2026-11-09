package dev.qtremors.osyster.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.ui.R

/**
 * Shown instead of a usage screen until the user grants Usage access. The button opens Android's
 * Usage access settings; the screen re-checks when the user comes back.
 */
@Composable
fun UsageAccessGate(message: String, onGrant: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Rounded.QueryStats,
        title = stringResource(R.string.usage_access_title),
        message = message,
        actionLabel = stringResource(R.string.usage_access_action),
        onAction = onGrant,
        modifier = modifier
    )
}
