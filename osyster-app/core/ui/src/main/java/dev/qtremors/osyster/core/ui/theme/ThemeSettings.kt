package dev.qtremors.osyster.core.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Read and change the look of the app. Features depend on this, never on the store below. */
interface ThemeSettings {
    val options: Flow<ThemeOptions>
    suspend fun update(transform: (ThemeOptions) -> ThemeOptions)
}

/** File name of the theme DataStore; backup rules exclude it (settings move through Osyster's own export). */
const val UI_PREFERENCES_FILE = "ui_preferences"

private val Context.uiPreferences: DataStore<Preferences> by preferencesDataStore(UI_PREFERENCES_FILE)

@Singleton
internal class ThemeSettingsStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ThemeSettings {
    private object Keys {
        val mode = stringPreferencesKey("theme_mode")
        val oled = booleanPreferencesKey("oled")
        val accentSource = stringPreferencesKey("accent_source")
        val preset = stringPreferencesKey("preset")
        val customAccent = intPreferencesKey("custom_accent")
        val haptics = booleanPreferencesKey("haptics")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
    }

    override val options: Flow<ThemeOptions> = context.uiPreferences.data
        .map { prefs -> prefs.toOptions() }
        .distinctUntilChanged()

    override suspend fun update(transform: (ThemeOptions) -> ThemeOptions) {
        context.uiPreferences.edit { prefs ->
            val next = transform(prefs.toOptions())
            prefs[Keys.mode] = next.mode.name
            prefs[Keys.oled] = next.oled
            prefs[Keys.accentSource] = next.accentSource.name
            prefs[Keys.preset] = next.preset.name
            prefs[Keys.customAccent] = next.customAccent.toArgb()
            prefs[Keys.haptics] = next.hapticsEnabled
            prefs[Keys.reduceMotion] = next.reduceMotion
        }
    }

    private fun Preferences.toOptions(): ThemeOptions {
        val defaults = ThemeOptions()
        return ThemeOptions(
            mode = enumOrDefault(this[Keys.mode], defaults.mode),
            oled = this[Keys.oled] ?: defaults.oled,
            accentSource = enumOrDefault(this[Keys.accentSource], defaults.accentSource),
            preset = enumOrDefault(this[Keys.preset], defaults.preset),
            customAccent = this[Keys.customAccent]?.let { Color(it) } ?: defaults.customAccent,
            hapticsEnabled = this[Keys.haptics] ?: defaults.hapticsEnabled,
            reduceMotion = this[Keys.reduceMotion] ?: defaults.reduceMotion
        )
    }
}

/** Unknown or removed enum names fall back to the default instead of crashing. */
internal inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
    name?.let { stored -> enumValues<E>().firstOrNull { it.name == stored } } ?: default

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ThemeSettingsModule {
    @Binds
    abstract fun bindThemeSettings(store: ThemeSettingsStore): ThemeSettings
}
