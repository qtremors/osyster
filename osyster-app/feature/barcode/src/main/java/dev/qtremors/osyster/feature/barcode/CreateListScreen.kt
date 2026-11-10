package dev.qtremors.osyster.feature.barcode

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.barcode.domain.CreateKind
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader

/** Picks what to make: QR codes by what they hold first, then every other format by name. */
@Composable
internal fun CreateListScreen(onOpen: (CreateKind) -> Unit) {
    OsysterScaffold(title = stringResource(R.string.create_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "qr-header") { SectionHeader(stringResource(R.string.create_section_qr)) }
            item(key = "qr") { KindGroup(CreateKind.qrKinds, onOpen) }
            item(key = "other-header") { SectionHeader(stringResource(R.string.create_section_other)) }
            item(key = "other") { KindGroup(CreateKind.otherKinds, onOpen) }
        }
    }
}

@Composable
private fun KindGroup(kinds: List<CreateKind>, onOpen: (CreateKind) -> Unit) {
    ConnectedGroup {
        kinds.forEach { kind ->
            ConnectedRow(
                title = kind.title(),
                supporting = stringResource(kind.detail),
                leading = { IconBadge(icon = kind.icon, key = "create-${kind.name}") },
                trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { onOpen(kind) }
            )
        }
    }
}
