package dev.qtremors.osyster.core.hid.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HidReportsTest {

    @Test
    fun `descriptor declares the three report ids`() {
        val d = HidReports.descriptor
        val ids = d.indices.filter { d[it] == 0x85.toByte() }.map { d[it + 1].toInt() }
        assertEquals(listOf(HidReports.REMOTE_ID, HidReports.KEYBOARD_ID, HidReports.MOUSE_ID), ids)
        // Every Collection opens and closes: two for remote and keyboard, three for the mouse.
        assertEquals(d.count { it == 0xA1.toByte() }, d.count { it == 0xC0.toByte() })
    }

    @Test
    fun `remote keys are sent low byte first`() {
        assertArrayEquals(byteArrayOf(0x41, 0x00), HidReports.remote(RemoteKey.SELECT))
        assertArrayEquals(byteArrayOf(0x23, 0x02), HidReports.remote(RemoteKey.HOME))
        assertArrayEquals(byteArrayOf(0x24, 0x02), HidReports.remote(RemoteKey.BACK))
        assertArrayEquals(byteArrayOf(0xE9.toByte(), 0x00), HidReports.remote(RemoteKey.VOLUME_UP))
    }

    @Test
    fun `keyboard report is modifiers then key`() {
        assertArrayEquals(byteArrayOf(Modifiers.SHIFT.toByte(), 0x04), HidReports.keyboard(KeyStroke(0x04, Modifiers.SHIFT)))
        assertArrayEquals(byteArrayOf(0x00, 0x00), HidReports.RELEASE)
    }

    @Test
    fun `mouse movement is clamped to one report`() {
        assertArrayEquals(byteArrayOf(0x01, 127, -127, 0), HidReports.mouse(MouseButtons.LEFT, dx = 500, dy = -500))
        assertArrayEquals(byteArrayOf(0x00, 5, -3, 1), HidReports.mouse(dx = 5, dy = -3, wheel = 1))
    }

    @Test
    fun `us keyboard types letters digits and shifted symbols`() {
        assertEquals(KeyStroke(0x04), UsKeyboard.strokeFor('a'))
        assertEquals(KeyStroke(0x1D, Modifiers.SHIFT), UsKeyboard.strokeFor('Z'))
        assertEquals(KeyStroke(0x27), UsKeyboard.strokeFor('0'))
        assertEquals(KeyStroke(0x1E, Modifiers.SHIFT), UsKeyboard.strokeFor('!'))
        assertEquals(KeyStroke(0x38, Modifiers.SHIFT), UsKeyboard.strokeFor('?'))
        assertEquals(KeyStroke(KeyCodes.SPACE), UsKeyboard.strokeFor(' '))
        assertNull(UsKeyboard.strokeFor('é'))
        assertEquals(5, UsKeyboard.strokesFor("Hi 5!").size)
    }

    @Test
    fun `digits and function keys`() {
        assertEquals(0x1E, KeyCodes.digit(1))
        assertEquals(0x26, KeyCodes.digit(9))
        assertEquals(0x27, KeyCodes.digit(0))
        assertEquals(0x45, KeyCodes.function(12))
    }

    @Test
    fun `automatic layout follows the device kind`() {
        assertEquals(RemoteLayout.TV, layoutFor(LayoutChoice.AUTO, DeviceKind.TV))
        assertEquals(RemoteLayout.COMPUTER, layoutFor(LayoutChoice.AUTO, DeviceKind.COMPUTER))
        assertEquals(RemoteLayout.TABLET, layoutFor(LayoutChoice.AUTO, DeviceKind.PHONE))
        assertEquals(RemoteLayout.TV, layoutFor(LayoutChoice.AUTO, null))
        assertEquals(RemoteLayout.TV, layoutFor(LayoutChoice.TV, DeviceKind.COMPUTER))
    }

    @Test
    fun `computers get keyboard arrows enter and esc`() {
        assertEquals(KeyStroke(KeyCodes.UP), keyboardFor(RemoteLayout.COMPUTER, RemoteKey.UP, enterSelects = false))
        assertEquals(KeyStroke(KeyCodes.ENTER), keyboardFor(RemoteLayout.COMPUTER, RemoteKey.SELECT, enterSelects = false))
        assertEquals(KeyStroke(KeyCodes.ESCAPE), keyboardFor(RemoteLayout.COMPUTER, RemoteKey.BACK, enterSelects = false))
        assertEquals(KeyStroke(0, Modifiers.META), keyboardFor(RemoteLayout.COMPUTER, RemoteKey.HOME, enterSelects = false))
        assertNull(keyboardFor(RemoteLayout.COMPUTER, RemoteKey.VOLUME_UP, enterSelects = false))
        assertNull(keyboardFor(RemoteLayout.TV, RemoteKey.UP, enterSelects = false))
        assertNull(keyboardFor(RemoteLayout.TV, RemoteKey.SELECT, enterSelects = false))
        assertEquals(KeyStroke(KeyCodes.ENTER), keyboardFor(RemoteLayout.TV, RemoteKey.SELECT, enterSelects = true))
    }

    @Test
    fun `layouts hide keys the device has no use for`() {
        assertEquals(true, RemoteLayout.TV.shows(RemoteKey.POWER))
        assertEquals(false, RemoteLayout.COMPUTER.shows(RemoteKey.CHANNEL_UP))
        assertEquals(false, RemoteLayout.COMPUTER.shows(RemoteKey.MENU))
        assertEquals(true, RemoteLayout.TABLET.shows(RemoteKey.MENU))
        assertEquals(false, RemoteLayout.TABLET.shows(RemoteKey.RED))
        assertEquals(true, RemoteLayout.COMPUTER.shows(RemoteKey.PLAY_PAUSE))
    }

    @Test
    fun `swipes pick the stronger axis after the threshold`() {
        assertNull(swipeDirection(2f, 2f, threshold = 5f))
        assertEquals(RemoteKey.RIGHT, swipeDirection(10f, 4f, threshold = 5f))
        assertEquals(RemoteKey.LEFT, swipeDirection(-10f, 4f, threshold = 5f))
        assertEquals(RemoteKey.DOWN, swipeDirection(3f, 9f, threshold = 5f))
        assertEquals(RemoteKey.UP, swipeDirection(3f, -9f, threshold = 5f))
    }
}
