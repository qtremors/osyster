package dev.qtremors.osyster.core.device.data.read

import android.hardware.Sensor
import java.util.Locale

/** Where a sensor is listed on the Sensors page. */
internal enum class SensorGroup { MOTION, POSITION, ENVIRONMENT, OTHER }

/**
 * Names, groups, and value formatting for Android sensor types. Shared by the inventory and the
 * live reading sheet so both describe a sensor the same way.
 */
internal object SensorCatalog {
    /** A key that finds the same sensor again within this process. */
    fun key(sensor: Sensor): String = "${sensor.type}|${sensor.name}|${sensor.vendor}"

    fun group(type: Int): SensorGroup = when (type) {
        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_ACCELEROMETER_UNCALIBRATED, Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_STEP_DETECTOR,
        Sensor.TYPE_SIGNIFICANT_MOTION, Sensor.TYPE_MOTION_DETECT, Sensor.TYPE_STATIONARY_DETECT -> SensorGroup.MOTION
        Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, Sensor.TYPE_PROXIMITY,
        Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, Sensor.TYPE_POSE_6DOF,
        HINGE_ANGLE -> SensorGroup.POSITION
        Sensor.TYPE_LIGHT, Sensor.TYPE_PRESSURE, Sensor.TYPE_AMBIENT_TEMPERATURE,
        Sensor.TYPE_RELATIVE_HUMIDITY -> SensorGroup.ENVIRONMENT
        else -> SensorGroup.OTHER
    }

    /** Android's own description of the type, such as "Accelerometer". Vendor types keep their string. */
    fun typeName(sensor: Sensor): String = TYPE_NAMES[sensor.type]
        ?: sensor.stringType.substringAfterLast('.').replace('_', ' ').replaceFirstChar { it.titlecase() }

    /** Axis labels and unit per type; values beyond the labels are shown by index. */
    fun format(type: Int, values: FloatArray, locale: Locale = Locale.getDefault()): List<String> {
        val (axes, unit) = AXES[type] ?: (emptyList<String>() to "")
        return values.mapIndexed { index, value ->
            val axis = axes.getOrNull(index) ?: "v${index + 1}"
            val number = if (type == Sensor.TYPE_STEP_COUNTER) value.toLong().toString() else String.format(locale, "%.3f", value)
            listOf(axis, number, unit).filter(String::isNotEmpty).joinToString(" ")
        }.take(MAX_VALUES)
    }

    private const val MAX_VALUES = 6
    private const val HINGE_ANGLE = 36

    private val TYPE_NAMES = mapOf(
        Sensor.TYPE_ACCELEROMETER to "Accelerometer",
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED to "Accelerometer (uncalibrated)",
        Sensor.TYPE_GYROSCOPE to "Gyroscope",
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED to "Gyroscope (uncalibrated)",
        Sensor.TYPE_GRAVITY to "Gravity",
        Sensor.TYPE_LINEAR_ACCELERATION to "Linear acceleration",
        Sensor.TYPE_ROTATION_VECTOR to "Rotation vector",
        Sensor.TYPE_GAME_ROTATION_VECTOR to "Game rotation vector",
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to "Geomagnetic rotation vector",
        Sensor.TYPE_MAGNETIC_FIELD to "Magnetometer",
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED to "Magnetometer (uncalibrated)",
        Sensor.TYPE_PROXIMITY to "Proximity",
        Sensor.TYPE_LIGHT to "Light",
        Sensor.TYPE_PRESSURE to "Barometer",
        Sensor.TYPE_AMBIENT_TEMPERATURE to "Ambient temperature",
        Sensor.TYPE_RELATIVE_HUMIDITY to "Humidity",
        Sensor.TYPE_STEP_COUNTER to "Step counter",
        Sensor.TYPE_STEP_DETECTOR to "Step detector",
        Sensor.TYPE_SIGNIFICANT_MOTION to "Significant motion",
        Sensor.TYPE_HEART_RATE to "Heart rate",
        HINGE_ANGLE to "Hinge angle"
    )

    private val XYZ = listOf("x", "y", "z")
    private val AXES: Map<Int, Pair<List<String>, String>> = mapOf(
        Sensor.TYPE_ACCELEROMETER to (XYZ to "m/s²"),
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED to (XYZ + listOf("bias x", "bias y", "bias z") to "m/s²"),
        Sensor.TYPE_GRAVITY to (XYZ to "m/s²"),
        Sensor.TYPE_LINEAR_ACCELERATION to (XYZ to "m/s²"),
        Sensor.TYPE_GYROSCOPE to (XYZ to "rad/s"),
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED to (XYZ + listOf("drift x", "drift y", "drift z") to "rad/s"),
        Sensor.TYPE_MAGNETIC_FIELD to (XYZ to "µT"),
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED to (XYZ + listOf("bias x", "bias y", "bias z") to "µT"),
        Sensor.TYPE_ROTATION_VECTOR to (listOf("x", "y", "z", "w") to ""),
        Sensor.TYPE_GAME_ROTATION_VECTOR to (listOf("x", "y", "z", "w") to ""),
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to (listOf("x", "y", "z", "w") to ""),
        Sensor.TYPE_LIGHT to (listOf("") to "lx"),
        Sensor.TYPE_PRESSURE to (listOf("") to "hPa"),
        Sensor.TYPE_PROXIMITY to (listOf("") to "cm"),
        Sensor.TYPE_AMBIENT_TEMPERATURE to (listOf("") to "°C"),
        Sensor.TYPE_RELATIVE_HUMIDITY to (listOf("") to "%"),
        Sensor.TYPE_STEP_COUNTER to (listOf("") to "steps"),
        Sensor.TYPE_HEART_RATE to (listOf("") to "bpm"),
        HINGE_ANGLE to (listOf("") to "°")
    )
}
