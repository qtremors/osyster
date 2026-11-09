package dev.qtremors.osyster.core.privilege.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietLaunchTest {
    private class FakeSettings(var writable: Boolean = true) : SystemSettings {
        val values = mutableMapOf<SettingKey, String?>()
        val writes = mutableListOf<SettingKey>()
        override suspend fun read(key: SettingKey): String? = values[key]
        override fun changes(keys: Set<SettingKey>): Flow<Unit> = emptyFlow()
        override suspend fun write(key: SettingKey, value: String?): Boolean {
            if (!writable) return false
            writes += key
            values[key] = value
            return true
        }
    }

    private class FakeStore : PowerToolsStore {
        override val quietApps = MutableStateFlow<Set<String>>(emptySet())
        override fun setQuietApps(apps: Set<String>) { quietApps.value = apps }
        override val freezeList = MutableStateFlow<Set<String>>(emptySet())
        override fun setFreezeList(apps: Set<String>) { freezeList.value = apps }
        private val snapshot = MutableStateFlow<DebugSnapshot?>(null)
        override val debugSnapshot: StateFlow<DebugSnapshot?> = snapshot
        override fun setDebugSnapshot(snapshot: DebugSnapshot?) { this.snapshot.value = snapshot }
    }

    @Test
    fun hideTurnsEverythingOffAndRestorePutsItBack() = runTest {
        val settings = FakeSettings().apply {
            values[Keys.DeveloperOptions] = "1"
            values[Keys.UsbDebugging] = "1"
            values[Keys.WirelessDebugging] = "0"
        }
        val store = FakeStore()
        val quiet = QuietLaunch(settings, store)

        assertTrue(quiet.hide())
        assertEquals("0", settings.values[Keys.DeveloperOptions])
        assertEquals("0", settings.values[Keys.UsbDebugging])
        assertEquals(DebugSnapshot(true, true, false), store.debugSnapshot.value)

        assertTrue(quiet.restore())
        assertEquals("1", settings.values[Keys.DeveloperOptions])
        assertEquals("1", settings.values[Keys.UsbDebugging])
        assertEquals("0", settings.values[Keys.WirelessDebugging])
        assertNull(store.debugSnapshot.value)
    }

    @Test
    fun secondHideKeepsFirstSnapshot() = runTest {
        val settings = FakeSettings().apply { values[Keys.DeveloperOptions] = "1" }
        val store = FakeStore()
        val quiet = QuietLaunch(settings, store)
        quiet.hide()
        settings.values[Keys.UsbDebugging] = "1"
        quiet.hide()
        assertEquals(DebugSnapshot(true, false, false), store.debugSnapshot.value)
    }

    @Test
    fun nothingOnMeansNothingWritten() = runTest {
        val settings = FakeSettings()
        val quiet = QuietLaunch(settings, FakeStore())
        assertTrue(quiet.hide())
        assertTrue(settings.writes.isEmpty())
        assertFalse(quiet.restore())
    }

    @Test
    fun failedRestoreKeepsSnapshot() = runTest {
        val settings = FakeSettings().apply { values[Keys.DeveloperOptions] = "1" }
        val store = FakeStore()
        val quiet = QuietLaunch(settings, store)
        quiet.hide()
        settings.writable = false
        assertFalse(quiet.restore())
        assertEquals(DebugSnapshot(true, false, false), store.debugSnapshot.value)
    }

    @Test
    fun debuggingIsTurnedOffBeforeDeveloperOptions() = runTest {
        val settings = FakeSettings().apply {
            values[Keys.DeveloperOptions] = "1"
            values[Keys.UsbDebugging] = "1"
        }
        QuietLaunch(settings, FakeStore()).hide()
        assertEquals(listOf(Keys.WirelessDebugging, Keys.UsbDebugging, Keys.DeveloperOptions), settings.writes)
    }
}
