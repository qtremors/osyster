package dev.qtremors.osyster.core.system.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * The flashlight. [maxLevel] above 1 means the phone supports brightness levels (Android 13+);
 * otherwise only on and off work. [available] is false while the camera is busy or absent.
 */
data class TorchState(
    val hasFlash: Boolean,
    val available: Boolean,
    val on: Boolean,
    val level: Int,
    val maxLevel: Int
) {
    val supportsBrightness: Boolean get() = maxLevel > 1
}

interface TorchController {
    val state: StateFlow<TorchState>
    /** Turns the torch on at the remembered brightness, or off. */
    fun setOn(on: Boolean)
    /** Sets and remembers the brightness (1..maxLevel); turns the torch on if it is off. */
    fun setLevel(level: Int)
}
