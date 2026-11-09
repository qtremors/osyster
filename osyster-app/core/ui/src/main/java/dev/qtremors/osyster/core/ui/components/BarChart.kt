package dev.qtremors.osyster.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One bar, made of stacked segments drawn bottom-up in the order given. */
@Immutable
data class ChartBar(val segments: List<Float>, val label: String)

/**
 * A rounded bar chart for daily or hourly usage. Segments stack (mobile under Wi-Fi), the
 * selected bar is drawn in full color and the rest are muted, and [description] is read aloud in
 * place of the drawing.
 */
@Composable
fun BarChart(
    bars: List<ChartBar>,
    segmentColors: List<Color>,
    description: String,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    height: Dp = 140.dp,
    labelEvery: Int = 1
) {
    val colors = MaterialTheme.colorScheme
    val maxValue = bars.maxOfOrNull { it.segments.sum() }?.takeIf { it > 0f } ?: 1f
    val track = colors.surfaceContainerHighest
    Surface(color = colors.surfaceBright, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .semantics { contentDescription = description }
                    .then(
                        if (onSelect != null) Modifier.pointerInput(bars.size) {
                            detectTapGestures { offset ->
                                val slot = size.width / bars.size.coerceAtLeast(1)
                                onSelect((offset.x / slot).toInt().coerceIn(0, bars.lastIndex))
                            }
                        } else Modifier
                    )
            ) {
                if (bars.isEmpty()) return@Canvas
                val slot = size.width / bars.size
                val barWidth = (slot * 0.62f).coerceAtMost(28.dp.toPx())
                val radius = CornerRadius(barWidth / 2, barWidth / 2)
                bars.forEachIndexed { index, bar ->
                    val x = index * slot + (slot - barWidth) / 2
                    drawRoundRect(track, Offset(x, 0f), Size(barWidth, size.height), radius, alpha = 0.5f)
                    val muted = selectedIndex != null && index != selectedIndex
                    var bottom = size.height
                    bar.segments.forEachIndexed { segment, value ->
                        val h = (value / maxValue) * size.height
                        if (h <= 0f) return@forEachIndexed
                        val color = segmentColors.getOrElse(segment) { colors.primary }
                        drawRoundRect(color, Offset(x, bottom - h), Size(barWidth, h), radius, alpha = if (muted) 0.45f else 1f)
                        bottom -= h
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                if (labelEvery <= 1) {
                    bars.forEachIndexed { index, bar ->
                        Text(
                            text = bar.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index == selectedIndex) colors.primary else colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Sparse labels ("6 AM") get the width of the whole group they start, so they
                    // are never clipped to a single thin bar.
                    bars.indices.chunked(labelEvery).forEach { group ->
                        Text(
                            text = bars[group.first()].label,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(group.size.toFloat())
                        )
                    }
                }
            }
        }
    }
}
