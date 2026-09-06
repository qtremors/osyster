@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.R
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.SystemMonitor
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import dev.qtremors.osyster.ui.theme.OsysterTheme
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoUiState
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoViewModel
import androidx.compose.ui.tooling.preview.Preview
import dev.qtremors.osyster.ui.util.collectAsVisibleState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

// =========================================================================
// Section Comment: Device Information & Battery Health Diagnostics
// =========================================================================

enum class InfoGroupPosition {
    Top, Middle, Bottom, Single
}

@Composable
fun InfoGroupShape(position: InfoGroupPosition): RoundedCornerShape {
    return when (position) {
        InfoGroupPosition.Top -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        InfoGroupPosition.Middle -> RoundedCornerShape(8.dp)
        InfoGroupPosition.Bottom -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        InfoGroupPosition.Single -> RoundedCornerShape(24.dp)
    }
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
fun DeviceInfoDashboard(
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
        modifier = modifier
    )
}

@Composable
fun DeviceInfoDashboardContent(
    uiState: DeviceInfoUiState,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val batteryState = uiState.batteryState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Top Header Row: Back & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = stringResource(R.string.device_specs_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Battery level dial card
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

        // Section Title: Battery details
        Text(
            text = stringResource(R.string.device_battery_specs),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        // Segmented list for Battery Specs
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.cpu_temperature), if (batteryState.tempCelsius > 0f) uiState.temperatureUnit.format(batteryState.tempCelsius) else stringResource(R.string.not_applicable), InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.device_voltage), "${batteryState.voltageMv} mV", InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_power_connection), batteryState.powerSource, InfoGroupPosition.Bottom)
        }

        // Section Title: Hardware Specs
        Text(
            text = stringResource(R.string.device_hardware_specs),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        // Segmented list for Hardware Specs
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.device_manufacturer), uiState.manufacturer, InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.device_model), uiState.model, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_board_hardware), uiState.board, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_processor_platform), uiState.hardware, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_supported_architectures), uiState.supportedAbis, InfoGroupPosition.Bottom)
        }

        // Section Title: System Software OS
        Text(
            text = stringResource(R.string.device_android_specs),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        // Segmented list for OS Specs
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow(stringResource(R.string.device_android_version), uiState.androidVersion, InfoGroupPosition.Top)
            InfoRow(stringResource(R.string.device_api_level), uiState.apiLevel, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_security_patch), uiState.securityPatch, InfoGroupPosition.Middle)
            InfoRow(stringResource(R.string.device_bootloader_release), uiState.bootloader, InfoGroupPosition.Bottom)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceInfoDashboardPreview() {
    OsysterTheme {
        DeviceInfoDashboardContent(
            uiState = DeviceInfoUiState(
                batteryState = dev.qtremors.osyster.monitor.BatteryState(
                    levelPercentage = 85,
                    tempCelsius = 31.5f,
                    health = "Good",
                    status = "Discharging",
                    voltageMv = 4120,
                    powerSource = "Battery"
                ),
                manufacturer = "Google",
                model = "Pixel 8 Pro",
                board = "husky",
                hardware = "tensor_g3",
                supportedAbis = "arm64-v8a",
                androidVersion = "14",
                apiLevel = "34",
                securityPatch = "2026-08-05",
                bootloader = "husky-1.0"
            )
        )
    }
}

