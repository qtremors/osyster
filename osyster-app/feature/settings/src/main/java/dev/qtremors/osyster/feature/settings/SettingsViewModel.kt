package dev.qtremors.osyster.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.ui.theme.AccentSource
import dev.qtremors.osyster.core.ui.theme.ThemeMode
import dev.qtremors.osyster.core.ui.theme.ThemeOptions
import dev.qtremors.osyster.core.ui.theme.ThemePreset
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeSettings: ThemeSettings
) : ViewModel() {
    /** Null until the stored options load, so controls never flash the defaults. */
    val theme: StateFlow<ThemeOptions?> = themeSettings.options
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setMode(mode: ThemeMode) = update { it.copy(mode = mode) }
    fun setOled(enabled: Boolean) = update { it.copy(oled = enabled) }
    fun setAccentSource(source: AccentSource) = update { it.copy(accentSource = source) }
    fun setPreset(preset: ThemePreset) = update { it.copy(preset = preset, accentSource = AccentSource.PRESET) }
    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setReduceMotion(enabled: Boolean) = update { it.copy(reduceMotion = enabled) }

    private fun update(transform: (ThemeOptions) -> ThemeOptions) {
        viewModelScope.launch { themeSettings.update(transform) }
    }
}
