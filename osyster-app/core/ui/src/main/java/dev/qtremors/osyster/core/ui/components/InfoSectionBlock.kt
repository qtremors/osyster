package dev.qtremors.osyster.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.AvailabilityLabel
import dev.qtremors.osyster.core.device.domain.InfoRow
import dev.qtremors.osyster.core.device.domain.InfoSection
import dev.qtremors.osyster.core.device.domain.label
import dev.qtremors.osyster.core.ui.R
import dev.qtremors.osyster.core.ui.theme.ValueTextStyle
import dev.qtremors.osyster.core.ui.theme.spacing

/**
 * Draws one [InfoSection] as a header and a group of connected fact rows. Rows with a value can
 * be long-pressed to copy; rows without one say why, never showing a fake value.
 */
@Composable
fun InfoSectionBlock(
    section: InfoSection,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvanced by rememberSaveable(section.key) { mutableStateOf(false) }
    Column(modifier = modifier) {
        SectionHeader(section.title)
        ConnectedGroup {
            section.primaryRows.forEach { row -> FactRow(row, onCopy) }
            if (section.advancedRows.isNotEmpty()) {
                AdvancedToggle(expanded = showAdvanced, count = section.advancedRows.size) {
                    showAdvanced = !showAdvanced
                }
            }
        }
        AnimatedVisibility(visible = showAdvanced && section.advancedRows.isNotEmpty()) {
            ConnectedGroup(modifier = Modifier.padding(top = 2.dp)) {
                section.advancedRows.forEach { row -> FactRow(row, onCopy) }
            }
        }
    }
}

/** A label above its value, like Android's own About phone screen. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FactRow(
    row: InfoRow,
    onCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val haptics = rememberAppHaptics()
    val value = row.value
    val copyModifier = if (value is Availability.Available && row.copyable) {
        Modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                haptics.longPress()
                onCopy(row.label, value.value)
            },
            onLongClickLabel = stringResource(R.string.action_copy)
        )
    } else Modifier

    Surface(color = colors.surfaceBright, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .then(copyModifier)
                .padding(horizontal = spacing.rowHorizontal, vertical = spacing.rowVertical),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(row.label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            when (value) {
                is Availability.Available -> Text(value.value, style = ValueTextStyle, color = colors.onSurface)
                else -> UnavailableValue(value.label ?: AvailabilityLabel.NOT_REPORTED)
            }
        }
    }
}

/** The muted explanation shown in place of a value Android does not give us. */
@Composable
fun UnavailableValue(label: AvailabilityLabel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = label.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = availabilityText(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun availabilityText(label: AvailabilityLabel): String = stringResource(
    when (label) {
        AvailabilityLabel.PERMISSION_REQUIRED -> R.string.availability_permission_required
        AvailabilityLabel.RESTRICTED_BY_ANDROID -> R.string.availability_restricted
        AvailabilityLabel.NOT_SUPPORTED -> R.string.availability_not_supported
        AvailabilityLabel.NOT_REPORTED -> R.string.availability_not_reported
        AvailabilityLabel.TEMPORARILY_UNAVAILABLE -> R.string.availability_temporarily_unavailable
        AvailabilityLabel.COLLECTION_FAILED -> R.string.availability_failed
    }
)

/** Short form for tight spaces such as hero metrics. */
@Composable
fun availabilityShortText(label: AvailabilityLabel): String = stringResource(
    when (label) {
        AvailabilityLabel.PERMISSION_REQUIRED -> R.string.availability_short_permission
        AvailabilityLabel.RESTRICTED_BY_ANDROID -> R.string.availability_short_restricted
        AvailabilityLabel.NOT_SUPPORTED -> R.string.availability_short_unsupported
        AvailabilityLabel.NOT_REPORTED,
        AvailabilityLabel.TEMPORARILY_UNAVAILABLE,
        AvailabilityLabel.COLLECTION_FAILED -> R.string.availability_short_unknown
    }
)

private fun AvailabilityLabel.icon(): ImageVector = when (this) {
    AvailabilityLabel.PERMISSION_REQUIRED -> Icons.Rounded.Key
    AvailabilityLabel.RESTRICTED_BY_ANDROID -> Icons.Rounded.Lock
    AvailabilityLabel.NOT_SUPPORTED -> Icons.Rounded.Block
    AvailabilityLabel.TEMPORARILY_UNAVAILABLE -> Icons.Rounded.Schedule
    AvailabilityLabel.NOT_REPORTED,
    AvailabilityLabel.COLLECTION_FAILED -> Icons.Rounded.Info
}

@Composable
private fun AdvancedToggle(expanded: Boolean, count: Int, onToggle: () -> Unit) {
    ConnectedRow(
        title = stringResource(if (expanded) R.string.action_hide_advanced else R.string.action_show_advanced),
        supporting = pluralStringResource(R.plurals.advanced_count, count, count),
        trailing = {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = onToggle
    )
}
