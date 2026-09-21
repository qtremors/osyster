package dev.qtremors.osyster.core.device.domain

import kotlinx.coroutines.flow.Flow

/**
 * What the Home hero card shows: who the phone is, and a few live numbers.
 * Live values are [Availability] so a restricted CPU load reads as restricted, not as 0%.
 */
data class DeviceSummary(
    val marketingName: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val cpuLoadPercent: Availability<Int>,
    /** Fastest current core clock, shown when Android restricts overall CPU load. */
    val cpuClockMhz: Availability<Int>,
    val ramUsedBytes: Availability<Long>,
    val ramTotalBytes: Availability<Long>,
    val storageUsedBytes: Availability<Long>,
    val storageTotalBytes: Availability<Long>,
    val batteryPercent: Availability<Int>,
    val isCharging: Boolean
)

/**
 * Supplies [DeviceSummary]. The flow samples only while collected, so the hero card stops
 * polling when Home is not visible.
 */
interface DeviceSummarySource {
    fun summary(): Flow<DeviceSummary>
}
