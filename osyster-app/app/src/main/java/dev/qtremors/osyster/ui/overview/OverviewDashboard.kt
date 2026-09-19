@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.overview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.qtremors.osyster.ui.util.collectAsVisibleState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.monitor.AppStopperMonitor
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.CpuState
import dev.qtremors.osyster.monitor.DeviceHeroState
import dev.qtremors.osyster.monitor.MemoryState
import dev.qtremors.osyster.monitor.TelemetryResult
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import dev.qtremors.osyster.ui.util.RestrictedByOsBadge
import dev.qtremors.osyster.monitor.NetworkInterval
import dev.qtremors.osyster.monitor.NetworkInterfaceFilter
import dev.qtremors.osyster.monitor.NetworkMonitor
import dev.qtremors.osyster.monitor.RealtimeSpeed
import dev.qtremors.osyster.monitor.ScreenUsageHelper
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.navigation.AppRoutes
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.R
import dev.qtremors.osyster.settings.OsysterPreferencesState
import dev.qtremors.osyster.settings.StayAwakeChangeResult
import dev.qtremors.osyster.settings.StayAwakeController
import dev.qtremors.osyster.settings.StayAwakeState
import dev.qtremors.osyster.ui.theme.MotionTokens
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.theme.pressBounce
import dev.qtremors.osyster.ui.viewmodel.BentoUiState
import dev.qtremors.osyster.ui.viewmodel.BentoViewModel
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoCategory
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
    val animatedProgress by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f) / 100f,
        animationSpec = MotionTokens.GaugeSmoothSpring,
        label = "oyster_arc_gauge_progress"
    )
    CircularWavyProgressIndicator(
        progress = { animatedProgress },
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
fun OverviewDashboard(
    onNavigateTo: (AppRoutes) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: BentoViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferencesManager = remember { OsysterPreferencesManager.getInstance(context) }
    val prefsState by preferencesManager.state.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsVisibleState()
    val stayAwakeChangeFailedMessage = stringResource(R.string.stay_awake_change_failed)
    var stayAwakeState by remember { mutableStateOf(StayAwakeController.read(context)) }
    var showStayAwakeSetup by remember { mutableStateOf(false) }

    LifecycleResumeEffect(prefsState.managedStopPackages) {
        viewModel.refreshAppStopperCounts(prefsState.managedStopPackages)
        viewModel.refreshNetworkSummary()
        viewModel.refreshScreenTimeSummary()
        stayAwakeState = StayAwakeController.read(context)
        onPauseOrDispose { }
    }

    BentoDashboardContent(
        uiState = uiState,
        prefsState = prefsState,
        onNavigateTo = onNavigateTo,
        onOpenSettings = onOpenSettings,
        stayAwakeState = stayAwakeState,
        onToggleStayAwake = {
            when (StayAwakeController.setEnabled(context, !stayAwakeState.enabled)) {
                StayAwakeChangeResult.CHANGED -> stayAwakeState = StayAwakeController.read(context)
                StayAwakeChangeResult.PERMISSION_REQUIRED -> showStayAwakeSetup = true
                StayAwakeChangeResult.FAILED -> Toast.makeText(
                    context,
                    stayAwakeChangeFailedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        modifier = modifier
    )

    if (showStayAwakeSetup) {
        StayAwakeSetupDialog(
            onDismiss = { showStayAwakeSetup = false },
            onPermissionRecheck = {
                stayAwakeState = StayAwakeController.read(context)
                if (stayAwakeState.canWrite) showStayAwakeSetup = false
            }
        )
    }
}

@Deprecated(
    message = "Use OverviewDashboard instead",
    replaceWith = ReplaceWith("OverviewDashboard(onNavigateTo, modifier, onOpenSettings, viewModel)")
)
@Composable
fun BentoDashboard(
    onNavigateTo: (AppRoutes) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: BentoViewModel = viewModel()
) {
    OverviewDashboard(
        onNavigateTo = onNavigateTo,
        modifier = modifier,
        onOpenSettings = onOpenSettings,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BentoDashboardContent(
    uiState: BentoUiState,
    prefsState: OsysterPreferencesState,
    onNavigateTo: (AppRoutes) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    stayAwakeState: StayAwakeState = StayAwakeState(false, false, 0),
    onToggleStayAwake: () -> Unit = {}
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
    val animatedRamProgress by animateFloatAsState(
        targetValue = (ramUsedPercent / 100f).coerceIn(0f, 1f),
        animationSpec = MotionTokens.GaugeSmoothSpring,
        label = "bento_ram_progress"
    )
    val animatedStorageProgress by animateFloatAsState(
        targetValue = (uiState.storageStats.usedPercentage / 100f).coerceIn(0f, 1f),
        animationSpec = MotionTokens.GaugeSmoothSpring,
        label = "bento_storage_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1st Widget: Hero Device Identity & Uptime / Deep Sleep Widget
        HeroDeviceWidget(
            hero = uiState.heroState,
            onClick = { onNavigateTo(AppRoutes.Display) }
        )

        // Quick Reach Jump Ribbon
        QuickReachRibbon(
            onNavigateTo = onNavigateTo,
            hapticEnabled = prefsState.hapticFeedback
        )

        StayAwakeTile(
            state = stayAwakeState,
            hapticEnabled = prefsState.hapticFeedback,
            onToggle = onToggleStayAwake,
            modifier = Modifier.fillMaxWidth()
        )

        // Core Duo Row: CPU & GPU Diagnostics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CPU Bento Block
            Card(
                onClick = { onNavigateTo(AppRoutes.Cpu) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .pressBounce()
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
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        when (val temp = cpuState.cpuTempCelsius) {
                            is TelemetryResult.Available -> {
                                Text(
                                    text = prefsState.temperatureUnit.format(temp.value),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (temp.value > 50f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                                )
                            }
                            else -> {}
                        }
                    }

                    Text(
                        text = stringResource(R.string.bento_processor_load),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    when (val usage = cpuState.overallUsage) {
                        is TelemetryResult.Available -> {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", usage.value),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                        is TelemetryResult.Restricted -> {
                            RestrictedByOsBadge()
                        }
                    }

                    Text(
                        text = cpuState.cpuModel.ifEmpty { stringResource(R.string.telemetry_tab_cpu) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // GPU Bento Block
            Card(
                onClick = { onNavigateTo(AppRoutes.Gpu) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .pressBounce()
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
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (uiState.gpuSpecs.vulkanVersion.isNotBlank()) "Vulkan" else "GLES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.bento_gpu_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = uiState.gpuSpecs.renderer.ifBlank { "Integrated GPU" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = uiState.gpuSpecs.glEsVersion.ifBlank { stringResource(R.string.category_soc_gpu) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Core Duo Row: RAM & Internal Storage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Memory Bento Block
            Card(
                onClick = { onNavigateTo(AppRoutes.Memory) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .pressBounce()
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
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        if (memoryState.swapTotalKb > 0) {
                            Text(
                                text = "SWAP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

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

                    LinearWavyProgressIndicator(
                        progress = { animatedRamProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                }
            }

            // Storage Bento Block
            Card(
                onClick = { onNavigateTo(AppRoutes.Storage) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .pressBounce()
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
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        if (uiState.storageStats.availableBytes > 0) {
                            Text(
                                text = NetworkMonitor.formatBytes(uiState.storageStats.availableBytes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.bento_internal_storage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", uiState.storageStats.usedPercentage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )

                    LinearProgressIndicator(
                        progress = { animatedStorageProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                    )
                }
            }
        }

        // Battery Power Bento Block
        Card(
            onClick = { onNavigateTo(AppRoutes.Battery) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth()
                .pressBounce()
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
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (batteryState.status == "Charging") Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.bento_battery_power),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val details = if (batteryState.powerSource.isNotBlank() && batteryState.powerSource != "Battery") {
                            "${batteryState.status} • ${batteryState.powerSource}"
                        } else {
                            batteryState.status
                        }
                        Text(
                            text = details,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${batteryState.levelPercentage}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        if (batteryState.tempCelsius > 0f) {
                            Text(
                                text = prefsState.temperatureUnit.format(batteryState.tempCelsius),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Network Hub Bento Block (Page 1 Destination Link)
        Card(
            onClick = { onNavigateTo(AppRoutes.Network) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth()
                .pressBounce()
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
                                else -> stringResource(R.string.dock_network)
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

        // Apps & App Stopper Bento Block (Page 2 Destination Link)
        Card(
            onClick = { onNavigateTo(AppRoutes.AppStopper) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth()
                .pressBounce()
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
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.dock_tasks),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val subtitleText = when {
                            appStopperCounts.totalCount == 0 -> stringResource(R.string.bento_running_tasks) + ": $processesCount"
                            appStopperCounts.uninstalledCount > 0 -> "$processesCount active • ${appStopperCounts.installedCount} managed"
                            else -> "$processesCount active • ${appStopperCounts.installedCount} managed"
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

        // Screen Time Bento Block
        Card(
            onClick = { onNavigateTo(AppRoutes.ScreenTime) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth()
                .pressBounce()
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
                                imageVector = Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.bento_screentime_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val subtitle = when {
                            !uiState.hasUsageAccess -> stringResource(R.string.onboarding_perm_usage_title)
                            uiState.topUsedAppName.isNotBlank() -> "Most used: ${uiState.topUsedAppName}"
                            else -> stringResource(R.string.bento_screentime_desc)
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.hasUsageAccess && uiState.screenTimeTodayMillis > 0L) {
                        Text(
                            text = ScreenUsageHelper.formatDuration(uiState.screenTimeTodayMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
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

        // Tools & Diagnostics Duo Row: Live Sensors & Security/DRM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Sensors Card
            Card(
                onClick = { onNavigateTo(AppRoutes.Sensors) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .pressBounce()
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.bento_sensors_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (uiState.sensorCount > 0) "${uiState.sensorCount} Sensors" else "Live Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Oscilloscope →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // DRM & Camera Diagnostics Card
            Card(
                onClick = { onNavigateTo(AppRoutes.Drm) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .pressBounce()
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.bento_media_drm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Widevine " + uiState.drmSecurityLevel.ifBlank { "L1" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (uiState.cameraCount > 0) "${uiState.cameraCount} Cameras • ${uiState.maxCameraMp}" else "Media Security →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Hardware & Software Duo Row: Display & OS Kernel Specs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display & Specs Card
            Card(
                onClick = { onNavigateTo(AppRoutes.Display) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .pressBounce()
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = stringResource(R.string.bento_hardware_specs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = uiState.displaySpecs.resolution.ifBlank { "Display Screen" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (uiState.displaySpecs.refreshRateHz.isNotBlank()) "${uiState.displaySpecs.refreshRateHz}Hz Screen" else "View Specs →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Software & Kernel Card
            Card(
                onClick = { onNavigateTo(AppRoutes.System) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .weight(1f)
                    .pressBounce()
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = stringResource(R.string.bento_software_specs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Linux " + uiState.systemSpecs.kernelVersion.substringBefore(" ").ifBlank { "Kernel" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SELinux: " + uiState.systemSpecs.selinuxStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}

@Composable
private fun StayAwakeTile(
    state: StayAwakeState,
    hapticEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val active = state.enabled
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        modifier = modifier
            .toggleable(
                value = active,
                role = Role.Switch,
                onValueChange = {
                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                    onToggle()
                }
            )
            .pressBounce()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (active) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DeveloperMode,
                        contentDescription = null,
                        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.stay_awake_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.stay_awake_tile_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(100),
                color = if (active) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ) {
                Text(
                    text = stringResource(
                        when {
                            !state.canWrite -> R.string.stay_awake_setup
                            active -> R.string.stay_awake_on
                            else -> R.string.stay_awake_off
                        }
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StayAwakeSetupDialog(
    onDismiss: () -> Unit,
    onPermissionRecheck: () -> Unit
) {
    val context = LocalContext.current
    val command = remember(context) { StayAwakeController.grantCommand(context) }
    val copiedMessage = stringResource(R.string.stay_awake_command_copied)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
        title = { Text(stringResource(R.string.stay_awake_setup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.stay_awake_setup_desc))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    SelectionContainer {
                        Text(
                            text = command,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("ADB command", command))
                            Toast.makeText(
                                context,
                                copiedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text(stringResource(R.string.stay_awake_copy_command))
                    }
                    TextButton(
                        onClick = {
                            runCatching { context.startActivity(StayAwakeController.developerOptionsIntent()) }
                        }
                    ) {
                        Text(stringResource(R.string.stay_awake_developer_options))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onPermissionRecheck) {
                Text(stringResource(R.string.stay_awake_check_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

// =========================================================================
// Hero Device Identity & Deep Sleep Widget (1st Widget)
// =========================================================================

@Composable
fun HeroDeviceWidget(
    hero: DeviceHeroState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
            .fillMaxWidth()
            .pressBounce()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = hero.deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${hero.androidVersion} • API ${hero.apiLevel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (hero.securityPatch.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = hero.securityPatch,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Split Bar for Deep Sleep vs Awake
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.bento_deep_sleep) + ": ${hero.formattedDeepSleep} (${String.format(Locale.getDefault(), "%.0f%%", hero.deepSleepPercentage)})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.bento_awake) + ": ${hero.formattedAwake} (${String.format(Locale.getDefault(), "%.0f%%", hero.awakePercentage)})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Dual progress split bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(100))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    val sleepWeight = (hero.deepSleepPercentage / 100f).coerceIn(0.02f, 0.98f)
                    val awakeWeight = (hero.awakePercentage / 100f).coerceIn(0.02f, 0.98f)
                    val animatedSleepWeight by animateFloatAsState(
                        targetValue = sleepWeight,
                        animationSpec = MotionTokens.GaugeSmoothSpring,
                        label = "hero_sleep_weight"
                    )
                    val animatedAwakeWeight by animateFloatAsState(
                        targetValue = awakeWeight,
                        animationSpec = MotionTokens.GaugeSmoothSpring,
                        label = "hero_awake_weight"
                    )

                    Box(
                        modifier = Modifier
                            .weight(animatedSleepWeight)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .weight(animatedAwakeWeight)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Bottom row: Uptime readout and link
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.bento_uptime) + ": " + hero.formattedUptime,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.bento_hardware_specs),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// Quick Reach Jump Ribbon (Thumb Reachability)
// =========================================================================

@Composable
fun QuickReachRibbon(
    onNavigateTo: (AppRoutes) -> Unit,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val jumpItems = remember {
        listOf(
            Triple("CPU", Icons.Default.Speed) { onNavigateTo(AppRoutes.Cpu) },
            Triple("GPU", Icons.Default.DeveloperMode) { onNavigateTo(AppRoutes.Gpu) },
            Triple("RAM", Icons.Default.Memory) { onNavigateTo(AppRoutes.Memory) },
            Triple("Storage", Icons.Default.Storage) { onNavigateTo(AppRoutes.Storage) },
            Triple("Network", Icons.Default.Wifi) { onNavigateTo(AppRoutes.Network) },
            Triple("Apps", Icons.Default.Apps) { onNavigateTo(AppRoutes.AppStopper) },
            Triple("Screen Time", Icons.Default.QueryStats) { onNavigateTo(AppRoutes.ScreenTime) },
            Triple("Sensors", Icons.Default.Sensors) { onNavigateTo(AppRoutes.Sensors) },
            Triple("Battery", Icons.Default.BatteryStd) { onNavigateTo(AppRoutes.Battery) },
            Triple("Cameras", Icons.Default.CameraAlt) { onNavigateTo(AppRoutes.Camera) },
            Triple("DRM", Icons.Default.Lock) { onNavigateTo(AppRoutes.Drm) },
            Triple("Specs", Icons.Default.Smartphone) { onNavigateTo(AppRoutes.Display) },
            Triple("OS", Icons.Default.CheckCircle) { onNavigateTo(AppRoutes.System) }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        jumpItems.forEach { (label, icon, action) ->
            Surface(
                onClick = {
                    OsysterHapticUtil.performSegmentTick(view, hapticEnabled)
                    action()
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .height(36.dp)
                    .pressBounce()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
