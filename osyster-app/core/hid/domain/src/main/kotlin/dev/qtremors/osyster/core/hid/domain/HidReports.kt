package dev.qtremors.osyster.core.hid.domain

/**
 * What the phone tells a TV or computer it is when it registers over Bluetooth: one device with
 * five parts, each with its own report ID. The first three are adapted from BT Remote by Atharok
 * (GPLv3).
 *
 * - Remote keys (Consumer page): one 16-bit usage, for D-pad, OK, Home, Back, media, volume, and
 *   app keys such as the browser.
 * - Keyboard: a modifier byte and one key code.
 * - Mouse: three buttons, then X, Y, and wheel as signed bytes.
 * - System control: the power and sleep keys some keyboards have.
 * - Gamepad: 16 buttons, a hat switch (the D-pad), and two sticks.
 *
 * A host reads this when it pairs, so after it changes a device may need pairing again before the
 * new parts work.
 *
 * The key codes are the HID usage tables Android maps to its own keys:
 * https://source.android.com/docs/core/interaction/input/keyboard-devices
 */
object HidReports {
    const val KEYBOARD_ID = 0x01
    const val REMOTE_ID = 0x02
    const val MOUSE_ID = 0x03
    const val SYSTEM_ID = 0x04
    const val GAMEPAD_ID = 0x05

    /** Every remote key and keyboard key report is followed by this, which lets the key go. */
    val RELEASE: ByteArray get() = byteArrayOf(0x00, 0x00)

    val descriptor: ByteArray = bytes(
        // Remote keys
        0x05, 0x0C,             // Usage Page (Consumer)
        0x09, 0x01,             // Usage (Consumer Control)
        0xA1, 0x01,             // Collection (Application)
        0x85, REMOTE_ID,        //   Report ID
        0x19, 0x00,             //   Usage Minimum (0)
        0x2A, 0xFF, 0x03,       //   Usage Maximum (1023)
        0x75, 0x10,             //   Report Size (16)
        0x95, 0x01,             //   Report Count (1)
        0x15, 0x00,             //   Logical Minimum (0)
        0x26, 0xFF, 0x03,       //   Logical Maximum (1023)
        0x81, 0x00,             //   Input (Data, Array, Absolute)
        0xC0,                   // End Collection

        // Keyboard
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x06,             // Usage (Keyboard)
        0xA1, 0x01,             // Collection (Application)
        0x85, KEYBOARD_ID,      //   Report ID
        0x05, 0x07,             //   Usage Page (Keyboard)
        0x19, 0xE0,             //   Usage Minimum (Left Control)
        0x29, 0xE7,             //   Usage Maximum (Right GUI)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95, 0x08,             //   Report Count (8): one bit per modifier
        0x81, 0x02,             //   Input (Data, Variable, Absolute)
        0x75, 0x08,             //   Report Size (8)
        0x95, 0x01,             //   Report Count (1): one key at a time
        0x15, 0x00,             //   Logical Minimum (0)
        0x26, 0xFF, 0x00,       //   Logical Maximum (255)
        0x05, 0x07,             //   Usage Page (Keyboard)
        0x19, 0x00,             //   Usage Minimum (0)
        0x29, 0xFF,             //   Usage Maximum (255)
        0x81, 0x00,             //   Input (Data, Array, Absolute)
        0xC0,                   // End Collection

        // Mouse
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x02,             // Usage (Mouse)
        0xA1, 0x01,             // Collection (Application)
        0x85, MOUSE_ID,         //   Report ID
        0x09, 0x01,             //   Usage (Pointer)
        0xA1, 0x00,             //   Collection (Physical)
        0x05, 0x09,             //     Usage Page (Button)
        0x19, 0x01,             //     Usage Minimum (1)
        0x29, 0x03,             //     Usage Maximum (3)
        0x15, 0x00,             //     Logical Minimum (0)
        0x25, 0x01,             //     Logical Maximum (1)
        0x75, 0x01,             //     Report Size (1)
        0x95, 0x03,             //     Report Count (3)
        0x81, 0x02,             //     Input (Data, Variable, Absolute)
        0x75, 0x05,             //     Report Size (5): padding
        0x95, 0x01,             //     Report Count (1)
        0x81, 0x01,             //     Input (Constant)
        0x05, 0x01,             //     Usage Page (Generic Desktop)
        0x09, 0x30,             //     Usage (X)
        0x09, 0x31,             //     Usage (Y)
        0x09, 0x38,             //     Usage (Wheel)
        0x15, 0x81,             //     Logical Minimum (-127)
        0x25, 0x7F,             //     Logical Maximum (127)
        0x75, 0x08,             //     Report Size (8)
        0x95, 0x03,             //     Report Count (3)
        0x81, 0x06,             //     Input (Data, Variable, Relative)
        0xC0,                   //   End Collection
        0xC0,                   // End Collection

        // System control
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x80,             // Usage (System Control)
        0xA1, 0x01,             // Collection (Application)
        0x85, SYSTEM_ID,        //   Report ID
        0x19, 0x81,             //   Usage Minimum (System Power Down)
        0x29, 0x83,             //   Usage Maximum (System Wake Up)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95, 0x03,             //   Report Count (3)
        0x81, 0x02,             //   Input (Data, Variable, Absolute)
        0x75, 0x05,             //   Report Size (5): padding
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x01,             //   Input (Constant)
        0xC0,                   // End Collection

        // Gamepad. Button numbers follow the order Android and Linux read gamepads in:
        // 1 A, 2 B, 4 X, 5 Y, 7 L1, 8 R1, 9 L2, 10 R2, 11 Select, 12 Start, 13 Home, 14 L3, 15 R3.
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x05,             // Usage (Game Pad)
        0xA1, 0x01,             // Collection (Application)
        0x85, GAMEPAD_ID,       //   Report ID
        0x05, 0x09,             //   Usage Page (Button)
        0x19, 0x01,             //   Usage Minimum (1)
        0x29, 0x10,             //   Usage Maximum (16)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95, 0x10,             //   Report Count (16)
        0x81, 0x02,             //   Input (Data, Variable, Absolute)
        0x05, 0x01,             //   Usage Page (Generic Desktop)
        0x09, 0x39,             //   Usage (Hat Switch)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x07,             //   Logical Maximum (7)
        0x35, 0x00,             //   Physical Minimum (0)
        0x46, 0x3B, 0x01,       //   Physical Maximum (315 degrees)
        0x65, 0x14,             //   Unit (Degrees)
        0x75, 0x04,             //   Report Size (4)
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x42,             //   Input (Data, Variable, Absolute, Null State): 8 is centered
        0x65, 0x00,             //   Unit (None)
        0x75, 0x04,             //   Report Size (4): padding
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x01,             //   Input (Constant)
        0x09, 0x30,             //   Usage (X): left stick
        0x09, 0x31,             //   Usage (Y)
        0x09, 0x32,             //   Usage (Z): right stick
        0x09, 0x35,             //   Usage (Rz)
        0x15, 0x81,             //   Logical Minimum (-127)
        0x25, 0x7F,             //   Logical Maximum (127)
        0x75, 0x08,             //   Report Size (8)
        0x95, 0x04,             //   Report Count (4)
        0x81, 0x02,             //   Input (Data, Variable, Absolute)
        0xC0                    // End Collection
    )

    /** A remote key report: the 16-bit usage, low byte first. */
    fun remote(key: RemoteKey): ByteArray = byteArrayOf((key.usage and 0xFF).toByte(), (key.usage shr 8 and 0xFF).toByte())

    /** A keyboard report: the held modifiers and one key. */
    fun keyboard(stroke: KeyStroke): ByteArray = byteArrayOf(stroke.modifiers.toByte(), stroke.key.toByte())

    /**
     * A mouse report. Movement and wheel are clamped to what one report can carry; the touchpad
     * splits bigger moves over several reports.
     */
    fun mouse(buttons: Int = 0, dx: Int = 0, dy: Int = 0, wheel: Int = 0): ByteArray = byteArrayOf(
        (buttons and 0x07).toByte(),
        dx.coerceIn(-127, 127).toByte(),
        dy.coerceIn(-127, 127).toByte(),
        wheel.coerceIn(-127, 127).toByte()
    )

    /** A system control report: one bit per key in [SystemKey]; 0 lets go. */
    fun system(key: SystemKey?): ByteArray = byteArrayOf((key?.bit ?: 0).toByte())

    /** A gamepad report: buttons (two bytes), hat, then the four stick axes. */
    fun gamepad(state: GamepadState): ByteArray = byteArrayOf(
        (state.buttons and 0xFF).toByte(),
        (state.buttons shr 8 and 0xFF).toByte(),
        (state.hat?.value ?: HAT_CENTERED).toByte(),
        axis(state.leftX), axis(state.leftY), axis(state.rightX), axis(state.rightY)
    )

    private const val HAT_CENTERED = 8

    private fun axis(value: Float): Byte = (value.coerceIn(-1f, 1f) * 127).toInt().toByte()

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}

/** Mouse button bits for [HidReports.mouse]. */
object MouseButtons {
    const val LEFT = 0x01
    const val RIGHT = 0x02
    const val MIDDLE = 0x04
}
