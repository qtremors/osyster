package dev.qtremors.osyster.core.device.data.read

import android.hardware.Sensor
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorCatalogTest {
    private val en = Locale.US

    @Test
    fun threeAxisSensorsLabelEachAxisWithUnit() {
        assertEquals(
            listOf("x 0.120 m/s²", "y 9.810 m/s²", "z -0.030 m/s²"),
            SensorCatalog.format(Sensor.TYPE_ACCELEROMETER, floatArrayOf(0.12f, 9.81f, -0.03f), en)
        )
    }

    @Test
    fun singleValueSensorsHaveNoAxisLabel() {
        assertEquals(listOf("412.000 lx"), SensorCatalog.format(Sensor.TYPE_LIGHT, floatArrayOf(412f), en))
    }

    @Test
    fun stepCounterShowsWholeSteps() {
        assertEquals(listOf("10234 steps"), SensorCatalog.format(Sensor.TYPE_STEP_COUNTER, floatArrayOf(10234f), en))
    }

    @Test
    fun unknownTypesFallBackToIndexedValuesAndCap() {
        val values = FloatArray(10) { it.toFloat() }
        val formatted = SensorCatalog.format(65_536, values, en)
        assertEquals(6, formatted.size)
        assertEquals("v1 0.000", formatted.first())
    }

    @Test
    fun groupsFollowAndroidCategories() {
        assertEquals(SensorGroup.MOTION, SensorCatalog.group(Sensor.TYPE_GYROSCOPE))
        assertEquals(SensorGroup.ENVIRONMENT, SensorCatalog.group(Sensor.TYPE_PRESSURE))
        assertEquals(SensorGroup.POSITION, SensorCatalog.group(Sensor.TYPE_PROXIMITY))
        assertEquals(SensorGroup.OTHER, SensorCatalog.group(65_536))
    }
}
