package dev.qtremors.osyster.feature.appstopper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.InlineSearchField
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.ToggleRow

/** Pick installed apps to add. Apps already on the list are not offered again. */
@Composable
internal fun AppPickerSheet(
    state: PickerState,
    alreadyAdded: Set<String>,
    onIncludeSystem: (Boolean) -> Unit,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var chosen by rememberSaveable { mutableStateOf(emptySet<String>()) }
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.stopper_add)) {
        InlineSearchField(query = query, onQueryChange = { query = it }, placeholder = stringResource(R.string.stopper_search))
        ConnectedGroup(modifier = Modifier.padding(top = 8.dp)) {
            ToggleRow(
                title = stringResource(R.string.stopper_show_system),
                checked = state.includeSystem,
                onCheckedChange = onIncludeSystem
            )
        }
        val apps = state.apps
        if (apps == null) {
            LoadingState()
        } else {
            val words = query.trim().lowercase()
            val visible = apps.filter { it.packageName !in alreadyAdded }
                .filter { words.isEmpty() || words in it.label.lowercase() || words in it.packageName.lowercase() }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                items(visible, key = { it.packageName }) { app ->
                    val selected = app.packageName in chosen
                    ConnectedGroup(modifier = Modifier.padding(bottom = 2.dp)) {
                        ConnectedRow(
                            title = app.label,
                            supporting = app.packageName,
                            leading = { AppIcon(app.packageName) },
                            trailing = { Checkbox(checked = selected, onCheckedChange = null) },
                            role = Role.Checkbox,
                            onClick = { chosen = if (selected) chosen - app.packageName else chosen + app.packageName }
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = { onConfirm(chosen) }, enabled = chosen.isNotEmpty()) {
                androidx.compose.material3.Text(pluralStringResource(R.plurals.stopper_add_count, chosen.size, chosen.size))
            }
        }
    }
}
