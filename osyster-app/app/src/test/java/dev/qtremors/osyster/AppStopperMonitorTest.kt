package dev.qtremors.osyster

import dev.qtremors.osyster.monitor.ManagedAppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStopperMonitorTest {

    @Test
    fun managedAppInfo_sortingPrioritizesActiveApps() {
        val app1 = ManagedAppInfo("com.test.alpha", "Alpha", null, isStopped = true, isSystemApp = false, isUninstalled = false)
        val app2 = ManagedAppInfo("com.test.beta", "Beta", null, isStopped = false, isSystemApp = false, isUninstalled = false)
        val app3 = ManagedAppInfo("com.test.gamma", "Gamma", null, isStopped = true, isSystemApp = false, isUninstalled = false)
        val app4 = ManagedAppInfo("com.test.delta", "Delta", null, isStopped = false, isSystemApp = false, isUninstalled = false)
        val appGhost = ManagedAppInfo("com.test.ghost", "A-Ghost", null, isStopped = true, isSystemApp = false, isUninstalled = true)

        val unsorted = listOf(app1, appGhost, app2, app3, app4)
        val sorted = unsorted.sortedWith(
            compareBy<ManagedAppInfo> { it.isUninstalled }
                .thenBy { it.isStopped }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )

        // Active apps first: Beta, Delta. Then stopped: Alpha, Gamma. Then ghost: A-Ghost
        assertEquals("Beta", sorted[0].label)
        assertFalse(sorted[0].isStopped)
        assertFalse(sorted[0].isUninstalled)

        assertEquals("Delta", sorted[1].label)
        assertFalse(sorted[1].isStopped)
        assertFalse(sorted[1].isUninstalled)

        assertEquals("Alpha", sorted[2].label)
        assertTrue(sorted[2].isStopped)
        assertFalse(sorted[2].isUninstalled)

        assertEquals("Gamma", sorted[3].label)
        assertTrue(sorted[3].isStopped)
        assertFalse(sorted[3].isUninstalled)

        assertEquals("A-Ghost", sorted[4].label)
        assertTrue(sorted[4].isUninstalled)
    }

    @Test
    fun getFallbackLabel_extractsReadableName() {
        assertEquals("Music", dev.qtremors.osyster.monitor.AppStopperMonitor.getFallbackLabel("com.spotify.music"))
        assertEquals("Browser", dev.qtremors.osyster.monitor.AppStopperMonitor.getFallbackLabel("org.example.browser"))
        assertEquals("Sample", dev.qtremors.osyster.monitor.AppStopperMonitor.getFallbackLabel("com.sample.app"))
    }

    @Test
    fun managedAppInfo_propertiesMatch() {
        val app = ManagedAppInfo(
            packageName = "org.example.browser",
            label = "Privacy Browser",
            icon = null,
            isStopped = false,
            isSystemApp = false,
            isUninstalled = false
        )
        assertEquals("org.example.browser", app.packageName)
        assertEquals("Privacy Browser", app.label)
        assertFalse(app.isStopped)
        assertFalse(app.isSystemApp)
        assertFalse(app.isUninstalled)
    }
}
