@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.qtremors.osyster.monitor.CpuCoreState
import dev.qtremors.osyster.monitor.CpuState
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.monitor.TelemetryResult
import dev.qtremors.osyster.R
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import dev.qtremors.osyster.ui.util.RestrictedByOsBadge
import dev.qtremors.osyster.ui.viewmodel.CpuUiState
import dev.qtremors.osyster.ui.viewmodel.CpuViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

// =========================================================================
// Section Comment: Custom Real-Time Sparkline Component
// =========================================================================

@Composable
fun Sparkline(
    history: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.5.dp
) {
    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val maxVal = 100f
        val pointsCount = history.size
        val xStep = width / (pointsCount - 1)

        val path = Path()
        val fillPath = Path()

        history.forEachIndexed { i, value ->
            val x = i * xStep
            val y = height - (value.coerceIn(0f, 100f) / maxVal) * height

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (i == pointsCount - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.24f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

// =========================================================================
// Section Comment: CPU Monitoring Dashboard
// =========================================================================

@Composable
fun CpuDashboard(
    modifier: Modifier = Modifier,
    viewModel: CpuViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CpuDashboardContent(
        uiState = uiState,
        modifier = modifier
    )
}

@Composable
fun CpuDashboardContent(
    uiState: CpuUiState,
    modifier: Modifier = Modifier
) {
    val cpuState = uiState.cpuState
    val cpuHistory = uiState.cpuHistory
    val temperatureUnit = uiState.temperatureUnit

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // CPU Core Gauge & Overall Load
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.cpu_processor_engine),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val usage = cpuState.overallUsage) {
                        is TelemetryResult.Available -> {
                            CircularWavyProgressIndicator(
                                progress = { (usage.value / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", usage.value),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.cpu_load_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is TelemetryResult.Restricted -> {
                            CircularWavyProgressIndicator(
                                progress = { 0f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(28.dp)
                                )
                                RestrictedByOsBadge()
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Sparkline(
                    history = cpuHistory.toList(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Processor specifications details
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.specifications),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.cpu_model_name), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = cpuState.cpuModel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.cpu_architecture), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = cpuState.cpuArchitecture,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.cpu_core_count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = stringResource(R.string.cpu_cores_format, cpuState.coreStates.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.cpu_temperature), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    when (val temp = cpuState.cpuTempCelsius) {
                        is TelemetryResult.Available -> {
                            Text(
                                text = temperatureUnit.format(temp.value),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (temp.value > 55f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                        is TelemetryResult.Restricted -> {
                            RestrictedByOsBadge()
                        }
                    }
                }
            }
        }

        // CPU Core load list
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.cpu_core_frequencies),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (cpuState.coreStates.isEmpty() || (cpuState.overallUsage is TelemetryResult.Restricted && cpuState.coreStates.all { it.currentFreqKhz == 0L })) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.cpu_restricted_selinux),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        RestrictedByOsBadge()
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        cpuState.coreStates.forEach { core ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.cpu_core_label, core.id),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (core.currentFreqKhz > 0) stringResource(R.string.cpu_freq_mhz, (core.currentFreqKhz / 1000).toInt()) else stringResource(R.string.not_applicable),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.0f%%", core.usagePercentage),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearWavyProgressIndicator(
                                    progress = { (core.usagePercentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}

@Preview(showBackground = true)
@Composable
private fun CpuDashboardPreview() {
    OsysterTheme {
        CpuDashboardContent(
            uiState = CpuUiState(
                cpuState = CpuState(
                    overallUsage = TelemetryResult.Available(42.5f),
                    coreStates = listOf(
                        CpuCoreState(0, 35f, 1800000L, 2400000L),
                        CpuCoreState(1, 50f, 2000000L, 2400000L)
                    ),
                    cpuTempCelsius = TelemetryResult.Available(39.2f),
                    cpuModel = "Snapdragon 8 Gen 2",
                    cpuArchitecture = "aarch64"
                ),
                cpuHistory = listOf(20f, 30f, 25f, 40f, 38f, 42.5f)
            )
        )
    }
}
