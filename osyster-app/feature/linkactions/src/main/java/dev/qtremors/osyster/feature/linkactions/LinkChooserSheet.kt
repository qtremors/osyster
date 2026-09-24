package dev.qtremors.osyster.feature.linkactions

import android.content.ClipData
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.system.domain.LinkHandler
import dev.qtremors.osyster.core.system.domain.LinkHandlers
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import kotlinx.coroutines.launch

/**
 * Lists every app that can open [url], apps made for that link first, then browsers. Also offers
 * copy and share, so a link can go anywhere without opening it.
 */
@Composable
internal fun LinkChooserSheet(
    url: String,
    links: LinkHandlers,
    onShare: (String) -> Unit,
    onDone: () -> Unit
) {
    var handlers by remember(url) { mutableStateOf<List<LinkHandler>?>(null) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(url) { handlers = links.handlersFor(url) }

    OsysterBottomSheet(onDismissRequest = onDone, title = stringResource(R.string.link_open_with)) {
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
        )
        val current = handlers
        when {
            current == null -> LoadingState()
            current.isEmpty() -> ConnectedGroup { ConnectedRow(title = stringResource(R.string.link_no_apps)) }
            else -> LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(current, key = { it.packageName + "/" + it.className }) { handler ->
                    ConnectedGroup(modifier = Modifier.padding(bottom = 2.dp)) {
                        ConnectedRow(
                            title = handler.label,
                            supporting = handler.packageName,
                            leading = { AppIcon(handler.packageName) },
                            onClick = {
                                links.open(url, handler)
                                onDone()
                            }
                        )
                    }
                }
            }
        }
        ConnectedGroup(modifier = Modifier.padding(top = 12.dp)) {
            ConnectedRow(
                title = stringResource(R.string.link_copy),
                leading = { IconBadge(Icons.Rounded.ContentCopy, key = "link_copy") },
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("link", url)))
                        onDone()
                    }
                }
            )
            ConnectedRow(
                title = stringResource(R.string.link_share),
                leading = { IconBadge(Icons.Rounded.Share, key = "link_share") },
                onClick = {
                    onShare(url)
                    onDone()
                }
            )
        }
    }
}
