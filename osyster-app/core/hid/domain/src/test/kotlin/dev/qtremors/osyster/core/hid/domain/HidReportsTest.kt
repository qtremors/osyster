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
        assertEquals(listOf(HidReports.REMOTE_ID, HidReports.KEYBOARD_ID, HidReports.MOUSE_ID, HidReports.SYSTEM_ID, HidReports.GAMEPAD_ID), ids)
        // Every Collection opens and closes.
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
    fun `system and gamepad reports`() {
        assertArrayEquals(byteArrayOf(0x02), HidReports.system(SystemKey.SLEEP))
        assertArrayEquals(byteArrayOf(0x00), HidReports.system(null))
        val pad = GamepadState().with(GamepadButton.A, true).with(GamepadButton.START, true).copy(hat = Hat.LEFT, leftX = 1f, rightY = -1f)
        assertArrayEquals(byteArrayOf(0x01, 0x08, 6, 127, 0, 0, -127), HidReports.gamepad(pad))
        assertArrayEquals(byteArrayOf(0, 0, 8, 0, 0, 0, 0), HidReports.gamepad(GamepadState()))
    }

    @Test
    fun `hat follows the held arrows`() {
        assertNull(hatFor(up = false, down = false, left = false, right = false))
        assertEquals(Hat.UP, hatFor(up = true, down = false, left = false, right = false))
        assertEquals(Hat.DOWN_LEFT, hatFor(up = false, down = true, left = true, right = false))
        assertNull(hatFor(up = true, down = true, left = false, right = false))
    }

    @Test
    fun `pc actions are the expected shortcuts`() {
        assertEquals(listOf(MacroStep.Keys(KeyStroke(0x0F, Modifiers.META))), PcAction.LOCK.steps)
        assertEquals(MacroStep.Keys(KeyStroke(0x15)), PcAction.RESTART.steps.last())
        assertEquals(MacroStep.Keys(KeyStroke(0x18)), PcAction.SHUT_DOWN.steps.last())
        assertEquals(true, PcAction.SHUT_DOWN.confirm)
    }

    @Test
    fun `typing sends only what changed at the end`() {
        assertEquals(TypingChange(0, "h"), typingChange("", "h"))
        assertEquals(TypingChange(0, " world"), typingChange("hello", "hello world"))
        assertEquals(TypingChange(1, ""), typingChange("hello", "hell"))
        // Autocorrect replacing the last word: delete back to where it differs, type the rest.
        assertEquals(TypingChange(2, "ere"), typingChange("I am thre", "I am there"))
        assertEquals(TypingChange(0, ""), typingChange("same", "same"))
    }

    @Test
    fun `computers open on the touchpad, the rest on the remote`() {
        assertEquals(RemoteInput.TOUCHPAD, defaultInput(RemoteLayout.COMPUTER))
        assertEquals(RemoteInput.REMOTE, defaultInput(RemoteLayout.TV))
        assertEquals(RemoteInput.REMOTE, defaultInput(RemoteLayout.TABLET))
    }

    @Test
    fun `air mouse ignores a resting hand`() {
        assertEquals(0f to 0f, airMouseDelta(0.01f, -0.02f, speed = 1f))
        assertEquals(-5f to 2f, airMouseDelta(-0.2f, 0.5f, speed = 1f))
    }

    @Test
    fun `slow pointer moves add up instead of being lost`() {
        val pointer = PointerAccumulator()
        // Four moves of 0.4 px at speed 1: nothing, nothing, then one unit, then nothing.
        val reports = (1..4).flatMap { pointer.move(0.4f, 0f, speed = 1f) }
        assertEquals(listOf(1 to 0), reports)
    }

    @Test
    fun `fast pointer moves split over several reports`() {
        val pointer = PointerAccumulator()
        assertEquals(listOf(127 to -10, 73 to 0), pointer.move(200f, -10f, speed = 1f))
        assertEquals(listOf(4 to 2), pointer.move(2f, 1f, speed = 2f))
    }

    @Test
    fun `scrolling follows the fingers unless inverted`() {
        val scroll = ScrollAccumulator(stepPx = 20f)
        assertEquals(0, scroll.scroll(15f, speed = 1f, invert = false))
        assertEquals(1, scroll.scroll(10f, speed = 1f, invert = false))
        assertEquals(-2, ScrollAccumulator(20f).scroll(40f, speed = 1f, invert = true))
        assertEquals(-1, ScrollAccumulator(20f).scroll(-20f, speed = 0.5f, invert = false))
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
