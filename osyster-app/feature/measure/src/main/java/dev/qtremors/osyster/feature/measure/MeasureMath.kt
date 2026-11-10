package dev.qtremors.osyster.feature.measure

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.log10
import kotlin.math.sqrt

/** How far the phone leans, in degrees: [roll] left and right, [pitch] toward and away. */
data class Tilt(val roll: Float, val pitch: Float) {
    /** Within half a degree both ways, which is as level as a phone's sensor can tell. */
    val level: Boolean get() = abs(roll) < LEVEL_DEGREES && abs(pitch) < LEVEL_DEGREES
}

/** Tilt from the gravity vector (m/s², device axes). */
fun tiltFrom(x: Float, y: Float, z: Float): Tilt {
    val roll = Math.toDegrees(atan2(x.toDouble(), sqrt((y * y + z * z).toDouble()))).toFloat()
    val pitch = Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
    return Tilt(roll, pitch)
}

/** A heading from 0 to 360 degrees, whatever the input. */
fun normalizeHeading(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

/**
 * Moves [current] a share of the way toward [target] the short way round, so the needle does not
 * spin all the way back when the heading crosses north.
 */
fun smoothHeading(current: Float, target: Float, share: Float = 0.15f): Float {
    var delta = normalizeHeading(target) - normalizeHeading(current)
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return normalizeHeading(current + delta * share)
}

/** The 16-point compass name for a heading, as an index into N, NNE, NE, ... NNW. */
fun cardinalIndex(degrees: Float): Int = ((normalizeHeading(degrees) + 11.25f) / 22.5f).toInt() % 16

/**
 * Loudness of 16-bit samples in decibels. Phone microphones are not calibrated, so this is the
 * level below full scale plus a fixed [offset] that lands normal speech near 60 dB; good for
 * comparing, not for measuring against a standard.
 */
fun decibels(samples: ShortArray, count: Int, offset: Double = MIC_OFFSET_DB): Double? {
    if (count <= 0) return null
    var sum = 0.0
    for (i in 0 until count) sum += samples[i].toDouble() * samples[i]
    val rms = sqrt(sum / count)
    if (rms < 1.0) return 0.0
    return (20 * log10(rms / Short.MAX_VALUE) + offset).coerceAtLeast(0.0)
}

/** A sound level in words, for the meter's caption. */
fun loudnessBand(db: Double): Int = when {
    db < 30 -> 0
    db < 50 -> 1
    db < 70 -> 2
    db < 85 -> 3
    else -> 4
}

/** Screen length in centimeters for [pixels] at [dpi] pixels per inch. */
fun pixelsToCm(pixels: Float, dpi: Float): Float = if (dpi <= 0f) 0f else pixels / dpi * CM_PER_INCH

fun cmToPixels(cm: Float, dpi: Float): Float = cm / CM_PER_INCH * dpi

const val CM_PER_INCH = 2.54f
private const val LEVEL_DEGREES = 0.5f
private const val MIC_OFFSET_DB = 90.0
