package dev.qtremors.osyster.core.system.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Timer mode keeps the screen on for a while, like Caffeinate. [endsAt] is epoch millis, or null
 * when running until stopped.
 */
sealed interface StayAwakeTimer {
    data object Off : StayAwakeTimer
    /** [minutes] is the chosen duration (null = until stopped), so the tile knows what comes next. */
    data class Running(val endsAt: Long?, val minutes: Int?) : StayAwakeTimer
}

/** Durations offered on the tool page and cycled by the tile, in order. Null means "until stopped". */
val StayAwakeDurationsMinutes: List<Int?> = listOf(15, 30, 60, 120, null)

/**
 * Android's developer option that keeps the screen on while charging. Changing it needs the
 * WRITE_SECURE_SETTINGS grant; without it [canChange] is false and the page shows how to grant it.
 */
data class WhileChargingState(val canChange: Boolean, val enabled: Boolean)

interface StayAwakeController {
    val timer: StateFlow<StayAwakeTimer>
    fun start(minutes: Int?)
    fun stop()

    fun whileCharging(): WhileChargingState
    /** Returns false when Android refused the change (no grant). */
    fun setWhileCharging(enabled: Boolean): Boolean
}

/** What one tap on the tile does: start at 15 minutes, step up through the durations, then stop. */
fun nextStayAwakeStep(current: StayAwakeTimer): StayAwakeStep = when (current) {
    StayAwakeTimer.Off -> StayAwakeStep.Start(StayAwakeDurationsMinutes.first())
    is StayAwakeTimer.Running -> {
        val index = StayAwakeDurationsMinutes.indexOf(current.minutes)
        if (index < 0 || index == StayAwakeDurationsMinutes.lastIndex) StayAwakeStep.Stop
        else StayAwakeStep.Start(StayAwakeDurationsMinutes[index + 1])
    }
}

sealed interface StayAwakeStep {
    data class Start(val minutes: Int?) : StayAwakeStep
    data object Stop : StayAwakeStep
}
