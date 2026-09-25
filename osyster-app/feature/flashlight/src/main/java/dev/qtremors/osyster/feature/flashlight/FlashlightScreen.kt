package dev.qtremors.osyster.feature.flashlight

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.NoFlash
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.system.domain.TorchState
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics
import kotlin.math.roundToInt

/** Flashlight pulse state: the user's choice and whether notification access allows it. */
internal data class PulseUi(val enabled: Boolean, val hasAccess: Boolean)

@Composable
internal fun FlashlightScreen(
    state: TorchState,
    pulse: PulseUi,
    onToggle: () -> Unit,
    onLevel: (Int) -> Unit,
    onPulse: (Boolean) -> Unit,
    onGrantAccess: () -> Unit
) {
    OsysterScaffold(title = stringResource(Tool.FLASHLIGHT.label.title)) { padding ->
        LazyColumn(contentPadding = padding) {
            if (!state.hasFlash) {
                item(key = "none") {
                    EmptyState(Icons.Rounded.NoFlash, stringResource(R.string.flash_none_title), stringResource(R.string.flash_none_message))
                }
                return@LazyColumn
            }
            item(key = "switch") { BigSwitch(state, onToggle) }
            if (state.supportsBrightness) {
                item(key = "brightness") { Brightness(state, onLevel) }
            } else {
                item(key = "no_levels") {
                    Column {
                        SectionHeader(stringResource(R.string.flash_brightness))
                        ConnectedGroup { ConnectedRow(title = stringResource(R.string.flash_no_levels), supporting = stringResource(R.string.flash_no_levels_detail)) }
                    }
                }
            }
            item(key = "pulse") { PulseSection(pulse, onPulse, onGrantAccess) }
        }
    }
}

/** A large cookie-shaped button: the whole point of the page. */
@Composable
private fun BigSwitch(state: TorchState, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val haptics = rememberAppHaptics()
    val container by animateColorAsState(if (state.on) colors.primary else colors.surfaceContainerHighest, label = "torchContainer")
    val content by animateColorAsState(if (state.on) colors.onPrimary else colors.onSurfaceVariant, label = "torchContent")
    val stateText = stringResource(if (state.on) R.string.flash_on else R.string.flash_off)
    val name = stringResource(R.string.tile_flashlight)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            onClick = {
                haptics.toggle(!state.on)
                onToggle()
            },
            enabled = state.available,
            shape = MaterialShapes.Cookie12Sided.toShape(),
            color = container,
            modifier = Modifier.size(180.dp).semantics {
                role = Role.Switch
                contentDescription = name
                stateDescription = stateText
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (state.on) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        Text(
            text = if (state.available) stateText else stringResource(R.string.flash_busy),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun Brightness(state: TorchState, onLevel: (Int) -> Unit) {
    // The slider follows the finger on its own; the level is committed when the drag ends, so the
    // torch is not re-triggered dozens of times per second. Keyed on the level so outside changes
    // (the tile, presets) move the thumb.
    val slider = rememberSliderState(
        state.level.toFloat(),
        steps = (state.maxLevel - 2).coerceAtLeast(0),
        trackRange = 1f..state.maxLevel.toFloat()
    )
    LaunchedEffect(state.level) { slider.value = state.level.toFloat() }
    val presets = listOf(25, 50, 75, 100)
    val currentPercent = (state.level * 100f / state.maxLevel).roundToInt()
    Column {
        SectionHeader(stringResource(R.string.flash_brightness))
        ConnectedGroup {
            Surface(color = MaterialTheme.colorScheme.surfaceBright) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.flash_level, currentPercent), style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        state = slider,
                        onValueChange = { slider.value = it },
                        onValueChangeFinished = { onLevel(slider.value.roundToInt()) }
                    )
                }
            }
        }
        ConnectedChoices(
            options = presets,
            selected = presets.minByOrNull { kotlin.math.abs(it - currentPercent) } ?: 100,
            label = { "$it%" },
            onSelect = { percent -> onLevel(((percent / 100f) * state.maxLevel).roundToInt().coerceAtLeast(1)) },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** Blink for notifications. Works only with notification access, shared with the music widgets. */
@Composable
private fun PulseSection(pulse: PulseUi, onPulse: (Boolean) -> Unit, onGrantAccess: () -> Unit) {
    Column {
        SectionHeader(stringResource(R.string.flash_pulse_section))
        ConnectedGroup {
            ToggleRow(
                title = stringResource(R.string.flash_pulse),
                supporting = stringResource(R.string.flash_pulse_detail),
                icon = Icons.Rounded.NotificationsActive,
                checked = pulse.enabled && pulse.hasAccess,
                onCheckedChange = onPulse,
                enabled = pulse.hasAccess
            )
            if (!pulse.hasAccess) {
                ConnectedRow(
                    title = stringResource(R.string.flash_pulse_access),
                    supporting = stringResource(R.string.flash_pulse_access_detail),
                    onClick = onGrantAccess
                )
            }
        }
    }
}
