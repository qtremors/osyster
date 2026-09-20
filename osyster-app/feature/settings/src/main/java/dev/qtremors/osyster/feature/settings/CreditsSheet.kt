package dev.qtremors.osyster.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet

private data class Credit(val name: Int, val detail: Int, val license: Int?)

private val credits = listOf(
    Credit(R.string.credit_essentials, R.string.credit_essentials_detail, R.string.license_essentials_mit),
    Credit(R.string.credit_roboto_flex, R.string.credit_roboto_flex_detail, null),
    Credit(R.string.credit_androidx, R.string.credit_androidx_detail, null),
    Credit(R.string.credit_materialkolor, R.string.credit_materialkolor_detail, null)
)

/** Projects Osyster builds on. Tapping a credit with a bundled notice shows its full text. */
@Composable
internal fun CreditsSheet(onDismiss: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.settings_credits)) {
        Column(modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
            ConnectedGroup {
                credits.forEach { credit ->
                    ConnectedRow(
                        title = stringResource(credit.name),
                        supporting = stringResource(credit.detail),
                        onClick = credit.license?.let { { expanded = if (expanded == credit.name) null else credit.name } }
                    )
                    if (credit.license != null) {
                        AnimatedVisibility(visible = expanded == credit.name) {
                            Surface(color = MaterialTheme.colorScheme.surfaceBright) {
                                Text(
                                    text = stringResource(credit.license),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
