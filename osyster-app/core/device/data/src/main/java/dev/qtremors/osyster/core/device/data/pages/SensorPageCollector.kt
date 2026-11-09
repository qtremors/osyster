package dev.qtremors.osyster.core.device.data.pages

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.read.SensorCatalog
import dev.qtremors.osyster.core.device.data.read.SensorGroup
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection
import dev.qtremors.osyster.core.device.domain.RowAction
import java.util.Locale

/** The Sensors page: every sensor Android lists, grouped, each opening live readings on tap. */
internal class SensorPageCollector(private val context: Context, private val text: DeviceText) {

    fun collect(): List<InfoSection> {
        val sensors = (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
            ?.getSensorList(Sensor.TYPE_ALL).orEmpty()
        val summary = section(text, "sensors", R.string.section_sensors_summary, "SensorManager") {
            row("count", R.string.row_sensor_count, sensors.size.toString(), copyable = false)
            row("wake_up", R.string.row_wake_up_sensors, sensors.count(Sensor::isWakeUpSensor).toString(), copyable = false)
            row("hint", R.string.row_sensor_hint, text[R.string.value_tap_for_live], copyable = false)
        }
        val groups = sensors.groupBy { SensorCatalog.group(it.type) }
        return listOf(summary) + SensorGroup.entries.mapNotNull { group ->
            val items = groups[group].orEmpty().sortedBy(SensorCatalog::typeName)
            if (items.isEmpty()) return@mapNotNull null
            val builder = SectionBuilder(text, "SensorManager")
            items.forEach { sensor ->
                val detail = buildString {
                    append(SensorCatalog.typeName(sensor))
                    append(" · ").append(sensor.vendor)
                    if (sensor.power > 0f) append(" · ").append(String.format(Locale.getDefault(), "%.2f mA", sensor.power))
                }
                builder.row(
                    key = SensorCatalog.key(sensor),
                    label = sensor.name,
                    value = Availability.Available(detail, "SensorManager", System.currentTimeMillis()),
                    action = RowAction.LiveSensor(SensorCatalog.key(sensor))
                )
            }
            builder.build("group_${group.name.lowercase()}", text[groupTitle(group)])
        }
    }

    private fun groupTitle(group: SensorGroup): Int = when (group) {
        SensorGroup.MOTION -> R.string.section_sensors_motion
        SensorGroup.POSITION -> R.string.section_sensors_position
        SensorGroup.ENVIRONMENT -> R.string.section_sensors_environment
        SensorGroup.OTHER -> R.string.section_sensors_other
    }
}
