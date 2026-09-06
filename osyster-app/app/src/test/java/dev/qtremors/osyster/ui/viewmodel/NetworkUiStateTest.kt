package dev.qtremors.osyster.ui.viewmodel

import dev.qtremors.osyster.monitor.AppNetworkUsage
import dev.qtremors.osyster.monitor.NetworkInterval
import dev.qtremors.osyster.monitor.NetworkUsageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkUiStateTest {

    @Test
    fun filteredApps_emptyQuery_returnsAll() {
        val app1 = AppNetworkUsage(1001, "com.example.one", "One", 100L, 200L, 300L)
        val app2 = AppNetworkUsage(1002, "com.example.two", "Two", 400L, 500L, 900L)
        val summary = NetworkUsageSummary(0, 0, 0, 0, 0, emptyList(), listOf(app1, app2))
        val state = NetworkUiState(summary = summary, searchQuery = "")

        assertEquals(2, state.filteredApps.size)
    }

    @Test
    fun filteredApps_filtersByAppNameCaseInsensitive() {
        val app1 = AppNetworkUsage(1001, "com.example.browser", "Web Browser", 100L, 200L, 300L)
        val app2 = AppNetworkUsage(1002, "com.example.player", "Media Player", 400L, 500L, 900L)
        val summary = NetworkUsageSummary(0, 0, 0, 0, 0, emptyList(), listOf(app1, app2))
        val state = NetworkUiState(summary = summary, searchQuery = "BROWSER")

        assertEquals(1, state.filteredApps.size)
        assertEquals("com.example.browser", state.filteredApps.first().packageName)
    }

    @Test
    fun dateLabel_forDayInterval_todayStartsFormatted() {
        val state = NetworkUiState(
            selectedInterval = NetworkInterval.DAY,
            targetDateMillis = System.currentTimeMillis()
        )
        assertTrue(state.dateLabel.startsWith("Today"))
    }
}
