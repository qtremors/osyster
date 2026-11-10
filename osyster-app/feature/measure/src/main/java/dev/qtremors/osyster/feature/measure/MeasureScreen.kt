package dev.qtremors.osyster.feature.measure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.MeasureRoute
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The four measuring tools, one at a time; each one's sensor runs only while it is on screen. */
enum class MeasureTool { LEVEL, COMPASS, RULER, SOUND }

sealed interface MeasureDestination {
    data object AppSettings : MeasureDestination
}

fun NavGraphBuilder.registerMeasureRoute(onNavigate: (MeasureDestination) -> Unit) {
    composable<MeasureRoute> { MeasureScreen(onNavigate) }
}

@Composable
private fun MeasureScreen(onNavigate: (MeasureDestination) -> Unit) {
    var tool by rememberSaveable { mutableStateOf(MeasureTool.LEVEL) }
    OsysterScaffold(title = stringResource(R.string.measure_title)) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)
        ) {
            ConnectedChoices(
                options = MeasureTool.entries,
                selected = tool,
                label = { stringResource(it.label) },
                onSelect = { tool = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            when (tool) {
                MeasureTool.LEVEL -> LevelTool()
                MeasureTool.COMPASS -> CompassTool()
                MeasureTool.RULER -> RulerTool()
                MeasureTool.SOUND -> SoundTool(onAppSettings = { onNavigate(MeasureDestination.AppSettings) })
            }
        }
    }
}

/** Listens to one sensor while in composition, at the rate the screen needs. */
@Composable
internal fun SensorEffect(type: Int, onAccuracy: (Int) -> Unit = {}, onValues: (FloatArray) -> Unit): Boolean {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember(type) { manager?.getDefaultSensor(type) }
    DisposableEffect(sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = onValues(event.values)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = onAccuracy(accuracy)
        }
        sensor?.let { manager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { manager?.unregisterListener(listener) }
    }
    return sensor != null
}

@Composable
private fun LevelTool() {
    val haptics = rememberAppHaptics()
    var tilt by remember { mutableStateOf(Tilt(0f, 0f)) }
    val gravity = SensorEffect(Sensor.TYPE_GRAVITY) { v -> tilt = tiltFrom(v[0], v[1], v[2]) }
    if (!gravity) SensorEffect(Sensor.TYPE_ACCELEROMETER) { v -> tilt = tiltFrom(v[0], v[1], v[2]) }
    // A tick as the phone comes level, so it can be felt without looking.
    var wasLevel by remember { mutableStateOf(false) }
    LaunchedEffect(tilt.level) {
        if (tilt.level && !wasLevel) haptics.confirm()
        wasLevel = tilt.level
    }
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.level_bubble)
    Surface(color = colors.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f).semantics { contentDescription = description }) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(colors.surfaceContainerHighest, radius, center)
                drawCircle(colors.outlineVariant, radius * 0.18f, center, style = Stroke(2.dp.toPx()))
                drawLine(colors.outlineVariant, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1.dp.toPx())
                drawLine(colors.outlineVariant, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1.dp.toPx())
                // The bubble floats away from the low side, as in a real level; 20 degrees reaches the rim.
                val reach = radius * 0.82f
                val bx = (-tilt.roll / MAX_TILT).coerceIn(-1f, 1f) * reach
                val by = (tilt.pitch / MAX_TILT).coerceIn(-1f, 1f) * reach
                drawCircle(if (tilt.level) colors.primary else colors.tertiary, radius * 0.16f, Offset(center.x + bx, center.y + by))
            }
            Text(
                if (tilt.level) stringResource(R.string.level_flat) else stringResource(R.string.level_tilt, oneDecimal(kotlin.math.abs(tilt.roll)), oneDecimal(kotlin.math.abs(tilt.pitch))),
                style = MaterialTheme.typography.headlineSmall,
                color = if (tilt.level) colors.primary else colors.onSurface,
                textAlign = TextAlign.Center
            )
            Text(stringResource(R.string.level_hint), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CompassTool() {
    var heading by remember { mutableFloatStateOf(0f) }
    var unsure by remember { mutableStateOf(false) }
    val matrix = remember { FloatArray(9) }
    val angles = remember { FloatArray(3) }
    val present = SensorEffect(
        Sensor.TYPE_ROTATION_VECTOR,
        onAccuracy = { unsure = it <= SensorManager.SENSOR_STATUS_ACCURACY_LOW }
    ) { v ->
        SensorManager.getRotationMatrixFromVector(matrix, v)
        SensorManager.getOrientation(matrix, angles)
        heading = smoothHeading(heading, Math.toDegrees(angles[0].toDouble()).toFloat())
    }
    val colors = MaterialTheme.colorScheme
    val points = stringArrayResource(R.array.compass_points)
    val description = stringResource(R.string.compass_dial)
    Surface(color = colors.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
            if (!present) {
                Text(stringResource(R.string.compass_missing), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            Canvas(modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f).semantics { contentDescription = description }) {
                val radius = min(size.width, size.height) / 2
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(colors.surfaceContainerHighest, radius, center)
                // The dial turns so north on it points to real north; the top mark is where the phone faces.
                rotate(-heading, center) {
                    for (deg in 0 until 360 step 15) {
                        val long = deg % 90 == 0
                        val a = Math.toRadians(deg.toDouble() - 90)
                        val outer = radius * 0.95f
                        val inner = radius * if (long) 0.8f else 0.88f
                        drawLine(
                            if (deg == 0) colors.error else colors.outline,
                            Offset(center.x + (cos(a) * inner).toFloat(), center.y + (sin(a) * inner).toFloat()),
                            Offset(center.x + (cos(a) * outer).toFloat(), center.y + (sin(a) * outer).toFloat()),
                            strokeWidth = (if (long) 4 else 2).dp.toPx()
                        )
                    }
                }
                drawLine(colors.primary, Offset(center.x, center.y - radius * 0.7f), center, strokeWidth = 6.dp.toPx())
                drawCircle(colors.primary, radius * 0.06f, center)
            }
            Text(stringResource(R.string.compass_heading, normalizeHeading(heading).toInt(), points[cardinalIndex(heading)]), style = MaterialTheme.typography.displaySmall)
            Text(
                stringResource(if (unsure) R.string.compass_calibrate else R.string.compass_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = if (unsure) colors.tertiary else colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

internal fun oneDecimal(value: Float): String = String.format(Locale.getDefault(), "%.1f", value)

private val MeasureTool.label: Int
    get() = when (this) {
        MeasureTool.LEVEL -> R.string.measure_level
        MeasureTool.COMPASS -> R.string.measure_compass
        MeasureTool.RULER -> R.string.measure_ruler
        MeasureTool.SOUND -> R.string.measure_sound
    }

private const val MAX_TILT = 20f
