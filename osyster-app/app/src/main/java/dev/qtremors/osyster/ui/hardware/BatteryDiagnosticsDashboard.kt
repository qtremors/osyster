@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.hardware

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.qtremors.osyster.R
import dev.qtremors.osyster.ui.util.LocalBottomContentPadding
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import dev.qtremors.osyster.ui.util.collectAsVisibleState
import dev.qtremors.osyster.ui.viewmodel.DeviceInfoViewModel

@Composable
fun BatteryDiagnosticsDashboard(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsVisibleState()
    val view = LocalView.current
    val batteryState = uiState.batteryState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Main Battery Card
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

        // Detailed Battery Specs
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

        Spacer(modifier = Modifier.height(LocalBottomContentPadding.current))
    }
}
