package dev.qtremors.osyster.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/** One colored share of a [UsageRing] or one stacked part of a [PillBarChart] bar. */
data class ChartPart(val value: Float, val color: Color)

/**
 * A ring split into colored arcs with rounded ends and small gaps, the big number in the middle.
 * Used for download and upload, or mobile and Wi-Fi, of a total.
 */
@Composable
fun UsageRing(
    parts: List<ChartPart>,
    centerValue: String,
    centerLabel: String,
    description: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 120.dp
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier = modifier.size(diameter).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = 9.dp.toPx()
            val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val total = parts.sumOf { it.value.toDouble() }.toFloat()
            val visible = parts.filter { it.value > 0f }
            if (total <= 0f || visible.isEmpty()) return@Canvas
            val radius = arcSize.width / 2
            // Round caps reach past an arc's ends, so leave room for them plus a small gap.
            val gap = if (visible.size > 1) ((stroke + 4.dp.toPx()) / (2 * PI.toFloat() * radius)) * 360f else 0f
            val available = 360f - gap * visible.size
            var start = -90f + gap / 2
            visible.forEach { part ->
                val sweep = (part.value / total * available).coerceAtLeast(MIN_SWEEP)
                drawArc(part.color, start, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                start += sweep + gap
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerValue, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(centerLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Pill-shaped bars, each stacked from its parts with a gap between them, on faint tracks. Tap a
 * bar to select it; [selected] is highlighted and the caller shows its numbers.
 */
@Composable
fun PillBarChart(
    bars: List<List<ChartPart>>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.onSurface
    val maxTotal = bars.maxOfOrNull { bar -> bar.sumOf { it.value.toDouble() } }?.toFloat()?.takeIf { it > 0f } ?: 1f
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(bars.size) {
                detectTapGestures { offset ->
                    if (bars.isNotEmpty()) onSelect((offset.x / (size.width.toFloat() / bars.size)).toInt().coerceIn(0, bars.lastIndex))
                }
            }
    ) {
        if (bars.isEmpty()) return@Canvas
        val column = size.width / bars.size
        val barWidth = (column * 0.58f).coerceIn(4.dp.toPx(), 22.dp.toPx())
        val radius = CornerRadius(barWidth / 2, barWidth / 2)
        val gap = 2.dp.toPx()
        bars.forEachIndexed { index, parts ->
            val left = index * column + (column - barWidth) / 2
            drawRoundRect(track.copy(alpha = if (index == selected) 1f else 0.55f), Offset(left, 0f), Size(barWidth, size.height), radius)
            val total = parts.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) {
                // An empty bar that is picked still gets an outline, so the tap visibly landed.
                if (index == selected) {
                    drawRoundRect(highlight, Offset(left, 0f), Size(barWidth, size.height), radius, style = Stroke(1.5.dp.toPx()))
                }
                return@forEachIndexed
            }
            // With a bar picked, the others fade back so the picked one stands out.
            val partAlpha = if (selected != null && index != selected) 0.4f else 1f
            val barHeight = (size.height * total / maxTotal).coerceAtLeast(barWidth)
            var top = size.height - barHeight
            val visible = parts.filter { it.value > 0f }
            val usable = barHeight - gap * (visible.size - 1).coerceAtLeast(0)
            visible.forEach { part ->
                val partHeight = (usable * part.value / total).coerceAtLeast(barWidth * 0.5f)
                drawRoundRect(part.color.copy(alpha = part.color.alpha * partAlpha), Offset(left, top), Size(barWidth, partHeight), radius)
                top += partHeight + gap
            }
            if (index == selected) {
                drawRoundRect(highlight, Offset(left - 2.dp.toPx(), size.height - barHeight - 2.dp.toPx()), Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()), CornerRadius(barWidth, barWidth), style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}

private const val MIN_SWEEP = 6f
