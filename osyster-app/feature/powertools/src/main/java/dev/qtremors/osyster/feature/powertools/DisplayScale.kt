package dev.qtremors.osyster.feature.powertools

import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.privilege.domain.DensityInfo
import dev.qtremors.osyster.core.privilege.domain.Keys
import dev.qtremors.osyster.core.privilege.domain.PowerCommands
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.privilege.domain.parseDensity
import dev.qtremors.osyster.core.privilege.domain.readFloat
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScaleState(val animation: Float = 1f, val font: Float = 1f)

@HiltViewModel
class DisplayScaleViewModel @Inject constructor(
    shell: PrivilegedShell,
    settings: SystemSettings
) : PowerViewModel(shell, settings) {
    val state = settingsState(setOf(Keys.WindowAnimation, Keys.FontScale), ScaleState()) {
        ScaleState(
            animation = settings.readFloat(Keys.WindowAnimation) ?: 1f,
            font = settings.readFloat(Keys.FontScale) ?: 1f
        )
    }

    private val _density = MutableStateFlow<DensityInfo?>(null)
    /** Null until read, and when there is no shell to read it with. */
    val density = _density.asStateFlow()

    fun loadDensity() {
        viewModelScope.launch {
            if (!access.value.shell) return@launch
            val result = shell.run(PowerCommands.readDensity)
            _density.value = if (result.ok) parseDensity(result.output) else null
        }
    }

    /** All three animation scales move together, the way most people want them. */
    fun setAnimation(scale: Float) {
        viewModelScope.launch {
            val value = scale.toString()
            listOf(Keys.WindowAnimation, Keys.TransitionAnimation, Keys.AnimatorDuration).forEach { key ->
                if (!writeNow(key, value)) return@launch
            }
        }
    }

    fun setFont(scale: Float) = write(Keys.FontScale, scale.toString())

    fun setDensity(dpi: Int?) {
        viewModelScope.launch {
            if (!access.value.shell) {
                say(PowerMessage.NEEDS_SHELL)
                return@launch
            }
            val command = if (dpi == null) PowerCommands.resetDensity else PowerCommands.setDensity(dpi)
            if (!shell.run(command).ok) say(PowerMessage.FAILED)
            loadDensity()
        }
    }
}

@Composable
internal fun DisplayScaleRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    val viewModel: DisplayScaleViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density by viewModel.density.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    LifecycleResumeEffect(access.shell) {
        viewModel.refresh()
        viewModel.loadDensity()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbar)
    val enabled = access.canWriteSettings
    OsysterScaffold(title = stringResource(R.string.display_scale_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "gate") { AccessCard(access, needsShell = false) { onNavigate(PowerToolsDestination.Setup) } }
            item(key = "animation") {
                SectionHeader(stringResource(R.string.display_scale_motion))
                ConnectedGroup {
                    ChoiceRow(
                        title = stringResource(R.string.display_scale_animation),
                        supporting = stringResource(R.string.display_scale_animation_summary),
                        options = ANIMATION_SCALES,
                        selected = ANIMATION_SCALES.minBy { abs(it - state.animation) },
                        label = { scale -> if (scale == 0f) stringResource(R.string.display_scale_off) else stringResource(R.string.display_scale_times, scale.toString()) },
                        onSelect = { if (enabled) viewModel.setAnimation(it) },
                        icon = Icons.Rounded.Animation
                    )
                }
            }
            item(key = "font") {
                SectionHeader(stringResource(R.string.display_scale_text))
                ConnectedGroup {
                    ChoiceRow(
                        title = stringResource(R.string.display_scale_font),
                        supporting = stringResource(R.string.display_scale_font_summary),
                        options = FONT_SCALES,
                        selected = FONT_SCALES.minBy { abs(it - state.font) },
                        label = { scale -> stringResource(R.string.power_percent, (scale * 100).roundToInt()) },
                        onSelect = { if (enabled) viewModel.setFont(it) },
                        icon = Icons.Rounded.FormatSize
                    )
                }
            }
            item(key = "density") { DensitySection(density, access.shell, viewModel::setDensity) }
        }
    }
}

@Composable
private fun DensitySection(density: DensityInfo?, hasShell: Boolean, onSet: (Int?) -> Unit) {
    SectionHeader(stringResource(R.string.display_scale_size))
    ConnectedGroup {
        val physical = density?.physical
        val options = if (physical == null) DENSITY_FACTORS.map { 0 } else DENSITY_FACTORS.map { (physical * it).roundToInt() }
        ChoiceRow(
            title = stringResource(R.string.display_scale_density),
            supporting = when {
                !hasShell -> stringResource(R.string.display_scale_density_needs_shell)
                density == null -> stringResource(R.string.display_scale_density_loading)
                else -> stringResource(R.string.display_scale_density_summary, density.current, density.physical)
            },
            options = DENSITY_FACTORS.indices.toList(),
            selected = density?.let { info -> options.indices.minBy { abs(options[it] - info.current) } } ?: DEFAULT_DENSITY_INDEX,
            label = { index -> stringResource(DENSITY_LABELS[index]) },
            onSelect = { index -> if (hasShell && physical != null) onSet(if (index == DEFAULT_DENSITY_INDEX) null else options[index]) },
            icon = Icons.Rounded.ZoomIn
        )
    }
    if (density?.override != null) {
        ActionButtons {
            OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = { onSet(null) }) { Text(stringResource(R.string.display_scale_density_reset)) }
        }
    }
}

private val ANIMATION_SCALES = listOf(0f, 0.5f, 1f, 1.5f, 2f)
private val FONT_SCALES = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f, 1.8f, 2f)

/** Smaller numbers mean a smaller density, which fits more on screen. */
private val DENSITY_FACTORS = listOf(0.85f, 0.92f, 1f, 1.08f, 1.15f)
private const val DEFAULT_DENSITY_INDEX = 2
private val DENSITY_LABELS = listOf(
    R.string.display_scale_density_smallest,
    R.string.display_scale_density_small,
    R.string.display_scale_density_default,
    R.string.display_scale_density_large,
    R.string.display_scale_density_largest
)
