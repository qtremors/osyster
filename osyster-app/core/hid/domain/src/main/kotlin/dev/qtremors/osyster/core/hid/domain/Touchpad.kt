package dev.qtremors.osyster.core.hid.domain

import kotlin.math.abs
import kotlin.math.sign

/**
 * Turns finger movement into mouse movement reports. A report carries whole units from -127 to
 * 127, so the fraction left after scaling is kept for the next move (slow, careful moves still
 * add up) and a fast flick is split over several reports instead of being cut off.
 */
class PointerAccumulator {
    private var restX = 0f
    private var restY = 0f

    /** The reports for a finger move of [dx], [dy] pixels at [speed]. */
    fun move(dx: Float, dy: Float, speed: Float): List<Pair<Int, Int>> {
        restX += dx * speed
        restY += dy * speed
        var x = restX.toInt()
        var y = restY.toInt()
        restX -= x
        restY -= y
        val reports = mutableListOf<Pair<Int, Int>>()
        while (x != 0 || y != 0) {
            val stepX = x.coerceIn(-MAX, MAX)
            val stepY = y.coerceIn(-MAX, MAX)
            reports += stepX to stepY
            x -= stepX
            y -= stepY
        }
        return reports
    }

    fun reset() {
        restX = 0f
        restY = 0f
    }

    private companion object {
        const val MAX = 127
    }
}

/**
 * Turns two-finger (or scroll strip) movement into wheel clicks: one click per [stepPx] of
 * finger travel, scaled by speed. Fingers moving down scroll the page down, like a phone screen;
 * [invert] flips that.
 */
class ScrollAccumulator(private val stepPx: Float = 24f) {
    private var rest = 0f

    /** Wheel clicks for a vertical finger move of [dy] pixels; positive scrolls up. */
    fun scroll(dy: Float, speed: Float, invert: Boolean): Int {
        rest += dy
        val steps = (rest / stepPx).toInt()
        if (steps == 0) return 0
        rest -= steps * stepPx
        val clicks = (steps * speed).let { if (abs(it) < 1f) sign(it) else it }.toInt()
        // A finger moving down (positive dy) moves the page with it, which is the wheel scrolling up.
        return (if (invert) -clicks else clicks).coerceIn(-127, 127)
    }

    fun reset() {
        rest = 0f
    }
}
