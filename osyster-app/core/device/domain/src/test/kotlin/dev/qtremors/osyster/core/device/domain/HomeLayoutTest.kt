package dev.qtremors.osyster.core.device.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test
    fun pinKeysRoundTrip() {
        val key = PinKey.tool("data_usage")
        assertEquals(key, PinKey.parse(key.raw))
        assertNull(PinKey.parse("widget:x"))
        assertNull(PinKey.parse("page:"))
        assertNull(PinKey.parse("nonsense"))
    }

    @Test
    fun togglePinAddsThenRemoves() {
        val key = PinKey.page("battery")
        val pinned = HomeLayout().togglePin(key)
        assertTrue(pinned.isPinned(key))
        assertFalse(pinned.togglePin(key).isPinned(key))
    }

    @Test
    fun moveSwapsWithinGroupOnly() {
        val moved = HomeLayout().move(DevicePage.MEMORY, up = true)
        assertEquals(DevicePage.MEMORY, moved.pageOrder[2])
        assertEquals(DevicePage.PROCESSOR, moved.pageOrder[3])
        // PROCESSOR is first in its group; moving it up must not cross into ABOUT.
        val layout = HomeLayout()
        assertSame(layout, layout.move(DevicePage.PROCESSOR, up = true))
    }

    @Test
    fun hidingAPageUnpinsIt() {
        val layout = HomeLayout().togglePin(PinKey.page("sensors")).hide(DevicePage.SENSORS)
        assertFalse(DevicePage.SENSORS in layout.visiblePages)
        assertFalse(layout.isPinned(PinKey.page("sensors")))
        assertTrue(DevicePage.SENSORS in layout.show(DevicePage.SENSORS).visiblePages)
    }
}
