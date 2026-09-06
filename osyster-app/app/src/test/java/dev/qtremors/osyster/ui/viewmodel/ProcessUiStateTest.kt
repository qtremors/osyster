package dev.qtremors.osyster.ui.viewmodel

import dev.qtremors.osyster.monitor.ProcessInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessUiStateTest {

    @Test
    fun filteredProcesses_emptyQuery_returnsAll() {
        val p1 = ProcessInfo(pid = 100, name = "system_server", ramKb = 50000)
        val p2 = ProcessInfo(pid = 200, name = "com.android.chrome", ramKb = 120000)
        val state = ProcessUiState(rawProcesses = listOf(p1, p2), searchQuery = "")

        assertEquals(2, state.filteredProcesses.size)
    }

    @Test
    fun filteredProcesses_filtersByNameCaseInsensitive() {
        val p1 = ProcessInfo(pid = 100, name = "system_server", ramKb = 50000)
        val p2 = ProcessInfo(pid = 200, name = "com.android.chrome", ramKb = 120000)
        val state = ProcessUiState(rawProcesses = listOf(p1, p2), searchQuery = "CHROME")

        assertEquals(1, state.filteredProcesses.size)
        assertEquals("com.android.chrome", state.filteredProcesses.first().name)
    }

    @Test
    fun filteredProcesses_filtersByPid() {
        val p1 = ProcessInfo(pid = 100, name = "system_server", ramKb = 50000)
        val p2 = ProcessInfo(pid = 200, name = "com.android.chrome", ramKb = 120000)
        val state = ProcessUiState(rawProcesses = listOf(p1, p2), searchQuery = "100")

        assertEquals(1, state.filteredProcesses.size)
        assertEquals(100, state.filteredProcesses.first().pid)
    }

    @Test
    fun processInfo_retainsResolvedUser() {
        val p1 = ProcessInfo(pid = 1, name = "init", ramKb = 2048, user = "root")
        val p2 = ProcessInfo(pid = 1000, name = "system_server", ramKb = 150000, user = "system")
        val state = ProcessUiState(rawProcesses = listOf(p1, p2), showKernelThreads = true)

        assertEquals("root", state.rawProcesses[0].user)
        assertEquals("system", state.rawProcesses[1].user)
        assertEquals(true, state.showKernelThreads)
    }
}
