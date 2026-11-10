package dev.qtremors.osyster.feature.notificationhistory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.notifications.domain.HistoryEntry
import dev.qtremors.osyster.core.notifications.domain.HistorySettings
import dev.qtremors.osyster.core.notifications.domain.RemovalReason
import dev.qtremors.osyster.core.notifications.domain.Retention
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow

/** All of one entry: its full text, the chat it came from, what happened to it, and actions. */
@Composable
internal fun EntrySheet(entry: HistoryEntry, viewModel: NotificationHistoryViewModel, onOpenApp: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val thread by viewModel.thread.collectAsStateWithLifecycle()
    val you = stringResource(R.string.history_you)
    OsysterBottomSheet(onDismissRequest = onDismiss, title = entry.appLabel) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                AppIcon(entry.packageName, size = 32.dp)
                Column {
                    Text(fullTime(context, entry.postedAt), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    removalLine(context, entry)?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary) }
                }
            }
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    entry.title?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
                    if (entry.conversation == null) {
                        entry.text?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    }
                    entry.subText?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    entry.details?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            rememberPicture(entry.picturePath, PICTURE_PX)?.let { picture ->
                Image(
                    bitmap = picture,
                    contentDescription = stringResource(R.string.history_picture),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp))
                )
            }
            entry.progress?.let { percent ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.history_progress, percent), style = MaterialTheme.typography.labelLarge)
                    LinearWavyProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (entry.replies.isNotEmpty()) {
                SectionHeader(stringResource(R.string.history_replies))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                    entry.replies.forEach { reply ->
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp), modifier = Modifier.widthIn(max = 300.dp)) {
                                SelectionContainer { Text(reply, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) }
                            }
                        }
                    }
                }
            }
            if (entry.actions.isNotEmpty()) {
                SectionHeader(stringResource(R.string.history_buttons))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    entry.actions.forEach { label ->
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = CircleShape) {
                            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }
            }
            if (entry.conversation != null && thread.isNotEmpty()) {
                SectionHeader(stringResource(R.string.history_conversation))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                    thread.takeLast(MAX_THREAD).forEach { message ->
                        MessageBubble(message, mine = message.sender == null, highlighted = message.id == entry.id, you = you)
                    }
                }
            }
            entry.channelId?.let {
                Text(stringResource(R.string.history_channel, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
            ConnectedGroup {
                ConnectedRow(
                    title = stringResource(R.string.history_open_app, entry.appLabel),
                    leading = { IconBadge(Icons.AutoMirrored.Rounded.OpenInNew, key = "history-open") },
                    onClick = onOpenApp
                )
                ConnectedRow(
                    title = stringResource(R.string.history_copy),
                    leading = { IconBadge(Icons.Rounded.ContentCopy, key = "history-copy") },
                    onClick = {
                        copy(context, (listOfNotNull(entry.title, entry.text, entry.subText, entry.details) + entry.replies).joinToString("\n"))
                        Toast.makeText(context, R.string.history_copied, Toast.LENGTH_SHORT).show()
                    }
                )
                ConnectedRow(
                    title = stringResource(R.string.history_only_app, entry.appLabel),
                    leading = { IconBadge(Icons.Rounded.FilterList, key = "history-filter") },
                    onClick = {
                        viewModel.filter(entry.packageName)
                        onDismiss()
                    }
                )
                ConnectedRow(
                    title = stringResource(R.string.history_exclude, entry.appLabel),
                    leading = { IconBadge(Icons.Rounded.Block, key = "history-exclude") },
                    onClick = {
                        viewModel.exclude(entry.packageName)
                        Toast.makeText(context, context.getString(R.string.history_excluded, entry.appLabel), Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
                ConnectedRow(
                    title = stringResource(R.string.history_delete),
                    leading = { IconBadge(Icons.Rounded.Delete, key = "history-delete") },
                    onClick = { viewModel.delete(entry) }
                )
            }
        }
    }
}

/** One chat message; the person's own are on the right, the one opened is outlined in color. */
@Composable
private fun MessageBubble(message: HistoryEntry, mine: Boolean, highlighted: Boolean, you: String) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = when {
                highlighted -> colors.primaryContainer
                mine -> colors.secondaryContainer
                else -> colors.surfaceContainerHighest
            },
            shape = RoundedCornerShape(20.dp, 20.dp, if (mine) 6.dp else 20.dp, if (mine) 20.dp else 6.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(message.sender ?: you, style = MaterialTheme.typography.labelMedium, color = colors.primary)
                SelectionContainer { Text(message.text.orEmpty(), style = MaterialTheme.typography.bodyMedium) }
                Text(
                    DateFormat.getTimeFormat(context).format(java.util.Date(message.postedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/** On or off, ongoing notifications, how long to keep, apps left out, and clearing everything. */
@Composable
internal fun HistorySettingsSheet(settings: HistorySettings, viewModel: NotificationHistoryViewModel, onDismiss: () -> Unit) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var clearing by rememberSaveable { mutableStateOf(false) }
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.history_settings)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            ConnectedGroup {
                ToggleRow(
                    title = stringResource(R.string.history_enabled),
                    supporting = stringResource(R.string.history_enabled_detail),
                    checked = settings.enabled,
                    onCheckedChange = { on -> viewModel.update { it.copy(enabled = on) } },
                    icon = Icons.Rounded.History
                )
                ToggleRow(
                    title = stringResource(R.string.history_ongoing),
                    supporting = stringResource(R.string.history_ongoing_detail),
                    checked = settings.keepOngoing,
                    onCheckedChange = { on -> viewModel.update { it.copy(keepOngoing = on) } }
                )
                ChoiceRow(
                    title = stringResource(R.string.history_retention),
                    supporting = stringResource(R.string.history_retention_detail),
                    options = Retention.entries,
                    selected = settings.retention,
                    label = { stringResource(it.label) },
                    onSelect = { retention -> viewModel.update { it.copy(retention = retention) } }
                )
            }

            SectionHeader(stringResource(R.string.history_excluded_title))
            if (settings.excluded.isEmpty()) {
                Text(
                    stringResource(R.string.history_excluded_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                ConnectedGroup {
                    settings.excluded.sorted().forEach { packageName ->
                        val label = apps.firstOrNull { it.packageName == packageName }?.label ?: packageName
                        ConnectedRow(
                            title = label,
                            supporting = stringResource(R.string.history_include_again, label),
                            leading = { AppIcon(packageName) },
                            trailing = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                            onClick = { viewModel.include(packageName) }
                        )
                    }
                }
            }

            if (apps.isNotEmpty()) {
                SectionHeader(stringResource(R.string.history_clear))
                ConnectedGroup {
                    apps.take(MAX_APPS).forEach { app ->
                        ConnectedRow(
                            title = stringResource(R.string.history_delete_app, app.label),
                            supporting = app.count.toString(),
                            leading = { AppIcon(app.packageName) },
                            trailing = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                            onClick = { viewModel.deleteApp(app.packageName) }
                        )
                    }
                }
                Button(
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    onClick = { clearing = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                    Text(stringResource(R.string.history_clear), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    if (clearing) {
        AlertDialog(
            onDismissRequest = { clearing = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.history_clear_confirm)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                Button(shapes = ButtonDefaults.shapes(), onClick = {
                    viewModel.clear()
                    clearing = false
                }) { Text(stringResource(R.string.history_clear)) }
            },
            dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = { clearing = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }
}

private val Retention.label: Int
    get() = when (this) {
        Retention.WEEK -> R.string.history_retention_week
        Retention.MONTH -> R.string.history_retention_month
        Retention.QUARTER -> R.string.history_retention_quarter
        Retention.YEAR -> R.string.history_retention_year
    }

private fun fullTime(context: Context, millis: Long): String =
    DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL)

private fun removalLine(context: Context, entry: HistoryEntry): String? {
    val reason = entry.removal ?: return null
    val at = entry.removedAt ?: return null
    val label = context.getString(
        when (reason) {
            RemovalReason.DISMISSED -> R.string.history_removed_dismissed
            RemovalReason.OPENED -> R.string.history_removed_opened
            RemovalReason.APP_REMOVED -> R.string.history_removed_app
            RemovalReason.OTHER -> R.string.history_removed_other
        }
    )
    return context.getString(R.string.history_removed_at, label, DateFormat.getTimeFormat(context).format(java.util.Date(at)))
}

private fun copy(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText(null, text))
}

private const val MAX_THREAD = 100
private const val PICTURE_PX = 1080
private const val MAX_APPS = 20
