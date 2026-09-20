package dev.qtremors.osyster.core.device.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DevicePageTest {
    @Test
    fun idsAreUnique() {
        val ids = DevicePage.entries.map(DevicePage::id)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun restoreOrderKeepsSavedOrderAndAppendsNewPages() {
        val restored = DevicePage.restoreOrder(listOf("battery", "gone_page", "system", "battery"))
        assertEquals(DevicePage.BATTERY, restored[0])
        assertEquals(DevicePage.SYSTEM, restored[1])
        assertEquals(DevicePage.entries.size, restored.size)
    }

    @Test
    fun unknownIdIsNull() {
        assertNull(DevicePage.fromId("cpu_gpu"))
    }
}
