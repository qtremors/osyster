package dev.qtremors.osyster.feature.barcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.barcode.domain.ScannerSettings
import dev.qtremors.osyster.core.barcode.domain.SearchSite
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import dev.qtremors.osyster.core.ui.theme.FieldShape

@Composable
internal fun ScannerSettingsSheet(
    settings: ScannerSettings,
    sites: List<SearchSite>,
    onChange: ((ScannerSettings) -> ScannerSettings) -> Unit,
    onAddSite: (String, String) -> Unit,
    onDeleteSite: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.scan_settings)) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            ConnectedGroup {
                ToggleRow(stringResource(R.string.scan_settings_beep), settings.beep, { on -> onChange { it.copy(beep = on) } })
                ToggleRow(stringResource(R.string.scan_settings_vibrate), settings.vibrate, { on -> onChange { it.copy(vibrate = on) } })
            }
            SectionHeader(stringResource(R.string.scan_settings_sites))
            Text(
                stringResource(R.string.scan_settings_sites_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            ConnectedGroup {
                sites.forEach { site ->
                    ConnectedRow(
                        title = site.name,
                        supporting = site.template,
                        leading = { IconBadge(icon = Icons.Rounded.Language, key = "site-${site.name}") },
                        trailing = {
                            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { onDeleteSite(site.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.scan_settings_site_delete, site.name))
                            }
                        }
                    )
                }
                ConnectedRow(
                    title = stringResource(R.string.scan_settings_site_add),
                    leading = { IconBadge(icon = Icons.Rounded.Add, key = "site-add") },
                    onClick = { adding = true }
                )
            }
        }
    }
    if (adding) {
        AddSiteDialog(onAdd = { name, template ->
            onAddSite(name, template)
            adding = false
        }, onDismiss = { adding = false })
    }
}

@Composable
private fun AddSiteDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var template by rememberSaveable { mutableStateOf("") }
    val valid = name.isNotBlank() && SearchSite.isValidTemplate(template)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Language, contentDescription = null) },
        title = { Text(stringResource(R.string.scan_settings_site_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    shape = FieldShape,
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.scan_settings_site_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    shape = FieldShape,
                    value = template,
                    onValueChange = { template = it.trim() },
                    label = { Text(stringResource(R.string.scan_settings_site_template)) },
                    placeholder = { Text(stringResource(R.string.scan_settings_site_example)) },
                    supportingText = {
                        if (template.isNotEmpty() && !SearchSite.isValidTemplate(template)) Text(stringResource(R.string.scan_settings_site_invalid))
                    },
                    isError = template.isNotEmpty() && !SearchSite.isValidTemplate(template),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(shapes = ButtonDefaults.shapes(), enabled = valid, onClick = { onAdd(name, template) }) { Text(stringResource(R.string.scan_settings_add)) }
        },
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}
