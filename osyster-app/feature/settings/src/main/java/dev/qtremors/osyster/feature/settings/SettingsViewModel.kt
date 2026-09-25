package dev.qtremors.osyster.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.preferences.domain.AppStartRepository
import dev.qtremors.osyster.core.preferences.domain.NotAnOsysterBackupException
import dev.qtremors.osyster.core.preferences.domain.SettingsBackup
import dev.qtremors.osyster.core.ui.theme.AccentSource
import dev.qtremors.osyster.core.ui.theme.ThemeMode
import dev.qtremors.osyster.core.ui.theme.ThemeOptions
import dev.qtremors.osyster.core.ui.theme.ThemePreset
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-off results shown as a snackbar. */
enum class SettingsMessage { EXPORTED, EXPORT_FAILED, IMPORTED, IMPORT_FAILED, NOT_A_BACKUP, WELCOME_RESET }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeSettings: ThemeSettings,
    private val backup: SettingsBackup,
    private val appStart: AppStartRepository
) : ViewModel() {
    /** Null until the stored options load, so controls never flash the defaults. */
    val theme: StateFlow<ThemeOptions?> = themeSettings.options
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<SettingsMessage?>(null)
    val message: StateFlow<SettingsMessage?> = _message.asStateFlow()

    fun setMode(mode: ThemeMode) = update { it.copy(mode = mode) }
    fun setOled(enabled: Boolean) = update { it.copy(oled = enabled) }
    fun setAccentSource(source: AccentSource) = update { it.copy(accentSource = source) }
    fun setPreset(preset: ThemePreset) = update { it.copy(preset = preset, accentSource = AccentSource.PRESET) }
    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setReduceMotion(enabled: Boolean) = update { it.copy(reduceMotion = enabled) }

    fun exportTo(uri: String) = viewModelScope.launch {
        _message.value = if (backup.exportTo(uri).isSuccess) SettingsMessage.EXPORTED else SettingsMessage.EXPORT_FAILED
    }

    fun importFrom(uri: String) = viewModelScope.launch {
        val result = backup.importFrom(uri)
        _message.value = when {
            result.isSuccess -> SettingsMessage.IMPORTED
            result.exceptionOrNull() is NotAnOsysterBackupException -> SettingsMessage.NOT_A_BACKUP
            else -> SettingsMessage.IMPORT_FAILED
        }
    }

    fun showWelcomeAgain() = viewModelScope.launch {
        appStart.resetOnboarding()
        _message.value = SettingsMessage.WELCOME_RESET
    }

    fun messageShown() {
        _message.value = null
    }

    private fun update(transform: (ThemeOptions) -> ThemeOptions) {
        viewModelScope.launch { themeSettings.update(transform) }
    }
}
