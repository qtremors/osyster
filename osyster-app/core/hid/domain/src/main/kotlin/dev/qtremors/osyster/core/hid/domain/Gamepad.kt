package dev.qtremors.osyster.core.hid.domain

/**
 * Gamepad buttons by bit, in the order Android and Linux read a Bluetooth gamepad's buttons
 * (button 1 is A, 2 is B, 4 is X, 5 is Y, and so on; 3 and 6 are unused on most pads).
 */
enum class GamepadButton(val bit: Int) {
    A(1 shl 0),
    B(1 shl 1),
    X(1 shl 3),
    Y(1 shl 4),
    L1(1 shl 6),
    R1(1 shl 7),
    L2(1 shl 8),
    R2(1 shl 9),
    SELECT(1 shl 10),
    START(1 shl 11),
    HOME(1 shl 12),
    L3(1 shl 13),
    R3(1 shl 14)
}

/** The D-pad as a hat switch: eight directions clockwise from up. */
enum class Hat(val value: Int) { UP(0), UP_RIGHT(1), RIGHT(2), DOWN_RIGHT(3), DOWN(4), DOWN_LEFT(5), LEFT(6), UP_LEFT(7) }

/** Which way the D-pad points for the arrows held, or null when none (or only opposite ones) are. */
fun hatFor(up: Boolean, down: Boolean, left: Boolean, right: Boolean): Hat? {
    val vertical = if (up == down) 0 else if (up) -1 else 1
    val horizontal = if (left == right) 0 else if (left) -1 else 1
    return when (vertical to horizontal) {
        -1 to 0 -> Hat.UP
        -1 to 1 -> Hat.UP_RIGHT
        0 to 1 -> Hat.RIGHT
        1 to 1 -> Hat.DOWN_RIGHT
        1 to 0 -> Hat.DOWN
        1 to -1 -> Hat.DOWN_LEFT
        0 to -1 -> Hat.LEFT
        -1 to -1 -> Hat.UP_LEFT
        else -> null
    }
}

/** Everything a gamepad report carries. Sticks run from -1 to 1; down and right are positive. */
data class GamepadState(
    val buttons: Int = 0,
    val hat: Hat? = null,
    val leftX: Float = 0f,
    val leftY: Float = 0f,
    val rightX: Float = 0f,
    val rightY: Float = 0f
) {
    fun with(button: GamepadButton, pressed: Boolean): GamepadState =
        copy(buttons = if (pressed) buttons or button.bit else buttons and button.bit.inv())
}
