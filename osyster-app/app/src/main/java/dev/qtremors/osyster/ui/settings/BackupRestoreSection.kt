package dev.qtremors.osyster.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.R
import dev.qtremors.osyster.settings.PreferencesBackupManager
import dev.qtremors.osyster.settings.PreferencesBackupPreview
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreSection(
    backupManager: PreferencesBackupManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isBusy by remember { mutableStateOf(false) }
    var restorePreview by remember { mutableStateOf<PreferencesBackupPreview?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isBusy = true
        coroutineScope.launch {
            backupManager.exportTo(uri).fold(
                onSuccess = {
                    isBusy = false
                    Toast.makeText(context, R.string.backup_exported_success, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    isBusy = false
                    Toast.makeText(context, context.getString(R.string.backup_failed, error.message), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isBusy = true
        coroutineScope.launch {
            backupManager.preview(uri).fold(
                onSuccess = { preview ->
                    isBusy = false
                    pendingRestoreUri = uri
                    restorePreview = preview
                },
                onFailure = { error ->
                    isBusy = false
                    Toast.makeText(context, context.getString(R.string.backup_failed, error.message), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    SettingsSection(
        title = stringResource(R.string.section_backup_restore),
        modifier = modifier
    ) {
        SettingsActionRow(
            title = stringResource(R.string.export_backup),
            description = stringResource(R.string.export_backup_description),
            leadingIcon = Icons.Default.FileUpload,
            onClick = {
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                exportLauncher.launch("osyster-settings-backup-$timestamp.json")
            },
            index = 0,
            count = 2,
            enabled = !isBusy
        )

        SettingsActionRow(
            title = stringResource(R.string.restore_backup),
            description = stringResource(R.string.restore_backup_description),
            leadingIcon = Icons.Default.SettingsBackupRestore,
            onClick = {
                restoreLauncher.launch(arrayOf("application/json", "*/*"))
            },
            index = 1,
            count = 2,
            enabled = !isBusy
        )
    }

    val preview = restorePreview
    val restoreUri = pendingRestoreUri
    if (preview != null && restoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                restorePreview = null
                pendingRestoreUri = null
            },
            title = {
                Text(
                    text = stringResource(R.string.restore_backup_confirmation_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.restore_backup_confirmation_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.backup_preview_items_header),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    preview.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uriToRestore = restoreUri
                        restorePreview = null
                        pendingRestoreUri = null
                        isBusy = true
                        coroutineScope.launch {
                            backupManager.restoreFrom(uriToRestore).fold(
                                onSuccess = {
                                    isBusy = false
                                    Toast.makeText(context, R.string.backup_restored_success, Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    isBusy = false
                                    Toast.makeText(context, context.getString(R.string.backup_failed, error.message), Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm_restore))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        restorePreview = null
                        pendingRestoreUri = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
