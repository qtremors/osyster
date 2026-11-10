package dev.qtremors.osyster.core.hid.domain

import kotlin.math.abs
import kotlin.math.hypot

/**
 * The arrow a swipe means: the stronger of the two axes once the finger has moved [threshold]
 * pixels, or null while it is still a tap. Screen Y grows downward, so a positive [dy] is DOWN.
 */
fun swipeDirection(dx: Float, dy: Float, threshold: Float): RemoteKey? {
    if (hypot(dx, dy) < threshold) return null
    return if (abs(dx) > abs(dy)) {
        if (dx > 0) RemoteKey.RIGHT else RemoteKey.LEFT
    } else {
        if (dy > 0) RemoteKey.DOWN else RemoteKey.UP
    }
}
