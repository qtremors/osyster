package dev.qtremors.osyster.core.ui.components

import dev.qtremors.osyster.core.device.domain.LevelStatus
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.qtremors.osyster.core.ui.theme.MetricTextStyle

/** The small picture at the bottom of a dashboard tile; each kind of reading gets its own. */
@Immutable
sealed interface HeroGraphic {
    /** Recent values from 0 to 1, oldest first, drawn as a filled line on a fixed scale. */
    data class Trend(val points: List<Float>) : HeroGraphic

    /** A half-ring gauge, for how full something is right now (memory). */
    data class Gauge(val fraction: Float) : HeroGraphic

    /** Ten segments filled to the level, for amounts like storage. */
    data class Segments(val fraction: Float) : HeroGraphic

    /** A battery shape filled to the charge, with a bolt while charging. */
    data class Battery(val fraction: Float, val charging: Boolean) : HeroGraphic

    /** Many thin bars, such as screen time in each hour of today. */
    data class Bars(val values: List<Float>) : HeroGraphic

    /** One bar split into colored parts, such as mobile and Wi-Fi data. */
    data class Split(val parts: List<ChartPart>) : HeroGraphic
}

@Immutable
data class HeroMetric(
    val label: String,
    val value: String,
    val detail: String? = null,
    val icon: ImageVector? = null,
    val graphic: HeroGraphic? = null,
    /** Colors the icon and graphic green, amber, or red; null keeps the accent color. */
    val status: LevelStatus? = null,
    val onClick: (() -> Unit)? = null
)

/**
 * The large card at the top of Home: which phone this is, and live tiles under it. Tiles come
 * in pairs of equal height; values and details shrink to fit rather than being cut off.
 */
@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    metrics: List<HeroMetric>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.onPrimaryContainer, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer.copy(alpha = 0.78f))
                }
                if (icon != null) Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.padding(start = 12.dp))
            }
            metrics.chunked(2).forEach { pair ->
                // Intrinsic height makes both tiles in a row as tall as the taller one.
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { metric -> MetricTile(metric, Modifier.weight(1f).fillMaxHeight()) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = colors.primaryContainer, modifier = modifier.fillMaxWidth()) { body() }
    } else {
        Surface(shape = shape, color = colors.primaryContainer, modifier = modifier.fillMaxWidth()) { body() }
    }
}

@Composable
private fun MetricTile(metric: HeroMetric, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val description = listOfNotNull(metric.label, metric.value, metric.detail).joinToString(", ")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface.copy(alpha = 0.55f),
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = description }
            .then(if (metric.onClick != null) Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = metric.onClick) else Modifier)
    ) {
        val graphic = metric.graphic
        // The battery stands upright beside the text; every other graphic sits along the bottom.
        if (graphic is HeroGraphic.Battery) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { TileTexts(metric) }
                Graphic(graphic, metric.status, Modifier.width(26.dp).fillMaxHeight())
            }
            return@Surface
        }
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TileTexts(metric)
            Spacer(Modifier.weight(1f))
            graphic?.let { Graphic(it, metric.status, Modifier.fillMaxWidth().height(GRAPHIC_HEIGHT).padding(top = 6.dp)) }
        }
    }
}

/** Label with icon, the value, and the detail line; values shrink to fit instead of being cut off. */
@Composable
private fun TileTexts(metric: HeroMetric) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (metric.icon != null) Icon(metric.icon, contentDescription = null, tint = metric.status?.let { StatusColors.current().of(it) } ?: colors.primary, modifier = Modifier.size(16.dp))
        Text(metric.label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    BasicText(
        text = metric.value,
        style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize, color = colors.onSurface),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = MaterialTheme.typography.titleLarge.fontSize)
    )
    if (metric.detail != null) {
        BasicText(
            text = metric.detail,
            style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = MaterialTheme.typography.bodySmall.fontSize)
        )
    }
}

@Composable
private fun Graphic(graphic: HeroGraphic, status: LevelStatus?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val accent = status?.let { StatusColors.current().of(it) } ?: colors.primary
    val track = colors.surfaceContainerHighest
    Canvas(modifier = modifier) {
        when (graphic) {
            is HeroGraphic.Trend -> drawTrend(graphic.points, accent)
            is HeroGraphic.Gauge -> drawGauge(graphic.fraction, accent, track)
            is HeroGraphic.Segments -> drawSegments(graphic.fraction, accent, track)
            is HeroGraphic.Battery -> drawBattery(graphic, accent, track, colors.surface)
            is HeroGraphic.Bars -> drawBars(graphic.values, accent, track)
            is HeroGraphic.Split -> drawSplit(graphic.parts, track)
        }
    }
}

/**
 * Fixed slots, newest at the right, fixed 0 to 1 scale: the line slides without rescaling. Until
 * the slots fill, the first reading is carried back to the left edge, so the graph spans the tile
 * from the first frame instead of starting as a sliver on the right.
 */
private fun DrawScope.drawTrend(points: List<Float>, color: Color) {
    if (points.isEmpty()) {
        drawLine(color.copy(alpha = 0.25f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        return
    }
    val step = size.width / (TREND_SLOTS - 1)
    val offset = (TREND_SLOTS - points.size).coerceAtLeast(0)
    val line = Path()
    val firstY = size.height - points.first().coerceIn(0f, 1f) * size.height
    line.moveTo(0f, firstY)
    points.takeLast(TREND_SLOTS).forEachIndexed { index, value ->
        val x = (offset + index) * step
        val y = size.height - value.coerceIn(0f, 1f) * size.height
        line.lineTo(x, y)
    }
    val area = Path().apply {
        addPath(line)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f))))
    drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawGauge(fraction: Float, color: Color, track: Color) {
    val stroke = 7.dp.toPx()
    val diameter = minOf(size.width, size.height * 2f) - stroke
    val topLeft = Offset((size.width - diameter) / 2, stroke / 2)
    val arc = Size(diameter, diameter)
    drawArc(track, 180f, 180f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
    drawArc(color, 180f, 180f * fraction.coerceIn(0f, 1f), false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
}

private fun DrawScope.drawSegments(fraction: Float, color: Color, track: Color) {
    val count = SEGMENTS
    val gap = 3.dp.toPx()
    val height = 10.dp.toPx()
    val width = (size.width - gap * (count - 1)) / count
    val top = size.height - height
    val filled = fraction.coerceIn(0f, 1f) * count
    for (index in 0 until count) {
        val left = index * (width + gap)
        drawRoundRect(track, Offset(left, top), Size(width, height), CornerRadius(height / 2))
        val portion = (filled - index).coerceIn(0f, 1f)
        if (portion > 0f) drawRoundRect(color, Offset(left, top), Size(width * portion, height), CornerRadius(height / 2))
    }
}

/** An upright battery: the nub on top, the charge filling from the bottom, a bolt while charging. */
private fun DrawScope.drawBattery(graphic: HeroGraphic.Battery, color: Color, track: Color, bolt: Color) {
    val nub = 4.dp.toPx()
    val gap = 2.dp.toPx()
    val width = size.width
    val bodyTop = nub + gap
    val bodyHeight = size.height - bodyTop
    val radius = CornerRadius(6.dp.toPx())
    drawRoundRect(track, Offset(width * 0.3f, 0f), Size(width * 0.4f, nub), CornerRadius(2.dp.toPx()))
    drawRoundRect(track, Offset(0f, bodyTop), Size(width, bodyHeight), radius)
    val inset = 3.dp.toPx()
    val fill = (bodyHeight - inset * 2) * graphic.fraction.coerceIn(0f, 1f)
    if (fill > 0f) {
        drawRoundRect(color, Offset(inset, bodyTop + bodyHeight - inset - fill), Size(width - inset * 2, fill), CornerRadius(4.dp.toPx()))
    }
    if (graphic.charging) {
        val cx = width / 2
        val cy = bodyTop + bodyHeight / 2
        val h = minOf(width * 0.42f, bodyHeight * 0.22f)
        val boltPath = Path().apply {
            moveTo(cx + h * 0.2f, cy - h)
            lineTo(cx - h * 0.45f, cy + h * 0.1f)
            lineTo(cx, cy + h * 0.1f)
            lineTo(cx - h * 0.2f, cy + h)
            lineTo(cx + h * 0.45f, cy - h * 0.1f)
            lineTo(cx, cy - h * 0.1f)
            close()
        }
        drawPath(boltPath, bolt)
    }
}

private fun DrawScope.drawBars(values: List<Float>, color: Color, track: Color) {
    if (values.isEmpty()) return
    val max = values.max().takeIf { it > 0f } ?: 1f
    val column = size.width / values.size
    val width = (column * 0.6f).coerceAtLeast(1.5f)
    values.forEachIndexed { index, value ->
        val left = index * column + (column - width) / 2
        drawRoundRect(track, Offset(left, size.height - 2.dp.toPx()), Size(width, 2.dp.toPx()), CornerRadius(width / 2))
        val height = size.height * (value / max)
        if (value > 0f) drawRoundRect(color, Offset(left, size.height - height), Size(width, height), CornerRadius(width / 2))
    }
}

private fun DrawScope.drawSplit(parts: List<ChartPart>, track: Color) {
    val height = 10.dp.toPx()
    val top = size.height - height
    drawRoundRect(track, Offset(0f, top), Size(size.width, height), CornerRadius(height / 2))
    val total = parts.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return
    var left = 0f
    val gap = 2.dp.toPx()
    val visible = parts.filter { it.value > 0f }
    val usable = size.width - gap * (visible.size - 1).coerceAtLeast(0)
    visible.forEach { part ->
        val width = usable * part.value / total
        drawRoundRect(part.color, Offset(left, top), Size(width, height), CornerRadius(height / 2))
        left += width + gap
    }
}

private val GRAPHIC_HEIGHT = 34.dp
private const val TREND_SLOTS = 30
private const val SEGMENTS = 10
