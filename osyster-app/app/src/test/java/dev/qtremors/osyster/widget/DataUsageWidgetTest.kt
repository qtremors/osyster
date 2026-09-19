package dev.qtremors.osyster.widget

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import dev.qtremors.osyster.monitor.NetworkMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DataUsageWidgetTest {

    @Test
    fun widgetConstants_areConfiguredCorrectly() {
        assertEquals("dev.qtremors.osyster.action.REFRESH_DAY_WIDGET", DataUsageWidgetUpdater.ACTION_REFRESH_DAY)
        assertEquals("dev.qtremors.osyster.action.REFRESH_MONTH_WIDGET", DataUsageWidgetUpdater.ACTION_REFRESH_MONTH)
        assertEquals("dev.qtremors.osyster.action.REFRESH_COMBINED_WIDGET", DataUsageWidgetUpdater.ACTION_REFRESH_COMBINED)
        assertEquals("target_screen", DataUsageWidgetUpdater.EXTRA_TARGET_SCREEN)
        assertEquals("network", DataUsageWidgetUpdater.TARGET_NETWORK)
    }

    @Test
    fun referenceWidgetFormatting_matchesExpectedOutputs() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)

            // Month reference values: 31.55 GB, 9.22 GB
            val monthMobileBytes = (31.55 * 1024 * 1024 * 1024).toLong()
            val monthWifiBytes = (9.22 * 1024 * 1024 * 1024).toLong()
            assertEquals("31.55 GB", NetworkMonitor.formatBytes(monthMobileBytes))
            assertEquals("9.22 GB", NetworkMonitor.formatBytes(monthWifiBytes))

            // Day reference values: 1004.63 MB, 271.23 MB
            val dayMobileBytes = (1004.63 * 1024 * 1024).toLong()
            val dayWifiBytes = (271.23 * 1024 * 1024).toLong()
            assertEquals("1004.63 MB", NetworkMonitor.formatBytes(dayMobileBytes))
            assertEquals("271.23 MB", NetworkMonitor.formatBytes(dayWifiBytes))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
