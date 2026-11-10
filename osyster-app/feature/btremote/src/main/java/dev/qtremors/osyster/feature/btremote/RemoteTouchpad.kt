package dev.qtremors.osyster.feature.btremote

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ToggleRow
import dev.qtremors.osyster.core.hid.domain.airMouseDelta
import dev.qtremors.osyster.core.hid.domain.RemoteSettings
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.hid.domain.MouseButtons
import dev.qtremors.osyster.core.hid.domain.RemoteKey
import kotlin.math.abs
import kotlin.math.max

/**
 * The touchpad: one finger moves the pointer and a quick tap clicks; two fingers scroll, and a
 * two-finger tap right-clicks (three fingers, middle). The strip on the right scrolls with one
 * finger. Hold a button below to drag. Back and Home stay at hand for TVs and computers alike.
 * Gestures follow BT Remote by Atharok.
 */
@Composable
internal fun TouchpadContent(viewModel: RemoteViewModel, keys: KeyActions, settings: RemoteSettings) {
    val context = LocalContext.current
    val sensors = remember { context.getSystemService(SensorManager::class.java) }
    val gyroscope = remember { sensors?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    if (gyroscope != null && settings.airMouse) AirMouse(sensors, gyroscope, settings.mouseSpeed, viewModel::moveMouse)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (gyroscope != null) {
            ConnectedGroup {
                ToggleRow(
                    title = stringResource(R.string.remote_air_mouse),
                    supporting = stringResource(R.string.remote_air_mouse_summary),
                    checked = settings.airMouse,
                    onCheckedChange = { on -> viewModel.update { it.copy(airMouse = on) } },
                    icon = Icons.Rounded.ScreenRotation
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
        ) {
            Touchpad(
                onMove = viewModel::moveMouse,
                onScroll = viewModel::scrollMouse,
                onClick = viewModel::click,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ScrollStrip(onScroll = viewModel::scrollMouse, modifier = Modifier.width(52.dp).fillMaxHeight())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MouseButton(R.string.remote_mouse_left, MouseButtons.LEFT, viewModel, Modifier.weight(0.4f))
            MouseButton(R.string.remote_mouse_middle, MouseButtons.MIDDLE, viewModel, Modifier.weight(0.2f))
            MouseButton(R.string.remote_mouse_right, MouseButtons.RIGHT, viewModel, Modifier.weight(0.4f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PressKey(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.remote_back), { keys.onPress(RemoteKey.BACK) }, keys.onRelease, Modifier.weight(1f).height(52.dp))
            PressKey(Icons.Rounded.Home, stringResource(R.string.remote_home), { keys.onPress(RemoteKey.HOME) }, keys.onRelease, Modifier.weight(1f).height(52.dp))
        }
    }
}

@Composable
private fun Touchpad(
    onMove: (Float, Float) -> Unit,
    onScroll: (Float) -> Unit,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val move by rememberUpdatedState(onMove)
    val scroll by rememberUpdatedState(onScroll)
    val click by rememberUpdatedState(onClick)
    val hint = stringResource(R.string.remote_touchpad_hint)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(36.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .semantics { contentDescription = hint }
            .pointerInput(Unit) {
                val tapSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val start = down.uptimeMillis
                    var fingers = 1
                    var drift = 0f
                    var end = start
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        fingers = max(fingers, pressed.size)
                        end = event.changes.maxOf { it.uptimeMillis }
                        when (pressed.size) {
                            // One finger moves only if no second finger joined: lifting one of two
                            // scrolling fingers must not jerk the pointer.
                            1 -> if (fingers == 1) {
                                val delta = pressed[0].positionChange()
                                drift += delta.getDistance()
                                move(delta.x, delta.y)
                            }
                            2 -> {
                                val dy = pressed.map { it.positionChange().y }.average().toFloat()
                                drift += abs(dy)
                                scroll(dy)
                            }
                        }
                        event.changes.forEach { it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                    if (end - start < TAP_MILLIS && drift < tapSlop) {
                        when (fingers) {
                            1 -> click(MouseButtons.LEFT)
                            2 -> click(MouseButtons.RIGHT)
                            else -> click(MouseButtons.MIDDLE)
                        }
                    }
                }
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Rounded.Mouse, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Drag up or down to scroll with one finger. */
@Composable
private fun ScrollStrip(onScroll: (Float) -> Unit, modifier: Modifier = Modifier) {
    val scroll by rememberUpdatedState(onScroll)
    val label = stringResource(R.string.remote_scroll_strip)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        scroll(change.positionChange().y)
                        change.consume()
                    }
                }
            }
    ) {
        Icon(Icons.Rounded.UnfoldMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Held down for as long as the finger stays, so it can drag with the touchpad. */
@Composable
private fun MouseButton(label: Int, button: Int, viewModel: RemoteViewModel, modifier: Modifier) {
    val text = stringResource(label)
    PressKey(text, { viewModel.pressButton(button) }, { viewModel.releaseButton(button) }, modifier.height(64.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Moves the pointer with the phone while it is shown: gyroscope rates turned into pointer moves
 * (see airMouseDelta), read about every 10 ms. Stops listening as soon as it leaves the screen.
 */
@Composable
private fun AirMouse(sensors: SensorManager?, gyroscope: Sensor, speed: Float, onMove: (Float, Float) -> Unit) {
    val move by rememberUpdatedState(onMove)
    val currentSpeed by rememberUpdatedState(speed)
    DisposableEffect(sensors, gyroscope) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (dx, dy) = airMouseDelta(event.values[0], event.values[2], currentSpeed)
                if (dx != 0f || dy != 0f) move(dx, dy)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors?.registerListener(listener, gyroscope, AIR_SAMPLE_MICROS)
        onDispose { sensors?.unregisterListener(listener) }
    }
}

private const val AIR_SAMPLE_MICROS = 10_000
private const val TAP_MILLIS = 200L
