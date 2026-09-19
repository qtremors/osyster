@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.hardware

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.qtremors.osyster.R
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.CameraInfo
import dev.qtremors.osyster.monitor.ConnectivitySpecs
import dev.qtremors.osyster.monitor.CpuClusterInfo
import dev.qtremors.osyster.monitor.DeviceDisplaySpecs
import dev.qtremors.osyster.monitor.GpuSpecs
import dev.qtremors.osyster.monitor.MediaDrmSpecs
import dev.qtremors.osyster.monitor.SensorItem
import dev.qtremors.osyster.monitor.StorageStats
import dev.qtremors.osyster.monitor.SystemSpecs
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import dev.qtremors.osyster.ui.util.collectAsVisibleState
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoCategory
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoUiState
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoViewModel
import java.util.Locale

// =========================================================================
// Section Comment: Device Information & Modular Hardware Telemetry
// =========================================================================

typealias InfoGroupPosition = dev.qtremors.osyster.ui.expressive.GroupPosition

fun InfoGroupShape(position: InfoGroupPosition): RoundedCornerShape {
    return dev.qtremors.osyster.ui.expressive.expressiveGroupShape(position, outerCorner = 24.dp, innerCorner = 6.dp)
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    position: InfoGroupPosition,
    modifier: Modifier = Modifier
) {
    Card(
        shape = InfoGroupShape(position),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SensorOscilloscope(
    channelHistories: List<List<Float>>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF00E6FF), // Cyan
        Color(0xFFFFB300), // Amber
        Color(0xFF00E676)  // Emerald
    )
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Subtle oscilloscope horizontal grid lines
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(0f, height * 0.25f),
            end = Offset(width, height * 0.25f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(0f, height * 0.75f),
            end = Offset(width, height * 0.75f),
            strokeWidth = 1.dp.toPx()
        )

        val allValues = channelHistories.flatten()
        val maxVal = allValues.maxOrNull()?.coerceAtLeast(1f) ?: 10f
        val minVal = allValues.minOrNull()?.coerceAtMost(-1f) ?: -10f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        channelHistories.forEachIndexed { channelIdx, history ->
            if (history.size < 2) return@forEachIndexed
            val channelColor = colors.getOrElse(channelIdx) { Color(0xFF00E6FF) }
            val pointsCount = history.size
            val xStep = width / (pointsCount - 1).coerceAtLeast(1)

            val path = Path()
            history.forEachIndexed { i, value ->
                val x = i * xStep
                val normalized = ((value - minVal) / range).coerceIn(0f, 1f)
                val y = height - (normalized * (height - 16.dp.toPx())) - 8.dp.toPx()

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = channelColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun HardwareDashboard(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsVisibleState()
    val view = LocalView.current

    DeviceInfoDashboardContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack?.let { back ->
            {
                OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                back()
            }
        },
        onCategorySelected = { category ->
            OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
            viewModel.selectCategory(category)
        },
        onSensorSelected = { sensor ->
            OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
            viewModel.selectSensor(sensor)
        },
        modifier = modifier
    )
}

@Deprecated(
    message = "Use HardwareDashboard instead",
    replaceWith = ReplaceWith("HardwareDashboard(modifier, onNavigateBack, hapticEnabled, viewModel)")
)
@Composable
fun DeviceInfoDashboard(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    HardwareDashboard(
        modifier = modifier,
        onNavigateBack = onNavigateBack,
        hapticEnabled = hapticEnabled,
        viewModel = viewModel
    )
}

@Composable
fun DeviceInfoDashboardContent(
    uiState: DeviceInfoUiState,
    onNavigateBack: (() -> Unit)? = null,
    onCategorySelected: (DeviceInfoCategory) -> Unit = {},
    onSensorSelected: (SensorItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val batteryState = uiState.batteryState
    val systemSpecs = uiState.systemSpecs
    val displaySpecs = uiState.displaySpecs
    val storageStats = uiState.storageStats
    val selectedCategory = uiState.selectedCategory

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                DeviceInfoCategory.ALL to stringResource(R.string.category_all),
                DeviceInfoCategory.SYSTEM to stringResource(R.string.category_system),
                DeviceInfoCategory.DEVICE to stringResource(R.string.category_device),
                DeviceInfoCategory.SOC_GPU to stringResource(R.string.category_soc_gpu),
                DeviceInfoCategory.STORAGE to stringResource(R.string.category_storage),
                DeviceInfoCategory.BATTERY to stringResource(R.string.category_battery),
                DeviceInfoCategory.CAMERA to stringResource(R.string.category_camera),
                DeviceInfoCategory.SENSORS to stringResource(R.string.category_sensors),
                DeviceInfoCategory.CONNECTIVITY to stringResource(R.string.category_connectivity),
                DeviceInfoCategory.DRM to stringResource(R.string.category_drm)
            )

            categories.forEach { (cat, label) ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategorySelected(cat) },
                    label = { Text(label, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // =========================================================================
        // Section: Battery Diagnostics
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.BATTERY) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = batteryState.levelPercentage / 100f
                        CircularWavyProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = when {
                                batteryState.status == "Charging" -> MaterialTheme.colorScheme.primary
                                batteryState.levelPercentage < 20 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )

                        Icon(
                            imageVector = when {
                                batteryState.status == "Charging" -> Icons.Default.BatteryChargingFull
                                batteryState.levelPercentage < 20 -> Icons.Default.BatteryAlert
                                else -> Icons.Default.BatteryStd
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.device_battery_status),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${batteryState.levelPercentage}% • ${batteryState.status}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.device_battery_health, batteryState.health),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.device_battery_specs),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.cpu_temperature), if (batteryState.tempCelsius > 0f) uiState.temperatureUnit.format(batteryState.tempCelsius) else stringResource(R.string.not_applicable), InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.device_voltage), "${batteryState.voltageMv} mV", InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.device_power_connection), batteryState.powerSource, InfoGroupPosition.Middle)
                if (batteryState.currentNowMa != 0) {
                    InfoRow(stringResource(R.string.battery_current), stringResource(R.string.battery_current_ma, batteryState.currentNowMa), InfoGroupPosition.Middle)
                }
                if (batteryState.cycleCount >= 0) {
                    InfoRow(stringResource(R.string.battery_cycle_count), "${batteryState.cycleCount}", InfoGroupPosition.Middle)
                }
                if (batteryState.technology.isNotBlank()) {
                    InfoRow(stringResource(R.string.battery_technology), batteryState.technology, InfoGroupPosition.Middle)
                }
                InfoRow(stringResource(R.string.battery_thermal_status), batteryState.thermalStatus.ifEmpty { stringResource(R.string.not_applicable) }, InfoGroupPosition.Bottom)
            }
        }

        // =========================================================================
        // Section: Device & Display
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.DEVICE) {
            Text(
                text = stringResource(R.string.device_hardware_specs),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.device_brand), displaySpecs.brand, InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.device_manufacturer), displaySpecs.manufacturer, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.device_model), displaySpecs.model, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.device_product), displaySpecs.product, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.device_sku), displaySpecs.sku, InfoGroupPosition.Bottom)
            }

            Text(
                text = stringResource(R.string.display_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.display_name), displaySpecs.displayName, InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.display_resolution), displaySpecs.resolution, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.display_size), displaySpecs.diagonalInches, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.display_aspect_ratio), displaySpecs.aspectRatio, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.display_refresh_rate), displaySpecs.refreshRateHz, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.display_supported_refresh_rates), displaySpecs.supportedRefreshRates, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.display_pixel_density), displaySpecs.pixelDensityDpi, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.display_density_bucket), displaySpecs.densityBucket, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.display_orientation), displaySpecs.defaultOrientation, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.display_hdr), if (displaySpecs.hdrSupported) displaySpecs.hdrFormats else stringResource(R.string.not_supported), InfoGroupPosition.Bottom)
            }

            Text(
                text = stringResource(R.string.features_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.feature_fingerprint), if (displaySpecs.hasFingerprint) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.feature_face_auth), if (displaySpecs.hasFaceAuth) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.feature_vulkan), if (displaySpecs.hasVulkan) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.feature_encryption), if (displaySpecs.hasEncryption) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Bottom)
            }
        }

        // =========================================================================
        // Section: System Software & OS Specs
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.SYSTEM) {
            Text(
                text = stringResource(R.string.device_android_specs),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.device_android_version), systemSpecs.androidVersion, InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.device_api_level), systemSpecs.apiLevel, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_released_with), systemSpecs.releasedWith, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_play_update), systemSpecs.playUpdate, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.device_security_patch), systemSpecs.securityPatch, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_build_number), systemSpecs.buildNumber, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.device_board_hardware), systemSpecs.board, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.device_processor_platform), systemSpecs.hardware, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_kernel), systemSpecs.kernelVersion, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_baseband), systemSpecs.basebandVersion, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.device_bootloader_release), systemSpecs.bootloader, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_build_fingerprint), systemSpecs.buildFingerprint, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_build_type), systemSpecs.buildType, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_build_tags), systemSpecs.buildTags, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_performance_class), systemSpecs.performanceClass, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_min_target_sdk), systemSpecs.minTargetSdk, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_language), systemSpecs.language, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_uptime), systemSpecs.uptime, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_usb_debugging), if (systemSpecs.isUsbDebugging) stringResource(R.string.enabled) else stringResource(R.string.disabled), InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_selinux), systemSpecs.selinuxStatus, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_root), systemSpecs.rootStatus, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_emulator), if (systemSpecs.isEmulator) stringResource(R.string.yes) else stringResource(R.string.no), InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_page_size), "${systemSpecs.pageSizeKb} KB", InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_treble), if (systemSpecs.isTrebleEnabled) stringResource(R.string.enabled) else stringResource(R.string.disabled), InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_seamless_updates), if (systemSpecs.seamlessUpdates) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.system_active_slot), systemSpecs.activeSlot, InfoGroupPosition.Bottom)
            }
        }

        // =========================================================================
        // Section: SoC & GPU Diagnostics
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.SOC_GPU) {
            Text(
                text = stringResource(R.string.soc_clusters_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            if (uiState.cpuClusters.isEmpty()) {
                InfoRow(stringResource(R.string.device_processor_platform), systemSpecs.hardware, InfoGroupPosition.Single)
            } else {
                uiState.cpuClusters.forEach { cluster ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.soc_cluster_item, cluster.clusterId, cluster.coreCount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.soc_cluster_cores, cluster.coreRange),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            val minMhz = if (cluster.minFreqKhz > 0) "${cluster.minFreqKhz / 1000} MHz" else "N/A"
                            val maxMhz = if (cluster.maxFreqKhz > 0) "${cluster.maxFreqKhz / 1000} MHz" else "N/A"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.soc_cluster_freq_range), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                Text("$minMhz - $maxMhz", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.soc_cluster_governor), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                Text(cluster.governor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.gpu_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.gpu_vendor), uiState.gpuSpecs.vendor, InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.gpu_renderer), uiState.gpuSpecs.renderer, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.gpu_gles_version), uiState.gpuSpecs.glEsVersion, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.gpu_vulkan_version), uiState.gpuSpecs.vulkanVersion, InfoGroupPosition.Bottom)
            }
        }

        // =========================================================================
        // Section: Internal Storage & Memory
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.STORAGE) {
            Text(
                text = stringResource(R.string.storage_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.storage_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", storageStats.usedPercentage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearWavyProgressIndicator(
                        progress = { (storageStats.usedPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.storage_used), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        Text(formatStorageBytes(storageStats.usedBytes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.storage_available), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        Text(formatStorageBytes(storageStats.availableBytes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.storage_total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        Text(formatStorageBytes(storageStats.totalBytes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.ram_low_ram_device), if (uiState.isLowRamDevice) stringResource(R.string.yes) else stringResource(R.string.no), InfoGroupPosition.Single)
            }
        }

        // =========================================================================
        // Section: Camera Diagnostics
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.CAMERA) {
            Text(
                text = stringResource(R.string.camera_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            if (uiState.cameras.isEmpty()) {
                InfoRow(stringResource(R.string.camera_section_title), stringResource(R.string.not_detected), InfoGroupPosition.Single)
            } else {
                uiState.cameras.forEach { camera ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = camera.facing,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f MP", camera.megapixels),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.camera_megapixels), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                Text(camera.resolution, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (camera.apertures.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.camera_apertures), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(camera.apertures.joinToString(", "), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.camera_ois), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                Text(if (camera.oisSupported) stringResource(R.string.supported) else stringResource(R.string.not_supported), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.camera_eis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                Text(if (camera.eisSupported) stringResource(R.string.supported) else stringResource(R.string.not_supported), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.camera_flash), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                Text(if (camera.flashSupported) stringResource(R.string.detected) else stringResource(R.string.not_detected), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (camera.focalLengths.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(stringResource(R.string.camera_focal_lengths), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(camera.focalLengths.joinToString(", "), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // Section: Sensors & Live Interactive Monitor
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.SENSORS) {
            Text(
                text = stringResource(R.string.sensors_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            // Live Sensor Oscilloscope Card
            val activeSensor = uiState.selectedSensor
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.sensors_live_monitor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (activeSensor != null) {
                        Text(
                            text = activeSensor.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Real-time oscilloscope graph
                        SensorOscilloscope(
                            channelHistories = uiState.sensorReadingsHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(8.dp)
                        )

                        // Live Readings
                        val latest = uiState.latestSensorValues
                        if (latest != null && latest.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (latest.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.sensors_reading_x, latest[0]),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E6FF)
                                    )
                                }
                                if (latest.size > 1) {
                                    Text(
                                        text = stringResource(R.string.sensors_reading_y, latest[1]),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                                if (latest.size > 2) {
                                    Text(
                                        text = stringResource(R.string.sensors_reading_z, latest[2]),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                }
                            }
                        }

                        // Sensor Specs
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            InfoRow(stringResource(R.string.sensors_vendor), activeSensor.vendor, InfoGroupPosition.Top)
                            InfoRow(stringResource(R.string.sensors_type), activeSensor.typeString, InfoGroupPosition.Middle)
                            InfoRow(stringResource(R.string.sensors_max_range), "${activeSensor.maxRange}", InfoGroupPosition.Middle)
                            InfoRow(stringResource(R.string.sensors_resolution), "${activeSensor.resolution}", InfoGroupPosition.Middle)
                            InfoRow(stringResource(R.string.sensors_power), "${activeSensor.powerMa} mA", InfoGroupPosition.Middle)
                            InfoRow(stringResource(R.string.sensors_version), "${activeSensor.version}", InfoGroupPosition.Bottom)
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.sensors_select_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Sensors List for selection
            if (uiState.sensors.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.sensors_section_title) + " (${uiState.sensors.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.sensors.forEach { sensor ->
                        val isSelected = uiState.selectedSensor?.name == sensor.name
                        Card(
                            onClick = { onSensorSelected(sensor) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sensor.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = sensor.vendor,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // Section: Network & Connectivity
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.CONNECTIVITY) {
            val conn = uiState.connectivitySpecs
            Text(
                text = stringResource(R.string.conn_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoRow(stringResource(R.string.conn_network_type), conn.networkType, InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.conn_link_speed), "↓ ${conn.linkDownMbps} Mbps • ↑ ${conn.linkUpMbps} Mbps", InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.conn_wifi_ssid), conn.wifiSsid, InfoGroupPosition.Middle)
                if (conn.wifiFrequencyMhz > 0) {
                    InfoRow(stringResource(R.string.conn_wifi_frequency), "${conn.wifiFrequencyMhz} MHz", InfoGroupPosition.Middle)
                }
                if (conn.wifiLinkSpeedMbps > 0) {
                    InfoRow(stringResource(R.string.display_refresh_rate), "${conn.wifiLinkSpeedMbps} Mbps", InfoGroupPosition.Middle)
                }
                if (conn.wifiRssi != 0) {
                    InfoRow(stringResource(R.string.conn_wifi_signal), "${conn.wifiRssi} dBm", InfoGroupPosition.Middle)
                }
                InfoRow(stringResource(R.string.conn_local_ipv4), conn.localIpv4, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.conn_local_ipv6), conn.localIpv6, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.conn_gateway), conn.gateway, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.conn_dns), conn.dnsServers, InfoGroupPosition.Middle)
                InfoRow(stringResource(R.string.conn_subnet_mask), conn.subnetMask, InfoGroupPosition.Bottom)
            }

            Text(
                text = stringResource(R.string.conn_bluetooth_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val btStatus = when {
                    !conn.bluetoothSupported -> stringResource(R.string.not_supported)
                    conn.bluetoothEnabled -> stringResource(R.string.enabled)
                    else -> stringResource(R.string.disabled)
                }
                InfoRow(stringResource(R.string.conn_bluetooth_status), btStatus, InfoGroupPosition.Top)
                InfoRow(stringResource(R.string.conn_bluetooth_le), if (conn.bluetoothLeSupported) stringResource(R.string.supported) else stringResource(R.string.not_supported), InfoGroupPosition.Bottom)
            }
        }

        // =========================================================================
        // Section: Media DRM Diagnostics
        // =========================================================================
        if (selectedCategory == DeviceInfoCategory.ALL || selectedCategory == DeviceInfoCategory.DRM) {
            val drm = uiState.mediaDrmSpecs
            Text(
                text = stringResource(R.string.drm_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.drm_widevine),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = drm.widevineSecurityLevel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow(stringResource(R.string.drm_vendor), drm.widevineVendor, InfoGroupPosition.Top)
                        InfoRow(stringResource(R.string.drm_version), drm.widevineVersion, InfoGroupPosition.Middle)
                        InfoRow(stringResource(R.string.drm_system_id), drm.widevineSystemId, InfoGroupPosition.Middle)
                        InfoRow(stringResource(R.string.drm_algorithms), drm.widevineAlgorithms, InfoGroupPosition.Middle)
                        InfoRow(stringResource(R.string.drm_hdcp_level), drm.widevineHdcpLevel, InfoGroupPosition.Middle)
                        InfoRow(stringResource(R.string.drm_max_hdcp), drm.widevineMaxHdcpLevel, InfoGroupPosition.Bottom)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.drm_clearkey),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow(stringResource(R.string.drm_vendor), drm.clearkeyVendor, InfoGroupPosition.Top)
                        InfoRow(stringResource(R.string.drm_version), drm.clearkeyVersion, InfoGroupPosition.Middle)
                        InfoRow(stringResource(R.string.drm_hdcp_level), drm.clearkeyHdcpLevel, InfoGroupPosition.Middle)
                        InfoRow(stringResource(R.string.drm_max_hdcp), drm.clearkeyMaxHdcpLevel, InfoGroupPosition.Bottom)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}

internal fun formatStorageBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f GB", gb)
    } else {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceInfoDashboardPreview() {
    OsysterTheme {
        DeviceInfoDashboardContent(
            uiState = DeviceInfoUiState(
                batteryState = BatteryState(
                    levelPercentage = 85,
                    tempCelsius = 31.5f,
                    health = "Good",
                    status = "Discharging",
                    voltageMv = 4120,
                    powerSource = "Battery",
                    technology = "Li-ion",
                    currentNowMa = -320,
                    cycleCount = 142,
                    thermalStatus = "None"
                )
            )
        )
    }
}
