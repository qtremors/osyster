package dev.qtremors.osyster.core.device.domain

import kotlinx.coroutines.flow.Flow

/**
 * One live sample, already formatted per axis ("x 0.12 m/s²"). Only the requested sensor is
 * registered, and only while the flow is collected.
 */
data class SensorReading(
    val name: String,
    val values: List<String>,
    val accuracy: SensorAccuracy
)

enum class SensorAccuracy { UNRELIABLE, LOW, MEDIUM, HIGH, UNKNOWN }

interface SensorReadingSource {
    fun readings(sensorKey: String): Flow<SensorReading>
}
