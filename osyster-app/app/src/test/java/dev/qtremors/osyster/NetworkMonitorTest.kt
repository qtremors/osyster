package dev.qtremors.osyster

import dev.qtremors.osyster.monitor.AppNetworkUsage
import dev.qtremors.osyster.monitor.NetworkBucket
import dev.qtremors.osyster.monitor.NetworkInterfaceFilter
import dev.qtremors.osyster.monitor.NetworkInterval
import dev.qtremors.osyster.monitor.NetworkMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMonitorTest {

    @Test
    fun formatBytes_formatsVariousMagnitudesCorrectly() {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.US)
            assertEquals("500 B", NetworkMonitor.formatBytes(500L))
            assertEquals("1.00 KB", NetworkMonitor.formatBytes(1024L))
            assertEquals("1.50 KB", NetworkMonitor.formatBytes(1536L))
            assertEquals("1.00 MB", NetworkMonitor.formatBytes(1024L * 1024L))
            assertEquals("849.77 MB", NetworkMonitor.formatBytes((849.77 * 1024 * 1024).toLong()))
            assertEquals("1.50 GB", NetworkMonitor.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))

            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("500 B", NetworkMonitor.formatBytes(500L))
            assertEquals("1,00 KB", NetworkMonitor.formatBytes(1024L))
            assertEquals("1,50 KB", NetworkMonitor.formatBytes(1536L))
            assertEquals("1,50 GB", NetworkMonitor.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun splitBytesAndUnit_splitsNumberAndUnitAccurately() {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.US)
            val (numB, unitB) = NetworkMonitor.splitBytesAndUnit(420L)
            assertEquals("420", numB)
            assertEquals("B", unitB)

            val (numKB, unitKB) = NetworkMonitor.splitBytesAndUnit(2048L)
            assertEquals("2.00", numKB)
            assertEquals("KB", unitKB)

            val (numMB, unitMB) = NetworkMonitor.splitBytesAndUnit((846.08 * 1024 * 1024).toLong())
            assertEquals("846.08", numMB)
            assertEquals("MB", unitMB)

            val (numGB, unitGB) = NetworkMonitor.splitBytesAndUnit((4.25 * 1024 * 1024 * 1024).toLong())
            assertEquals("4.25", numGB)
            assertEquals("GB", unitGB)

            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            val (numDeKB, unitDeKB) = NetworkMonitor.splitBytesAndUnit(2048L)
            assertEquals("2,00", numDeKB)
            assertEquals("KB", unitDeKB)

            val (numDeGB, unitDeGB) = NetworkMonitor.splitBytesAndUnit((4.25 * 1024 * 1024 * 1024).toLong())
            assertEquals("4,25", numDeGB)
            assertEquals("GB", unitDeGB)
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun formatSpeed_appendsPerSecond() {
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.US)
            assertEquals("120.00 KB/s", NetworkMonitor.formatSpeed(120L * 1024L))
            assertEquals("1.50 MB/s", NetworkMonitor.formatSpeed((1.5 * 1024 * 1024).toLong()))

            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("120,00 KB/s", NetworkMonitor.formatSpeed(120L * 1024L))
            assertEquals("1,50 MB/s", NetworkMonitor.formatSpeed((1.5 * 1024 * 1024).toLong()))
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun networkInterval_entriesExist() {
        assertEquals(3, NetworkInterval.entries.size)
        assertTrue(NetworkInterval.entries.contains(NetworkInterval.DAY))
        assertTrue(NetworkInterval.entries.contains(NetworkInterval.WEEK))
        assertTrue(NetworkInterval.entries.contains(NetworkInterval.MONTH))
    }

    @Test
    fun networkInterfaceFilter_entriesExist() {
        assertEquals(3, NetworkInterfaceFilter.entries.size)
        assertTrue(NetworkInterfaceFilter.entries.contains(NetworkInterfaceFilter.ALL))
        assertTrue(NetworkInterfaceFilter.entries.contains(NetworkInterfaceFilter.MOBILE))
        assertTrue(NetworkInterfaceFilter.entries.contains(NetworkInterfaceFilter.WIFI))
    }

    @Test
    fun emptySummary_hasZeroMetrics() {
        val summary = NetworkMonitor.emptySummary()
        assertEquals(0L, summary.downloadBytes)
        assertEquals(0L, summary.uploadBytes)
        assertEquals(0L, summary.mobileBytes)
        assertEquals(0L, summary.wifiBytes)
        assertEquals(0L, summary.totalBytes)
        assertTrue(summary.timeline.isEmpty())
        assertTrue(summary.apps.isEmpty())
    }

    @Test
    fun networkBucket_holdsCorrectMetrics() {
        val bucket = NetworkBucket(
            startTimeMillis = 1000L,
            endTimeMillis = 2000L,
            rxBytes = 800L,
            txBytes = 200L,
            totalBytes = 1000L,
            label = "12 pm"
        )
        assertEquals(1000L, bucket.startTimeMillis)
        assertEquals(2000L, bucket.endTimeMillis)
        assertEquals(800L, bucket.rxBytes)
        assertEquals(200L, bucket.txBytes)
        assertEquals(1000L, bucket.totalBytes)
        assertEquals("12 pm", bucket.label)
    }

    @Test
    fun appNetworkUsage_holdsCorrectAttribution() {
        val app = AppNetworkUsage(
            uid = 10042,
            packageName = "org.example.app",
            appName = "Example App",
            rxBytes = 5000000L,
            txBytes = 1000000L,
            totalBytes = 6000000L,
            icon = null
        )
        assertEquals(10042, app.uid)
        assertEquals("org.example.app", app.packageName)
        assertEquals("Example App", app.appName)
        assertEquals(5000000L, app.rxBytes)
        assertEquals(1000000L, app.txBytes)
        assertEquals(6000000L, app.totalBytes)
    }
}
