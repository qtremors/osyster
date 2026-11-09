package dev.qtremors.osyster.core.device.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.data.read.SensorCatalog
import dev.qtremors.osyster.core.device.domain.SensorAccuracy
import dev.qtremors.osyster.core.device.domain.SensorReading
import dev.qtremors.osyster.core.device.domain.SensorReadingSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow

/** Registers exactly one sensor while collected and unregisters it the moment collection stops. */
@Singleton
internal class SensorReadingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SensorReadingSource {

    override fun readings(sensorKey: String): Flow<SensorReading> {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return emptyFlow()
        val sensor = manager.getSensorList(Sensor.TYPE_ALL).firstOrNull { SensorCatalog.key(it) == sensorKey }
            ?: return emptyFlow()
        return callbackFlow {
            var accuracy = SensorAccuracy.UNKNOWN
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(SensorReading(sensor.name, SensorCatalog.format(sensor.type, event.values), accuracy))
                }

                override fun onAccuracyChanged(changed: Sensor, value: Int) {
                    accuracy = when (value) {
                        SensorManager.SENSOR_STATUS_UNRELIABLE -> SensorAccuracy.UNRELIABLE
                        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> SensorAccuracy.LOW
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracy.MEDIUM
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracy.HIGH
                        else -> SensorAccuracy.UNKNOWN
                    }
                }
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose { manager.unregisterListener(listener) }
        }.conflate()
    }
}
