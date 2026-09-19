package dev.qtremors.osyster.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.R

@Composable
fun LicensesScreen(
    onOpenLicenseDocument: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val layoutDirection = LocalLayoutDirection.current
    val effectivePadding = remember(contentPadding, layoutDirection) {
        PaddingValues(
            start = 16.dp + contentPadding.calculateStartPadding(layoutDirection),
            end = 16.dp + contentPadding.calculateEndPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = effectivePadding,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.about_action_notices),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            Text(
                text = stringResource(R.string.notices_introduction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        item {
            SettingsSection(title = stringResource(R.string.runtime_libraries_title)) {
                OsysterOpenSourceComponents.all.forEachIndexed { index, component ->
                    SettingsActionRow(
                        index = index,
                        count = OsysterOpenSourceComponents.all.size,
                        title = component.name,
                        description = "${component.purpose} • ${component.license}",
                        leadingIcon = Icons.Filled.Code,
                        trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = { uriHandler.openUri(component.sourceUrl) }
                    )
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.notices_full_document)) {
                SettingsActionRow(
                    index = 0,
                    count = 1,
                    title = stringResource(R.string.notices_full_document),
                    description = stringResource(R.string.notices_full_document_description),
                    leadingIcon = Icons.Filled.Balance,
                    trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    onClick = onOpenLicenseDocument
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
        }
    }
}
