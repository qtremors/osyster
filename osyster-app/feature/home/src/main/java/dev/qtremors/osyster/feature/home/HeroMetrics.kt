package dev.qtremors.osyster.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.AvailabilityLabel
import dev.qtremors.osyster.core.device.domain.DeviceSummary
import dev.qtremors.osyster.core.device.domain.getOrNull
import dev.qtremors.osyster.core.device.domain.label
import dev.qtremors.osyster.core.ui.components.HeroMetric
import dev.qtremors.osyster.core.ui.components.availabilityShortText
import java.util.Locale

/**
 * The four live tiles. CPU shows load when Android allows it and the fastest core clock when it
 * does not, labeled so nobody mistakes a clock speed for a load percentage.
 */
@Composable
internal fun heroMetrics(summary: DeviceSummary?, trends: Trends = Trends()): List<HeroMetric> {
    val waiting = stringResource(R.string.home_metric_waiting)
    if (summary == null) {
        return listOf(
            HeroMetric(stringResource(R.string.home_metric_cpu), waiting, icon = Icons.Rounded.DeveloperBoard),
            HeroMetric(stringResource(R.string.home_metric_ram), waiting, icon = Icons.Rounded.Memory),
            HeroMetric(stringResource(R.string.home_metric_storage), waiting, icon = Icons.Rounded.Storage),
            HeroMetric(stringResource(R.string.home_metric_battery), waiting, icon = Icons.Rounded.BatteryStd)
        )
    }
    return listOf(cpu(summary).copy(trend = trends.cpu), ram(summary).copy(trend = trends.ram), storage(summary), battery(summary))
}

@Composable
private fun cpu(summary: DeviceSummary): HeroMetric {
    val load = summary.cpuLoadPercent.getOrNull()
    val clock = summary.cpuClockMhz.getOrNull()
    return when {
        load != null -> HeroMetric(stringResource(R.string.home_metric_cpu), "$load%", progress = load / 100f, icon = Icons.Rounded.DeveloperBoard)
        clock != null -> HeroMetric(
            label = stringResource(R.string.home_metric_cpu_clock),
            value = if (clock >= 1000) String.format(Locale.getDefault(), "%.2f GHz", clock / 1000f) else "$clock MHz",
            detail = stringResource(R.string.home_metric_cpu_load_restricted),
            icon = Icons.Rounded.DeveloperBoard
        )
        else -> HeroMetric(stringResource(R.string.home_metric_cpu), shortLabel(summary.cpuLoadPercent), icon = Icons.Rounded.DeveloperBoard)
    }
}

@Composable
private fun ram(summary: DeviceSummary): HeroMetric = usage(
    label = stringResource(R.string.home_metric_ram),
    used = summary.ramUsedBytes,
    total = summary.ramTotalBytes,
    icon = Icons.Rounded.Memory
)

@Composable
private fun storage(summary: DeviceSummary): HeroMetric = usage(
    label = stringResource(R.string.home_metric_storage),
    used = summary.storageUsedBytes,
    total = summary.storageTotalBytes,
    icon = Icons.Rounded.Storage
)

@Composable
private fun battery(summary: DeviceSummary): HeroMetric {
    val percent = summary.batteryPercent.getOrNull()
    return HeroMetric(
        label = stringResource(R.string.home_metric_battery),
        value = percent?.let { "$it%" } ?: shortLabel(summary.batteryPercent),
        detail = stringResource(if (summary.isCharging) R.string.home_metric_charging else R.string.home_metric_on_battery),
        progress = percent?.div(100f),
        icon = if (summary.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd
    )
}

@Composable
private fun usage(label: String, used: Availability<Long>, total: Availability<Long>, icon: androidx.compose.ui.graphics.vector.ImageVector): HeroMetric {
    val usedBytes = used.getOrNull()
    val totalBytes = total.getOrNull()
    if (usedBytes == null || totalBytes == null || totalBytes <= 0) {
        return HeroMetric(label, shortLabel(total), icon = icon)
    }
    return HeroMetric(
        label = label,
        value = gigabytes(usedBytes),
        detail = stringResource(R.string.home_metric_of_total, gigabytes(totalBytes)),
        progress = usedBytes.toFloat() / totalBytes,
        icon = icon
    )
}

@Composable
private fun shortLabel(value: Availability<*>): String =
    availabilityShortText(value.label ?: AvailabilityLabel.NOT_REPORTED)

/** Hero tiles are narrow: one decimal in GB is enough ("5.1 GB"). */
private fun gigabytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 100) String.format(Locale.getDefault(), "%.0f GB", gb) else String.format(Locale.getDefault(), "%.1f GB", gb)
}
