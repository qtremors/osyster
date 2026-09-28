package dev.qtremors.osyster.core.device.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.domain.ReadingPreferences
import dev.qtremors.osyster.core.device.domain.ReadingPrefs
import dev.qtremors.osyster.core.device.domain.RefreshSpeed
import dev.qtremors.osyster.core.device.domain.TemperatureUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** SharedPreferences, so collectors can read the current choice synchronously on every tick. */
@Singleton
internal class ReadingPreferencesStore @Inject constructor(@ApplicationContext context: Context) : ReadingPreferences {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    override val prefs: StateFlow<ReadingPrefs> = _state.asStateFlow()

    override fun setTemperatureUnit(unit: TemperatureUnit) {
        store.edit().putString(KEY_UNIT, unit.name).apply()
        _state.value = _state.value.copy(temperatureUnit = unit)
    }

    override fun setRefresh(speed: RefreshSpeed) {
        store.edit().putString(KEY_REFRESH, speed.name).apply()
        _state.value = _state.value.copy(refresh = speed)
    }

    private fun read() = ReadingPrefs(
        temperatureUnit = TemperatureUnit.entries.firstOrNull { it.name == store.getString(KEY_UNIT, null) } ?: TemperatureUnit.CELSIUS,
        refresh = RefreshSpeed.entries.firstOrNull { it.name == store.getString(KEY_REFRESH, null) } ?: RefreshSpeed.NORMAL
    )

    companion object {
        const val FILE = "reading_preferences"
        private const val KEY_UNIT = "temperature_unit"
        private const val KEY_REFRESH = "refresh_speed"
    }
}
