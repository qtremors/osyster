package dev.qtremors.osyster.feature.barcode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.barcode.domain.CodeStyle
import dev.qtremors.osyster.core.barcode.domain.CreateKind
import dev.qtremors.osyster.core.barcode.domain.Recovery
import dev.qtremors.osyster.core.barcode.domain.luminance
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics

/** Colors, shape, border, recovery, and size for a created code, applied to the preview live. */
@Composable
internal fun CreateStyleSheet(
    kind: CreateKind,
    style: CodeStyle,
    size: ExportSize,
    onStyle: ((CodeStyle) -> CodeStyle) -> Unit,
    onSize: (ExportSize) -> Unit,
    onDismiss: () -> Unit
) {
    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.create_style)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            SectionHeader(stringResource(R.string.create_style_foreground))
            Swatches(FOREGROUNDS, style.foreground) { color -> onStyle { it.copy(foreground = color) } }
            SectionHeader(stringResource(R.string.create_style_background))
            Swatches(BACKGROUNDS, style.background) { color -> onStyle { it.copy(background = color) } }

            SectionHeader(stringResource(R.string.create_style_margin))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.create_style_margin_value, style.margin), style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = style.margin.toFloat(),
                    onValueChange = { value -> onStyle { it.copy(margin = value.toInt()) } },
                    valueRange = 0f..MAX_MARGIN.toFloat(),
                    steps = MAX_MARGIN - 1
                )
                Text(stringResource(R.string.create_style_margin_detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Round dots suit QR codes only: other 2D codes are found by solid edges that dots
            // would break, so they get no look options.
            if (kind.qr || !kind.format.twoD) SectionHeader(stringResource(R.string.create_style_look))
            if (kind.qr || !kind.format.twoD) ConnectedGroup {
                if (kind.qr) {
                    ToggleRow(
                        title = stringResource(R.string.create_style_rounded),
                        supporting = stringResource(R.string.create_style_rounded_detail),
                        checked = style.rounded,
                        onCheckedChange = { on -> onStyle { it.copy(rounded = on) } }
                    )
                } else {
                    ToggleRow(
                        title = stringResource(R.string.create_style_text),
                        supporting = stringResource(R.string.create_style_text_detail),
                        checked = style.showText,
                        onCheckedChange = { on -> onStyle { it.copy(showText = on) } }
                    )
                }
            }

            if (kind.qr) {
                SectionHeader(stringResource(R.string.create_style_recovery))
                Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConnectedChoices(
                        options = Recovery.entries,
                        selected = style.recovery,
                        label = { stringResource(it.label) },
                        onSelect = { recovery -> onStyle { it.copy(recovery = recovery) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.create_style_recovery_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            SectionHeader(stringResource(R.string.create_style_size))
            ConnectedChoices(
                options = ExportSize.entries,
                selected = size,
                label = { stringResource(it.label) },
                onSelect = onSize,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )

            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = { onStyle { CodeStyle.defaultsFor(kind.format) } },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                Text(stringResource(R.string.create_style_reset), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** A row of color circles; the chosen one has a ring and a check. */
@Composable
private fun Swatches(colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    val haptics = rememberAppHaptics()
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        colors.forEach { color ->
            val chosen = color == selected
            val name = if (color == CodeStyle.TRANSPARENT) stringResource(R.string.create_style_transparent) else "#%06X".format(color and 0xFFFFFF)
            val description = stringResource(R.string.create_color, name)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(if (chosen) 3.dp else 1.dp, if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .semantics { contentDescription = description }
                    .selectable(selected = chosen, role = Role.RadioButton, onClick = {
                        haptics.tick()
                        onPick(color)
                    })
            ) {
                if (color == CodeStyle.TRANSPARENT) Checkers() else Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(Color(color)) }
                if (chosen) {
                    val onColor = if (color != CodeStyle.TRANSPARENT && luminance(color) < 0.4) Color.White else Color.Black
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = onColor)
                }
            }
        }
    }
}

/** The usual grey and white squares that mean see-through. */
@Composable
internal fun Checkers() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Fixed-size squares tiled over the whole area, wide barcodes included.
        val cell = 8.dp.toPx()
        for (y in 0..(size.height / cell).toInt()) for (x in 0..(size.width / cell).toInt()) {
            drawRect(
                color = if ((x + y) % 2 == 0) Color(0xFFDDDDDD) else Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(x * cell, y * cell),
                size = androidx.compose.ui.geometry.Size(cell, cell)
            )
        }
    }
}

private val FOREGROUNDS = listOf(
    CodeStyle.BLACK,
    Color(0xFF0B2A5B).toArgb(),
    Color(0xFF14532D).toArgb(),
    Color(0xFF7F1D1D).toArgb(),
    Color(0xFF4C1D95).toArgb(),
    Color(0xFF3E2723).toArgb(),
    CodeStyle.WHITE
)

private val BACKGROUNDS = listOf(
    CodeStyle.WHITE,
    Color(0xFFFFF8E1).toArgb(),
    Color(0xFFE8F5E9).toArgb(),
    Color(0xFFE3F2FD).toArgb(),
    Color(0xFFFCE4EC).toArgb(),
    CodeStyle.BLACK,
    CodeStyle.TRANSPARENT
)

private const val MAX_MARGIN = 16
