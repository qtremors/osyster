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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.SignalCellularAlt
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.CpuState
import dev.qtremors.osyster.monitor.MemoryState
import dev.qtremors.osyster.monitor.NetworkInterval
import dev.qtremors.osyster.monitor.NetworkInterfaceFilter
import dev.qtremors.osyster.monitor.NetworkMonitor
import dev.qtremors.osyster.monitor.RealtimeSpeed
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.navigation.AppRoutes
import dev.qtremors.osyster.settings.OsysterPreferencesManager
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
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    strokeWidth: androidx.compose.ui.unit.Dp = 8.dp
) {
    CircularProgressIndicator(
        progress = { percentage.coerceIn(0f, 100f) / 100f },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        strokeCap = StrokeCap.Round
    )
}

// =========================================================================
// Section Comment: Bento Grid Layout Dashboard Screen
// =========================================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BentoDashboard(
    onNavigateTo: (AppRoutes) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferencesManager = remember { OsysterPreferencesManager.getInstance(context) }
    val prefsState by preferencesManager.state.collectAsStateWithLifecycle()

    var cpuState by remember { mutableStateOf(SystemMonitor.getCpuState()) }
    var memoryState by remember { mutableStateOf(SystemMonitor.getMemoryState()) }
    var batteryState by remember { mutableStateOf(SystemMonitor.getBatteryState(context)) }
    var processesCount by remember { mutableIntStateOf(0) }
    val realtimeSpeed by remember { NetworkMonitor.streamRealtimeSpeed() }
        .collectAsStateWithLifecycle(initialValue = RealtimeSpeed(0L, 0L))
    var activeNetworkType by remember { mutableStateOf(NetworkMonitor.getActiveNetworkType(context)) }
    var todayNetworkTotal by remember { mutableStateOf("") }
    var todayNetworkLabel by remember { mutableStateOf("Today") }

    LaunchedEffect(Unit) {
        if (NetworkMonitor.hasUsageAccess(context)) {
            val summary = NetworkMonitor.queryNetworkUsage(
                context = context,
                interval = NetworkInterval.DAY,
                filter = NetworkInterfaceFilter.ALL,
                targetDateMillis = System.currentTimeMillis()
            )
            val currentType = NetworkMonitor.getActiveNetworkType(context)
            activeNetworkType = currentType
            when (currentType) {
                NetworkInterfaceFilter.MOBILE -> {
                    todayNetworkTotal = NetworkMonitor.formatBytes(summary.mobileBytes)
                    todayNetworkLabel = "Mobile • Today"
                }
                NetworkInterfaceFilter.WIFI -> {
                    todayNetworkTotal = NetworkMonitor.formatBytes(summary.wifiBytes)
                    todayNetworkLabel = "Wi-Fi • Today"
                }
                NetworkInterfaceFilter.ALL -> {
                    todayNetworkTotal = NetworkMonitor.formatBytes(summary.totalBytes)
                    todayNetworkLabel = "Today"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Collect diagnostic updates concurrently
        SystemMonitor.streamCpu(1000L).collectLatest { cpuState = it }
    }
    LaunchedEffect(Unit) {
        SystemMonitor.streamMemory(1000L).collectLatest { memoryState = it }
    }
    LaunchedEffect(Unit) {
        SystemMonitor.streamBattery(context, 3000L).collectLatest { batteryState = it }
    }
    LaunchedEffect(Unit) {
        while (true) {
            processesCount = SystemMonitor.getActiveProcesses().size
            kotlinx.coroutines.delay(4000)
        }
    }

    val ramUsedPercent = if (memoryState.ramTotalKb > 0) {
        (memoryState.ramUsedKb.toFloat() / memoryState.ramTotalKb.toFloat()) * 100f
    } else 0f

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
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTo(AppRoutes.Cpu) }
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
                        text = "Processor Load",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%% Load", cpuState.overallUsage),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = cpuState.cpuModel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OysterArcGauge(
                        percentage = cpuState.overallUsage,
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateTo(AppRoutes.Memory) }
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
                        text = "Active RAM",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", ramUsedPercent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )

                    // Linear RAM Bar
                    LinearProgressIndicator(
                        progress = { ramUsedPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(100)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                }
            }

            // Thermal Bento Block
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateTo(AppRoutes.Cpu) }
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = if (cpuState.cpuTempCelsius > 50f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "CPU Temp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f °C", cpuState.cpuTempCelsius),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (cpuState.cpuTempCelsius > 50f) "Elevated" else "Stable Temp",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cpuState.cpuTempCelsius > 50f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                    )
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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateTo(AppRoutes.Processes) }
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
                        text = "Running Tasks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "$processesCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Tap to manage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Battery Energy block
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateTo(AppRoutes.DeviceInfo) }
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
                        text = "Battery Power",
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTo(AppRoutes.Network) }
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
                                NetworkInterfaceFilter.MOBILE -> "Mobile Data"
                                NetworkInterfaceFilter.WIFI -> "Wi-Fi Traffic"
                                else -> "Network Traffic"
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

        // SWAP File block (Bottom of grid)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTo(AppRoutes.Memory) }
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
                        text = "Virtual SWAP",
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
                        text = if (swapActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (swapActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
