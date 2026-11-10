package dev.qtremors.osyster.feature.barcode

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.barcode.domain.CodeContent
import dev.qtremors.osyster.core.barcode.domain.ScanRecord
import dev.qtremors.osyster.core.barcode.domain.parseContent
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.theme.FieldShape

/** Past scans, newest first. Tap to open one again; long-press to rename or delete it. */
@Composable
internal fun HistoryScreen(viewModel: ScannerViewModel) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val shown by viewModel.shown.collectAsStateWithLifecycle()
    var clearing by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ScanRecord?>(null) }

    OsysterScaffold(
        title = stringResource(R.string.scan_history_title),
        actions = {
            if (!records.isNullOrEmpty()) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { clearing = true }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.scan_history_clear))
                }
            }
        }
    ) { padding ->
        val list = records
        when {
            list == null -> LoadingState()
            list.isEmpty() -> Box(modifier = Modifier.padding(padding)) {
                EmptyState(Icons.Rounded.QrCode2, stringResource(R.string.scan_history_empty_title), stringResource(R.string.scan_history_empty_message))
            }
            else -> LazyColumn(contentPadding = padding) {
                item(key = "records") {
                    ConnectedGroup {
                        list.forEach { record ->
                            HistoryRow(record, onOpen = { viewModel.show(record) }, onRename = { renaming = record }, onDelete = { viewModel.delete(record.id) })
                        }
                    }
                }
            }
        }
    }

    shown?.let { ScanResultSheet(code = it, sites = sites, onDismiss = viewModel::dismiss) }
    renaming?.let { record ->
        RenameDialog(record.name.orEmpty(), onSave = { name ->
            viewModel.rename(record.id, name)
            renaming = null
        }, onDismiss = { renaming = null })
    }
    if (clearing) {
        AlertDialog(
            onDismissRequest = { clearing = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text(stringResource(R.string.scan_history_clear_confirm)) },
            text = { Text(stringResource(R.string.scan_history_clear_body)) },
            confirmButton = {
                Button(shapes = ButtonDefaults.shapes(), onClick = {
                    viewModel.clearHistory()
                    clearing = false
                }) { Text(stringResource(R.string.scan_history_clear)) }
            },
            dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = { clearing = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }
}

@Composable
private fun HistoryRow(record: ScanRecord, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val content = remember(record) { parseContent(record.text, record.format) }
    // The same title as the result sheet: a contact's name or a network's name, not the raw
    // vCard or WIFI: text (which would show the password in the list).
    val headline = remember(content) { content.headline(context).lineSequence().first() }
    val now = System.currentTimeMillis()
    val ago = if (now - record.scannedAt < DateUtils.MINUTE_IN_MILLIS) {
        stringResource(R.string.scan_history_just_now)
    } else {
        DateUtils.getRelativeTimeSpanString(record.scannedAt, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
    Box {
        ConnectedRow(
            title = record.name ?: headline,
            supporting = stringResource(R.string.scan_format_line, stringResource(content.kindLabel), ago),
            leading = { IconBadge(icon = content.icon, key = "scan-${content::class.simpleName}") },
            onClick = onOpen,
            onLongClick = { menuOpen = true }
        )
        OsysterMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            groups = listOf(
                listOf(MenuAction(stringResource(R.string.scan_history_rename), Icons.Rounded.DriveFileRenameOutline, onClick = onRename)),
                listOf(MenuAction(stringResource(R.string.scan_history_delete), Icons.Rounded.Delete, destructive = true, onClick = onDelete))
            )
        )
    }
}

@Composable
private fun RenameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
        title = { Text(stringResource(R.string.scan_history_rename)) },
        text = {
            OutlinedTextField(shape = FieldShape, value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.scan_history_name)) }, singleLine = true)
        },
        confirmButton = { Button(shapes = ButtonDefaults.shapes(), onClick = { onSave(name) }) { Text(stringResource(R.string.scan_history_save)) } },
        dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

private val CodeContent.icon: ImageVector
    get() = when (this) {
        is CodeContent.Link -> Icons.Rounded.Link
        is CodeContent.Email -> Icons.Rounded.Email
        is CodeContent.Phone -> Icons.Rounded.Phone
        is CodeContent.Sms -> Icons.Rounded.Sms
        is CodeContent.Location -> Icons.Rounded.Place
        is CodeContent.Contact -> Icons.Rounded.Contacts
        is CodeContent.Event -> Icons.Rounded.Event
        is CodeContent.Wifi -> Icons.Rounded.Wifi
        is CodeContent.Product -> if (isbn) Icons.Rounded.Book else Icons.Rounded.ShoppingBag
        is CodeContent.Text -> Icons.Rounded.QrCode2
    }
