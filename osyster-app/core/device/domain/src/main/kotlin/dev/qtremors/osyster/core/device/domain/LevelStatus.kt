package dev.qtremors.osyster.core.device.domain

/** How a reading is doing at a glance; the UI draws it green, amber, or red. */
enum class LevelStatus { GOOD, WARNING, CRITICAL }

/** For how full something is (storage, memory): fine below 75%, watch from 75%, act from 90%. */
fun fullnessStatus(fraction: Float): LevelStatus = when {
    fraction >= FULL_CRITICAL -> LevelStatus.CRITICAL
    fraction >= FULL_WARNING -> LevelStatus.WARNING
    else -> LevelStatus.GOOD
}

/** For battery charge: low from 30%, critical from 15%; charging always reads as fine. */
fun batteryStatus(fraction: Float, charging: Boolean): LevelStatus = when {
    charging -> LevelStatus.GOOD
    fraction <= BATTERY_CRITICAL -> LevelStatus.CRITICAL
    fraction <= BATTERY_WARNING -> LevelStatus.WARNING
    else -> LevelStatus.GOOD
}

/** For processor load: busy from 60%, very busy from 85%. */
fun loadStatus(fraction: Float): LevelStatus = when {
    fraction >= LOAD_CRITICAL -> LevelStatus.CRITICAL
    fraction >= LOAD_WARNING -> LevelStatus.WARNING
    else -> LevelStatus.GOOD
}

/** Battery temperature in Celsius: warm from 40, hot from 45. */
fun batteryTemperatureStatus(celsius: Float): LevelStatus = when {
    celsius >= TEMP_CRITICAL -> LevelStatus.CRITICAL
    celsius >= TEMP_WARNING -> LevelStatus.WARNING
    else -> LevelStatus.GOOD
}

/** The worse of two statuses, so a hot battery shows even when its charge is fine. */
fun worst(a: LevelStatus, b: LevelStatus): LevelStatus = if (a.ordinal >= b.ordinal) a else b

private const val FULL_WARNING = 0.75f
private const val FULL_CRITICAL = 0.9f
private const val BATTERY_WARNING = 0.3f
private const val BATTERY_CRITICAL = 0.15f
private const val LOAD_WARNING = 0.6f
private const val LOAD_CRITICAL = 0.85f
private const val TEMP_WARNING = 40f
private const val TEMP_CRITICAL = 45f
