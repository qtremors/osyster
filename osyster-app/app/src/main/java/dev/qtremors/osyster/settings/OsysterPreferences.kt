package dev.qtremors.osyster.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// =========================================================================
// Enums & Preference Contracts
// =========================================================================

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    OLED
}

enum class AccentPalette(val primaryColor: Color?, val label: String) {
    CYAN(Color(0xFF00E6FF), "Cyan"),
    AMBER(Color(0xFFFFB300), "Amber"),
    ROSE(Color(0xFFFF5252), "Rose"),
    EMERALD(Color(0xFF00E676), "Emerald"),
    PURPLE(Color(0xFFB388FF), "Purple"),
    BLUE(Color(0xFF2979FF), "Blue"),
    DYNAMIC(null, "Dynamic"),
    MONOCHROME(Color(0xFFE0E0E0), "Monochrome")
}

enum class DiagnosticsInterval(val millis: Long, val labelResName: String) {
    INTERVAL_500MS(500L, "interval_fast"),
    INTERVAL_1000MS(1000L, "interval_normal"),
    INTERVAL_2000MS(2000L, "interval_relaxed"),
    INTERVAL_3000MS(3000L, "interval_saver")
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun convertFromCelsius(celsius: Float): Float {
        return when (this) {
            CELSIUS -> celsius
            FAHRENHEIT -> (celsius * 9f / 5f) + 32f
        }
    }

    fun format(celsius: Float): String {
        val converted = convertFromCelsius(celsius)
        return "%.1f %s".format(converted, symbol)
    }
}

data class OsysterPreferencesState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.CYAN,
    val dynamicColor: Boolean = false,
    val hapticFeedback: Boolean = true,
    val diagnosticsInterval: DiagnosticsInterval = DiagnosticsInterval.INTERVAL_1000MS,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val showKernelThreads: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val managedStopPackages: Set<String> = emptySet(),
    val appStopperGridColumns: Int = 4
)

// =========================================================================
// Reactive Preference Manager
// =========================================================================

class OsysterPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<OsysterPreferencesState> = _state.asStateFlow()

    private fun loadInitialState(): OsysterPreferencesState {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val accentStr = prefs.getString(KEY_ACCENT_PALETTE, AccentPalette.CYAN.name) ?: AccentPalette.CYAN.name
        val intervalStr = prefs.getString(KEY_DIAGNOSTICS_INTERVAL, DiagnosticsInterval.INTERVAL_1000MS.name) ?: DiagnosticsInterval.INTERVAL_1000MS.name
        val tempUnitStr = prefs.getString(KEY_TEMP_UNIT, TemperatureUnit.CELSIUS.name) ?: TemperatureUnit.CELSIUS.name

        val managedRaw = prefs.getString(KEY_MANAGED_STOP_PACKAGES, "") ?: ""
        val managedPackages = if (managedRaw.isNotBlank()) {
            managedRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } else {
            emptySet()
        }

        val gridCols = prefs.getInt(KEY_APP_STOPPER_GRID_COLUMNS, 4).coerceIn(4, 6)

        return OsysterPreferencesState(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM),
            accentPalette = runCatching { AccentPalette.valueOf(accentStr) }.getOrDefault(AccentPalette.CYAN),
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true),
            diagnosticsInterval = runCatching { DiagnosticsInterval.valueOf(intervalStr) }.getOrDefault(DiagnosticsInterval.INTERVAL_1000MS),
            temperatureUnit = runCatching { TemperatureUnit.valueOf(tempUnitStr) }.getOrDefault(TemperatureUnit.CELSIUS),
            showKernelThreads = prefs.getBoolean(KEY_SHOW_KERNEL_THREADS, false),
            isOnboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false),
            managedStopPackages = managedPackages,
            appStopperGridColumns = gridCols
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun setAccentPalette(palette: AccentPalette) {
        prefs.edit().putString(KEY_ACCENT_PALETTE, palette.name).apply()
        _state.value = _state.value.copy(accentPalette = palette)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _state.value = _state.value.copy(dynamicColor = enabled)
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
        _state.value = _state.value.copy(hapticFeedback = enabled)
    }

    fun setDiagnosticsInterval(interval: DiagnosticsInterval) {
        prefs.edit().putString(KEY_DIAGNOSTICS_INTERVAL, interval.name).apply()
        _state.value = _state.value.copy(diagnosticsInterval = interval)
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        prefs.edit().putString(KEY_TEMP_UNIT, unit.name).apply()
        _state.value = _state.value.copy(temperatureUnit = unit)
    }

    fun setShowKernelThreads(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_KERNEL_THREADS, enabled).apply()
        _state.value = _state.value.copy(showKernelThreads = enabled)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _state.value = _state.value.copy(isOnboardingCompleted = completed)
    }

    fun setManagedStopPackages(packages: Set<String>) {
        val serialized = packages.filter { it.isNotBlank() }.joinToString(",")
        prefs.edit().putString(KEY_MANAGED_STOP_PACKAGES, serialized).apply()
        _state.value = _state.value.copy(managedStopPackages = packages)
    }

    fun addManagedStopPackage(packageName: String) {
        if (packageName.isBlank()) return
        val current = _state.value.managedStopPackages
        val updated = current + packageName.trim()
        setManagedStopPackages(updated)
    }

    fun removeManagedStopPackage(packageName: String) {
        val current = _state.value.managedStopPackages
        val updated = current - packageName.trim()
        setManagedStopPackages(updated)
    }

    fun setAppStopperGridColumns(columns: Int) {
        val clamped = columns.coerceIn(4, 6)
        prefs.edit().putInt(KEY_APP_STOPPER_GRID_COLUMNS, clamped).apply()
        _state.value = _state.value.copy(appStopperGridColumns = clamped)
    }

    companion object {
        private const val PREFS_NAME = "osyster_settings_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_PALETTE = "accent_palette"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_DIAGNOSTICS_INTERVAL = "diagnostics_interval"
        private const val KEY_TEMP_UNIT = "temperature_unit"
        private const val KEY_SHOW_KERNEL_THREADS = "show_kernel_threads"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_MANAGED_STOP_PACKAGES = "managed_stop_packages"
        private const val KEY_APP_STOPPER_GRID_COLUMNS = "app_stopper_grid_columns"

        @Volatile
        private var instance: OsysterPreferencesManager? = null

        fun getInstance(context: Context): OsysterPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: OsysterPreferencesManager(context).also { instance = it }
            }
        }
    }
}
