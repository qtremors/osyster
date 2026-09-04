package dev.qtremors.osyster

import dev.qtremors.osyster.monitor.ManagedAppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStopperMonitorTest {

    @Test
    fun managedAppInfo_sortingPrioritizesActiveApps() {
        val app1 = ManagedAppInfo("com.test.alpha", "Alpha", null, isStopped = true, isSystemApp = false)
        val app2 = ManagedAppInfo("com.test.beta", "Beta", null, isStopped = false, isSystemApp = false)
        val app3 = ManagedAppInfo("com.test.gamma", "Gamma", null, isStopped = true, isSystemApp = false)
        val app4 = ManagedAppInfo("com.test.delta", "Delta", null, isStopped = false, isSystemApp = false)

        val unsorted = listOf(app1, app2, app3, app4)
        val sorted = unsorted.sortedWith(
            compareBy<ManagedAppInfo> { it.isStopped }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )

        // Active apps should come first, then sorted alphabetically: Beta, Delta, Alpha, Gamma
        assertEquals("Beta", sorted[0].label)
        assertFalse(sorted[0].isStopped)
        assertEquals("Delta", sorted[1].label)
        assertFalse(sorted[1].isStopped)
        assertEquals("Alpha", sorted[2].label)
        assertTrue(sorted[2].isStopped)
        assertEquals("Gamma", sorted[3].label)
        assertTrue(sorted[3].isStopped)
    }

    @Test
    fun managedAppInfo_propertiesMatch() {
        val app = ManagedAppInfo(
            packageName = "org.example.browser",
            label = "Privacy Browser",
            icon = null,
            isStopped = false,
            isSystemApp = false
        )
        assertEquals("org.example.browser", app.packageName)
        assertEquals("Privacy Browser", app.label)
        assertFalse(app.isStopped)
        assertFalse(app.isSystemApp)
    }
}
