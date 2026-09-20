package dev.qtremors.osyster.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.theme.MetricTextStyle

/**
 * One number in the hero card. [value] is already formatted ("5.1 GB" or "Restricted");
 * [progress] draws a meter when the value has a meaningful fill level.
 */
@Immutable
data class HeroMetric(
    val label: String,
    val value: String,
    val detail: String? = null,
    val progress: Float? = null,
    val icon: ImageVector? = null
)

/**
 * The large card at the top of Home: which phone this is, and a few live readings under it.
 * It uses the primary container so it stands apart from the neutral rows below.
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
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
            metrics.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { metric -> MetricTile(metric, Modifier.weight(1f)) }
                    if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
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
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (metric.icon != null) {
                    Icon(metric.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.height(16.dp))
                }
                Text(metric.label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            Text(
                metric.value,
                style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (metric.detail != null) {
                Text(metric.detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
            }
            if (metric.progress != null) {
                LinearWavyProgressIndicator(
                    progress = { metric.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}
