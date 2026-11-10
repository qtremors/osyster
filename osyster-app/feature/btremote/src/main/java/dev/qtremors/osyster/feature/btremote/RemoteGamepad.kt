package dev.qtremors.osyster.feature.btremote

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.hid.domain.GamepadButton
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics
import kotlin.math.roundToInt

/**
 * A gamepad for Android TV and computer games: shoulder buttons, two sticks, the D-pad, the four
 * face buttons, and Select, Home, and Start. A tap on a stick presses it in (L3 or R3).
 */
@Composable
internal fun GamepadContent(viewModel: RemoteViewModel) {
    val button = { b: GamepadButton -> Pair({ viewModel.gamepadButton(b, true) }, { viewModel.gamepadButton(b, false) }) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PadButton(R.string.pad_l2, button(GamepadButton.L2))
            PadButton(R.string.pad_l1, button(GamepadButton.L1))
            PadButton(R.string.pad_r1, button(GamepadButton.R1))
            PadButton(R.string.pad_r2, button(GamepadButton.R2))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Stick(
                label = stringResource(R.string.pad_left_stick),
                onMove = { x, y -> viewModel.gamepadStick(left = true, x = x, y = y) },
                onTap = { viewModel.gamepadButton(GamepadButton.L3, true); viewModel.gamepadButton(GamepadButton.L3, false) },
                modifier = Modifier.weight(1f)
            )
            FaceButtons(button, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            PadArrows(viewModel, Modifier.weight(1f))
            Stick(
                label = stringResource(R.string.pad_right_stick),
                onMove = { x, y -> viewModel.gamepadStick(left = false, x = x, y = y) },
                onTap = { viewModel.gamepadButton(GamepadButton.R3, true); viewModel.gamepadButton(GamepadButton.R3, false) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PadButton(R.string.pad_select, button(GamepadButton.SELECT))
            PadButton(R.string.pad_home, button(GamepadButton.HOME))
            PadButton(R.string.pad_start, button(GamepadButton.START))
        }
    }
}

@Composable
private fun RowScope.PadButton(label: Int, actions: Pair<() -> Unit, () -> Unit>) {
    val text = stringResource(label)
    PressKey(text, actions.first, actions.second, Modifier.weight(1f).height(52.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** A, B, X, and Y in the usual diamond, colored like most controllers. */
@Composable
private fun FaceButtons(button: (GamepadButton) -> Pair<() -> Unit, () -> Unit>, modifier: Modifier) {
    Box(modifier = modifier.aspectRatio(1f)) {
        Face(GamepadButton.Y, R.string.pad_y, Color(0xFFF5C518), button, Modifier.align(Alignment.TopCenter))
        Face(GamepadButton.X, R.string.pad_x, Color(0xFF3E63DD), button, Modifier.align(Alignment.CenterStart))
        Face(GamepadButton.B, R.string.pad_b, Color(0xFFE5484D), button, Modifier.align(Alignment.CenterEnd))
        Face(GamepadButton.A, R.string.pad_a, Color(0xFF30A46C), button, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Face(b: GamepadButton, label: Int, color: Color, button: (GamepadButton) -> Pair<() -> Unit, () -> Unit>, modifier: Modifier) {
    val text = stringResource(label)
    val (press, release) = button(b)
    PressKey(text, press, release, modifier.size(56.dp), container = color) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

/** The D-pad; diagonals work by holding two arrows. */
@Composable
private fun PadArrows(viewModel: RemoteViewModel, modifier: Modifier) {
    val arrow = { index: Int -> Pair({ viewModel.gamepadArrow(index, true) }, { viewModel.gamepadArrow(index, false) }) }
    Box(modifier = modifier.aspectRatio(1f)) {
        listOf(
            Triple(0, Icons.Rounded.KeyboardArrowUp, Alignment.TopCenter),
            Triple(1, Icons.Rounded.KeyboardArrowDown, Alignment.BottomCenter),
            Triple(2, Icons.Rounded.KeyboardArrowLeft, Alignment.CenterStart),
            Triple(3, Icons.Rounded.KeyboardArrowRight, Alignment.CenterEnd)
        ).forEach { (index, icon, alignment) ->
            val (press, release) = arrow(index)
            PressKey(icon, stringResource(ARROW_LABELS[index]), press, release, Modifier.align(alignment).size(52.dp), iconSize = 30.dp)
        }
    }
}

private val ARROW_LABELS = listOf(R.string.remote_up, R.string.remote_down, R.string.remote_left, R.string.remote_right)

/**
 * A thumbstick: the knob follows the finger inside the ring and springs back to the middle on
 * lift. X and Y run from -1 to 1. A touch that never moves is a press of the stick (L3 or R3).
 */
@Composable
private fun Stick(label: String, onMove: (Float, Float) -> Unit, onTap: () -> Unit, modifier: Modifier) {
    val haptics = rememberAppHaptics()
    val move by rememberUpdatedState(onMove)
    val tap by rememberUpdatedState(onTap)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .onSizeChanged { size = it }
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f * KNOB_TRAVEL
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val raw = change.position - center
                        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                        val distance = raw.getDistance()
                        knob = if (distance > radius) raw * (radius / distance) else raw
                        if (moved) move(knob.x / radius, knob.y / radius)
                        change.consume()
                    }
                    knob = Offset.Zero
                    move(0f, 0f)
                    if (!moved) {
                        haptics.tap()
                        tap()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/** How far the knob travels, as a share of the ring's radius. */
private const val KNOB_TRAVEL = 0.62f
