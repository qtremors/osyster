@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.qtremors.osyster.ui.util.collectAsVisibleState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.monitor.AppStopperMonitor
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.CpuState
import dev.qtremors.osyster.monitor.MemoryState
import dev.qtremors.osyster.monitor.TelemetryResult
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import dev.qtremors.osyster.ui.util.RestrictedByOsBadge
import dev.qtremors.osyster.monitor.NetworkInterval
import dev.qtremors.osyster.monitor.NetworkInterfaceFilter
import dev.qtremors.osyster.monitor.NetworkMonitor
import dev.qtremors.osyster.monitor.RealtimeSpeed
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.navigation.AppRoutes
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.R
import dev.qtremors.osyster.settings.OsysterPreferencesState
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.viewmodel.BentoUiState
import dev.qtremors.osyster.ui.viewmodel.BentoViewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

// =========================================================================
// Section Comment: Custom Original Canvas Gauge (Oyster Ring)
// =========================================================================

@Composable
fun OysterArcGauge(
    percentage: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
) {
    CircularWavyProgressIndicator(
        progress = { percentage.coerceIn(0f, 100f) / 100f },
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}

// =========================================================================
// Section Comment: Bento Grid Layout Dashboard Screen
// =========================================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BentoDashboard(
    onNavigateTo: (AppRoutes) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BentoViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { OsysterPreferencesManager.getInstance(context) }
    val prefsState by preferencesManager.state.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsVisibleState()

    LifecycleResumeEffect(prefsState.managedStopPackages) {
        viewModel.refreshAppStopperCounts(prefsState.managedStopPackages)
        viewModel.refreshNetworkSummary()
        onPauseOrDispose { }
    }

    BentoDashboardContent(
        uiState = uiState,
        prefsState = prefsState,
        onNavigateTo = onNavigateTo,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BentoDashboardContent(
    uiState: BentoUiState,
    prefsState: OsysterPreferencesState,
    onNavigateTo: (AppRoutes) -> Unit,
    modifier: Modifier = Modifier
) {
    val cpuState = uiState.cpuState
    val memoryState = uiState.memoryState
    val batteryState = uiState.batteryState
    val processesCount = uiState.processesCount
    val realtimeSpeed = uiState.realtimeSpeed
    val activeNetworkType = uiState.activeNetworkType
    val todayNetworkTotal = uiState.todayNetworkTotal
    val todayNetworkLabel = uiState.todayNetworkLabel
    val appStopperCounts = uiState.appStopperCounts
    val ramUsedPercent = uiState.ramUsedPercent

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Large CPU Core Ring Bento Block
        Card(
            onClick = { onNavigateTo(AppRoutes.Cpu) },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bento_processor_load),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    when (val usage = cpuState.overallUsage) {
                        is TelemetryResult.Available -> {
                            Text(
                                text = stringResource(R.string.bento_cpu_load_format, usage.value),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                        is TelemetryResult.Restricted -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.restricted),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                RestrictedByOsBadge()
                            }
                        }
                    }
                    Text(
                        text = cpuState.cpuModel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                val isUsageRestricted = cpuState.overallUsage is TelemetryResult.Restricted
                val usagePercent = (cpuState.overallUsage as? TelemetryResult.Available)?.value ?: 0f

                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OysterArcGauge(
                        percentage = usagePercent,
                        modifier = Modifier.fillMaxSize(),
                        color = if (isUsageRestricted) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isUsageRestricted) Icons.Default.Lock else Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isUsageRestricted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Two Column Grid Row (Memory & Thermal blocks)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Memory Bento Block
            Card(
                onClick = { onNavigateTo(AppRoutes.Memory) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.bento_active_ram),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", ramUsedPercent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )

                    // Expressive Wavy RAM Bar
                    LinearWavyProgressIndicator(
                        progress = { ramUsedPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                }
            }

            // Thermal Bento Block
            Card(
                onClick = { onNavigateTo(AppRoutes.Cpu) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isTempElevated = (cpuState.cpuTempCelsius as? TelemetryResult.Available)?.let { it.value > 50f } ?: false
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = if (isTempElevated) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.bento_cpu_temp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    when (val temp = cpuState.cpuTempCelsius) {
                        is TelemetryResult.Available -> {
                            Text(
                                text = prefsState.temperatureUnit.format(temp.value),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (temp.value > 50f) stringResource(R.string.bento_temp_elevated) else stringResource(R.string.bento_temp_stable),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (temp.value > 50f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                            )
                        }
                        is TelemetryResult.Restricted -> {
                            Text(
                                text = stringResource(R.string.restricted),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.outline
                            )
                            RestrictedByOsBadge()
                        }
                    }
                }
            }
        }

        // Two Column Grid Row (Tasks & Battery blocks)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Tasks block
            Card(
                onClick = { onNavigateTo(AppRoutes.Processes) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.bento_running_tasks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "$processesCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(R.string.bento_tap_to_manage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Battery Energy block
            Card(
                onClick = { onNavigateTo(AppRoutes.DeviceInfo) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (batteryState.status == "Charging") Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        if (batteryState.tempCelsius > 0f) {
                            Text(
                                text = prefsState.temperatureUnit.format(batteryState.tempCelsius),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.bento_battery_power),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${batteryState.levelPercentage}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (batteryState.powerSource.isNotBlank() && batteryState.powerSource != "Battery") {
                            "${batteryState.status} • ${batteryState.powerSource}"
                        } else {
                            batteryState.status
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Network Traffic Bento Block
        Card(
            onClick = { onNavigateTo(AppRoutes.Network) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = when (activeNetworkType) {
                            NetworkInterfaceFilter.MOBILE -> Color(0xFFFFB300).copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (activeNetworkType) {
                                    NetworkInterfaceFilter.MOBILE -> Icons.Default.SignalCellularAlt
                                    else -> Icons.Default.Wifi
                                },
                                contentDescription = null,
                                tint = when (activeNetworkType) {
                                    NetworkInterfaceFilter.MOBILE -> Color(0xFFFFB300)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = when (activeNetworkType) {
                                NetworkInterfaceFilter.MOBILE -> stringResource(R.string.network_traffic_mobile)
                                NetworkInterfaceFilter.WIFI -> stringResource(R.string.network_traffic_wifi)
                                else -> stringResource(R.string.network_bento_title)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "↓ ${NetworkMonitor.formatSpeed(realtimeSpeed.rxBytesPerSec)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00E6FF)
                            )
                            Text(
                                text = "↑ ${NetworkMonitor.formatSpeed(realtimeSpeed.txBytesPerSec)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todayNetworkTotal.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = todayNetworkTotal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = todayNetworkLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // App Stopper Bento Block
        Card(
            onClick = { onNavigateTo(AppRoutes.AppStopper) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_stopper_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val subtitleText = when {
                            appStopperCounts.totalCount == 0 -> stringResource(R.string.bento_app_stopper_empty)
                            appStopperCounts.uninstalledCount > 0 -> stringResource(R.string.bento_app_stopper_with_uninstalled, appStopperCounts.installedCount, appStopperCounts.uninstalledCount)
                            else -> stringResource(R.string.bento_app_stopper_monitored, appStopperCounts.installedCount)
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // SWAP File block (Bottom of grid)
        Card(
            onClick = { onNavigateTo(AppRoutes.Memory) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeveloperMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.bento_virtual_swap),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val swapActive = memoryState.swapTotalKb > 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(100))
                            .background(if (swapActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (swapActive) stringResource(R.string.status_active) else stringResource(R.string.status_inactive),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (swapActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
private fun BentoDashboardPreview() {
    OsysterTheme {
        BentoDashboardContent(
            uiState = BentoUiState(
                cpuState = dev.qtremors.osyster.monitor.CpuState(
                    overallUsage = dev.qtremors.osyster.monitor.TelemetryResult.Available(28.4f),
                    coreStates = emptyList(),
                    cpuTempCelsius = dev.qtremors.osyster.monitor.TelemetryResult.Available(36.5f),
                    cpuModel = "Snapdragon 8 Gen 2",
                    cpuArchitecture = "aarch64"
                ),
                memoryState = dev.qtremors.osyster.monitor.MemoryState(
                    ramTotalKb = 8388608L,
                    ramUsedKb = 4194304L,
                    ramAvailableKb = 4194304L,
                    ramFreeKb = 2097152L,
                    ramCachedKb = 1572864L,
                    ramBuffersKb = 524288L,
                    swapTotalKb = 4194304L,
                    swapUsedKb = 0L,
                    swapFreeKb = 4194304L
                ),
                batteryState = dev.qtremors.osyster.monitor.BatteryState(
                    levelPercentage = 78,
                    tempCelsius = 32.0f,
                    health = "Good",
                    status = "Discharging",
                    voltageMv = 4050,
                    powerSource = "Battery"
                ),
                processesCount = 142,
                realtimeSpeed = dev.qtremors.osyster.monitor.RealtimeSpeed(1250000L, 450000L),
                activeNetworkType = dev.qtremors.osyster.monitor.NetworkInterfaceFilter.WIFI,
                todayNetworkTotal = "2.45 GB",
                todayNetworkLabel = "Wi-Fi • Today",
                appStopperCounts = dev.qtremors.osyster.monitor.AppStopperCounts(5, 1, 6)
            ),
            prefsState = OsysterPreferencesState(),
            onNavigateTo = {}
        )
    }
}
