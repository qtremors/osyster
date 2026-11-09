package dev.qtremors.osyster.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet

/** Shown once after an update: the highlights of this version, in plain words. */
@Composable
internal fun WhatsNewSheet(version: String, onDismiss: () -> Unit) {
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.whats_new_title, version)) {
        ConnectedGroup {
            stringArrayResource(R.array.whats_new_items).forEach { item ->
                ConnectedRow(title = item.substringBefore(": "), supporting = item.substringAfter(": ", ""))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = onDismiss) { Text(stringResource(R.string.whats_new_ok)) }
        }
    }
}
