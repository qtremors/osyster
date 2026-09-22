package dev.qtremors.osyster.core.device.data

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.data.format.UnitFormat
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.DevicePageSource
import dev.qtremors.osyster.core.device.domain.DeviceReportSource
import dev.qtremors.osyster.core.device.domain.InfoSection
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * A plain-text report of the device pages, meant for sharing in a forum thread or bug report.
 * Network addresses, the Wi-Fi name, and long inventories (codecs, certificates) are left out.
 */
@Singleton
internal class DeviceReportRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pages: DevicePageSource
) : DeviceReportSource {

    override suspend fun report(): String = buildString {
        appendLine(context.getString(R.string.report_title, Build.MANUFACTURER, Build.MODEL))
        appendLine(context.getString(R.string.report_generated, UnitFormat.dateTime(System.currentTimeMillis())))
        REPORTED_PAGES.forEach { page ->
            val sections = runCatchingCancellable { pages.sections(page).first() }.getOrNull() ?: return@forEach
            appendLine()
            appendLine("== ${context.getString(pageTitle(page))} ==")
            sections.filterNot { it.key in SKIPPED_SECTIONS }.forEach { appendSection(it) }
        }
    }

    private fun StringBuilder.appendSection(section: InfoSection) {
        appendLine()
        appendLine("[${section.title}]")
        section.rows.filterNot { it.key in SKIPPED_ROWS }.forEach { row ->
            val value = when (val v = row.value) {
                is Availability.Available -> v.value.replace("\n", "; ")
                is Availability.PermissionRequired -> context.getString(R.string.report_permission_required)
                is Availability.Restricted -> context.getString(R.string.report_restricted)
                is Availability.Unsupported -> context.getString(R.string.report_not_supported)
                is Availability.Unavailable, is Availability.Failed -> context.getString(R.string.report_not_reported)
            }
            appendLine("${row.label}: $value")
        }
    }

    private fun pageTitle(page: DevicePage): Int = when (page) {
        DevicePage.SYSTEM -> R.string.report_page_system
        DevicePage.DEVICE -> R.string.report_page_device
        DevicePage.PROCESSOR -> R.string.report_page_processor
        DevicePage.MEMORY -> R.string.report_page_memory
        DevicePage.BATTERY -> R.string.report_page_battery
        DevicePage.DISPLAY -> R.string.report_page_display
        DevicePage.CAMERAS -> R.string.report_page_cameras
        DevicePage.SENSORS -> R.string.report_page_sensors
        DevicePage.NETWORK -> R.string.report_page_network
        DevicePage.BLUETOOTH -> R.string.report_page_bluetooth
        DevicePage.MEDIA -> R.string.report_page_media
        DevicePage.CERTIFICATES -> R.string.report_page_certificates
    }

    private companion object {
        val REPORTED_PAGES = DevicePage.entries - DevicePage.CERTIFICATES
        val SKIPPED_SECTIONS = setOf("video_decoders", "video_encoders", "audio_decoders", "audio_encoders", "this_app", "live_clocks")
        val SKIPPED_ROWS = setOf("ssid", "ipv4", "ipv6", "dns", "download", "upload", "hint")
    }
}
