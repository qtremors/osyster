package dev.qtremors.osyster.feature.home

import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.batteryStatus
import dev.qtremors.osyster.core.device.domain.batteryTemperatureStatus
import dev.qtremors.osyster.core.device.domain.fullnessStatus
import dev.qtremors.osyster.core.device.domain.loadStatus
import dev.qtremors.osyster.core.device.domain.worst
import dev.qtremors.osyster.core.usage.domain.NetworkFilter
import androidx.compose.material.icons.rounded.Wifi
import dev.qtremors.osyster.core.ui.components.HeroGraphic
import dev.qtremors.osyster.core.ui.components.ChartColors
import dev.qtremors.osyster.core.ui.components.ChartPart
import dev.qtremors.osyster.core.device.domain.HeroTile
import dev.qtremors.osyster.core.device.domain.TemperatureUnit
import dev.qtremors.osyster.core.device.domain.formatTemperature
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.format.UsageFormat
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.DataUsage
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
internal fun heroMetrics(state: HomeState, actions: HomeActions): List<HeroMetric> {
    val summary = state.summary
    val waiting = stringResource(R.string.home_metric_waiting)
    val tiles = state.layout?.visibleTiles ?: HeroTile.entries
    val chart = ChartColors.current()
    return tiles.map { tile ->
        when (tile) {
            HeroTile.CPU -> (summary?.let { cpu(it).copy(graphic = HeroGraphic.Trend(state.trends.cpu), status = state.trends.cpu.lastOrNull()?.let(::loadStatus)) } ?: HeroMetric(stringResource(R.string.home_metric_cpu), waiting, icon = Icons.Rounded.DeveloperBoard))
                .copy(onClick = { actions.onOpenPage(DevicePage.PROCESSOR) })
            HeroTile.RAM -> (summary?.let { ram(it) } ?: HeroMetric(stringResource(R.string.home_metric_ram), waiting, icon = Icons.Rounded.Memory))
                .copy(onClick = { actions.onOpenPage(DevicePage.MEMORY) })
            HeroTile.STORAGE -> (summary?.let { storage(it) } ?: HeroMetric(stringResource(R.string.home_metric_storage), waiting, icon = Icons.Rounded.Storage))
                .copy(onClick = { actions.onOpenPage(DevicePage.MEMORY) })
            HeroTile.BATTERY -> (summary?.let { battery(it, state.unit) } ?: HeroMetric(stringResource(R.string.home_metric_battery), waiting, icon = Icons.Rounded.BatteryStd))
                .copy(onClick = { actions.onOpenPage(DevicePage.BATTERY) })
            HeroTile.SCREEN_TIME -> usageTile(
                label = stringResource(R.string.home_metric_screen),
                value = state.usage.screenMillis?.let(UsageFormat::duration),
                allowed = state.usage.allowed,
                icon = Icons.Rounded.HourglassBottom,
                graphic = HeroGraphic.Bars(state.usage.screenHourly.map(Long::toFloat)),
                onClick = if (state.usage.allowed) ({ actions.onOpenTool(Tool.SCREEN_USAGE) }) else actions.onGrantUsage
            )
            HeroTile.DATA_TODAY -> {
                val traffic = state.usage.traffic
                val network = state.usage.network
                val down = traffic?.received(network) ?: 0L
                val up = traffic?.sent(network) ?: 0L
                usageTile(
                label = stringResource(
                    when (network) {
                        NetworkFilter.WIFI -> R.string.home_metric_data_wifi
                        NetworkFilter.MOBILE -> R.string.home_metric_data_mobile
                        NetworkFilter.ALL -> R.string.home_metric_data
                    }
                ),
                value = traffic?.let { UsageFormat.bytes(it.total(network)) },
                allowed = state.usage.allowed,
                icon = if (network == NetworkFilter.WIFI) Icons.Rounded.Wifi else Icons.Rounded.DataUsage,
                detail = stringResource(R.string.home_metric_data_split, UsageFormat.bytes(down), UsageFormat.bytes(up)),
                graphic = HeroGraphic.Split(listOf(ChartPart(down.toFloat(), chart.download), ChartPart(up.toFloat(), chart.upload))),
                onClick = if (state.usage.allowed) ({ actions.onOpenTool(Tool.DATA_USAGE) }) else actions.onGrantUsage
            )
            }
        }
    }
}

@Composable
private fun usageTile(
    label: String,
    value: String?,
    allowed: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    graphic: HeroGraphic,
    onClick: () -> Unit,
    detail: String? = null
): HeroMetric = HeroMetric(
    label = label,
    value = when {
        !allowed -> stringResource(R.string.home_metric_allow)
        value == null -> stringResource(R.string.home_metric_waiting)
        else -> value
    },
    detail = if (allowed) detail ?: stringResource(R.string.home_metric_today) else stringResource(R.string.home_metric_needs_usage),
    icon = icon,
    graphic = if (allowed) graphic else null,
    onClick = onClick
)

@Composable
private fun cpu(summary: DeviceSummary): HeroMetric {
    val load = summary.cpuLoadPercent.getOrNull()
    val clock = summary.cpuClockMhz.getOrNull()
    return when {
        load != null -> HeroMetric(stringResource(R.string.home_metric_cpu), "$load%", icon = Icons.Rounded.DeveloperBoard)
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
    icon = Icons.Rounded.Memory,
    graphic = { HeroGraphic.Gauge(it) }
)

@Composable
private fun storage(summary: DeviceSummary): HeroMetric = usage(
    label = stringResource(R.string.home_metric_storage),
    used = summary.storageUsedBytes,
    total = summary.storageTotalBytes,
    icon = Icons.Rounded.Storage,
    graphic = { HeroGraphic.Segments(it) }
)

@Composable
private fun battery(summary: DeviceSummary, unit: TemperatureUnit): HeroMetric {
    val percent = summary.batteryPercent.getOrNull()
    val state = stringResource(if (summary.isCharging) R.string.home_metric_charging else R.string.home_metric_on_battery)
    return HeroMetric(
        label = stringResource(R.string.home_metric_battery),
        value = percent?.let { "$it%" } ?: shortLabel(summary.batteryPercent),
        detail = listOfNotNull(summary.batteryTemperatureC?.let { formatTemperature(it, unit) }, state).joinToString(" · "),
        graphic = percent?.let { HeroGraphic.Battery(it / 100f, summary.isCharging) },
        status = percent?.let { level ->
            val charge = batteryStatus(level / 100f, summary.isCharging)
            summary.batteryTemperatureC?.let { worst(charge, batteryTemperatureStatus(it)) } ?: charge
        },
        icon = if (summary.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd
    )
}

@Composable
private fun usage(label: String, used: Availability<Long>, total: Availability<Long>, icon: androidx.compose.ui.graphics.vector.ImageVector, graphic: (Float) -> HeroGraphic): HeroMetric {
    val usedBytes = used.getOrNull()
    val totalBytes = total.getOrNull()
    if (usedBytes == null || totalBytes == null || totalBytes <= 0) {
        return HeroMetric(label, shortLabel(total), icon = icon)
    }
    return HeroMetric(
        label = label,
        value = gigabytes(usedBytes),
        detail = stringResource(R.string.home_metric_of_total, gigabytes(totalBytes)),
        icon = icon,
        graphic = graphic(usedBytes.toFloat() / totalBytes),
        status = fullnessStatus(usedBytes.toFloat() / totalBytes)
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
