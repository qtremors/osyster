package dev.qtremors.osyster.ui.viewmodel

import dev.qtremors.osyster.monitor.ManagedAppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStopperUiStateTest {

    @Test
    fun counts_computeCorrectly() {
        val app1 = ManagedAppInfo("com.test.alpha", "Alpha", null, isStopped = false, isSystemApp = false, isUninstalled = false)
        val app2 = ManagedAppInfo("com.test.beta", "Beta", null, isStopped = true, isSystemApp = false, isUninstalled = false)
        val app3 = ManagedAppInfo("com.test.ghost", "Ghost", null, isStopped = true, isSystemApp = false, isUninstalled = true)
        val app4 = ManagedAppInfo("com.test.gamma", "Gamma", null, isStopped = false, isSystemApp = false, isUninstalled = false)

        val state = AppStopperUiState(managedApps = listOf(app1, app2, app3, app4))

        assertEquals(2, state.activeCount)
        assertEquals(1, state.stoppedCount)
        assertEquals(1, state.uninstalledCount)
    }

    @Test
    fun filteredApps_filtersByLabelCaseInsensitive() {
        val app1 = ManagedAppInfo("com.test.alpha", "Alpha", null, isStopped = false, isSystemApp = false, isUninstalled = false)
        val app2 = ManagedAppInfo("com.test.beta", "Beta", null, isStopped = true, isSystemApp = false, isUninstalled = false)

        val state = AppStopperUiState(managedApps = listOf(app1, app2), searchQuery = "BETA")

        assertEquals(1, state.filteredApps.size)
        assertEquals("Beta", state.filteredApps.first().label)
    }
}
