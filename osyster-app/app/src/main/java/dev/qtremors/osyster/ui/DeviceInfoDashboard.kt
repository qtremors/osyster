package dev.qtremors.osyster.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.monitor.BatteryState
import dev.qtremors.osyster.monitor.SystemMonitor
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
fun DeviceInfoDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var batteryState by remember { mutableStateOf(SystemMonitor.getBatteryState(context)) }

    LaunchedEffect(Unit) {
        SystemMonitor.streamBattery(context, 3000L).collectLatest { state ->
            batteryState = state
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = when {
                            batteryState.status == "Charging" -> MaterialTheme.colorScheme.primary
                            batteryState.levelPercentage < 20 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
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
                        text = "Battery Status",
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
                        text = "Health: ${batteryState.health}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Section Title: Battery details
        Text(
            text = "Battery Specifications",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        // Segmented list for Battery Specs
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow("Temperature", String.format(Locale.getDefault(), "%.1f °C", batteryState.tempCelsius), InfoGroupPosition.Top)
            InfoRow("Voltage", "${batteryState.voltageMv} mV", InfoGroupPosition.Middle)
            InfoRow("Power Connection", batteryState.powerSource, InfoGroupPosition.Bottom)
        }

        // Section Title: Hardware Specs
        Text(
            text = "Hardware Specifications",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        // Segmented list for Hardware Specs
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow("Manufacturer", android.os.Build.MANUFACTURER, InfoGroupPosition.Top)
            InfoRow("Device Model", android.os.Build.MODEL, InfoGroupPosition.Middle)
            InfoRow("Board Hardware", android.os.Build.BOARD, InfoGroupPosition.Middle)
            InfoRow("Processor Platform", android.os.Build.HARDWARE, InfoGroupPosition.Middle)
            InfoRow("Supported Architectures", android.os.Build.SUPPORTED_ABIS.joinToString(", "), InfoGroupPosition.Bottom)
        }

        // Section Title: System Software OS
        Text(
            text = "Android System Specs",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        // Segmented list for OS Specs
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow("Android Version", android.os.Build.VERSION.RELEASE, InfoGroupPosition.Top)
            InfoRow("API Level", android.os.Build.VERSION.SDK_INT.toString(), InfoGroupPosition.Middle)
            InfoRow("Security Patch", android.os.Build.VERSION.SECURITY_PATCH, InfoGroupPosition.Middle)
            InfoRow("Bootloader Release", android.os.Build.BOOTLOADER, InfoGroupPosition.Bottom)
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}
