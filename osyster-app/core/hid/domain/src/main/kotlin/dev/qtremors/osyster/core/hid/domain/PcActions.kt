package dev.qtremors.osyster.core.hid.domain

import kotlin.math.abs

/** One step of a key sequence sent to a computer. */
sealed interface MacroStep {
    data class Keys(val stroke: KeyStroke) : MacroStep
    data class System(val key: SystemKey) : MacroStep
    data class Wait(val millis: Long) : MacroStep
}

/**
 * Computer actions a Bluetooth keyboard can trigger, with nothing installed on the computer
 * (what PC Remote Control did with a server on the PC). Lock and the screenshot are shortcuts;
 * sleep and the power key are the keyboard's own system keys; shut down and restart walk Windows'
 * Win+X menu, so they are Windows only.
 */
enum class PcAction(val steps: List<MacroStep>, val windowsOnly: Boolean = false, val confirm: Boolean = false) {
    /** Win+L on Windows and most Linux desktops. */
    LOCK(listOf(MacroStep.Keys(KeyStroke(KEY_L, Modifiers.META)))),
    SLEEP(listOf(MacroStep.System(SystemKey.SLEEP)), confirm = true),

    /** Whatever the computer's own power button does: often sleep, sometimes shut down. */
    POWER_KEY(listOf(MacroStep.System(SystemKey.POWER_DOWN)), confirm = true),
    SHUT_DOWN(winXMenu(KEY_U), windowsOnly = true, confirm = true),
    RESTART(winXMenu(KEY_R), windowsOnly = true, confirm = true),

    /** Win+Print Screen: Windows saves the whole screen to Pictures, Screenshots. */
    SCREENSHOT(listOf(MacroStep.Keys(KeyStroke(KeyCodes.PRINT_SCREEN, Modifiers.META))), windowsOnly = true),

    /** Ctrl+Shift+Esc opens Task Manager on Windows. */
    TASK_MANAGER(listOf(MacroStep.Keys(KeyStroke(KeyCodes.ESCAPE, Modifiers.CTRL or Modifiers.SHIFT))), windowsOnly = true),

    /** Win+D shows the desktop on Windows. */
    SHOW_DESKTOP(listOf(MacroStep.Keys(KeyStroke(KEY_D, Modifiers.META))), windowsOnly = true)
}

// Letter keys used by the shortcuts above (Keyboard page usages).
private const val KEY_D = 0x07
private const val KEY_L = 0x0F
private const val KEY_R = 0x15
private const val KEY_U = 0x18
private const val KEY_X = 0x1B

/**
 * Win+X opens Windows' power user menu, U opens "Shut down or sign out", and [last] picks from it
 * (U shuts down, R restarts). The pauses give each menu time to open.
 */
private fun winXMenu(last: Int): List<MacroStep> = listOf(
    MacroStep.Keys(KeyStroke(KEY_X, Modifiers.META)),
    MacroStep.Wait(MENU_WAIT_MILLIS),
    MacroStep.Keys(KeyStroke(KEY_U)),
    MacroStep.Wait(MENU_WAIT_MILLIS),
    MacroStep.Keys(KeyStroke(last))
)

private const val MENU_WAIT_MILLIS = 500L

/**
 * Air mouse: turning the phone left or right (rotation about its Z axis) moves the pointer
 * sideways, tilting it up or down (X axis) moves it up and down, as BT Remote does. Rates are in
 * radians per second, read about every 10 ms; small rates are a resting hand and move nothing.
 */
fun airMouseDelta(rateX: Float, rateZ: Float, speed: Float, deadZone: Float = 0.03f): Pair<Float, Float> {
    fun scaled(rate: Float) = if (abs(rate) < deadZone) 0f else -rate * AIR_GAIN * speed
    return scaled(rateZ) to scaled(rateX)
}

private const val AIR_GAIN = 10f
