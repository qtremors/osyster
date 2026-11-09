package dev.qtremors.osyster.core.hid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.hid.domain.LayoutChoice
import dev.qtremors.osyster.core.hid.domain.RemoteInput
import dev.qtremors.osyster.core.hid.domain.RemoteNavigation
import dev.qtremors.osyster.core.hid.domain.RemoteSettings
import dev.qtremors.osyster.core.hid.domain.RemoteSettingsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Android backup leaves every DataStore out (backup_rules.xml), so the saved address stays on this phone. */
private val Context.remoteStore: DataStore<Preferences> by preferencesDataStore("bt_remote")

@Singleton
internal class RemoteSettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : RemoteSettingsStore {
    private object Keys {
        val navigation = stringPreferencesKey("navigation")
        val minimal = booleanPreferencesKey("minimal")
        val enterSelects = booleanPreferencesKey("enter_selects")
        val notificationButtons = booleanPreferencesKey("notification_buttons")
        val mouseSpeed = floatPreferencesKey("mouse_speed")
        val scrollSpeed = floatPreferencesKey("scroll_speed")
        val invertScroll = booleanPreferencesKey("invert_scroll")
        val airMouse = booleanPreferencesKey("air_mouse")
        val volumeKeys = booleanPreferencesKey("volume_keys")
        val lastDevice = stringPreferencesKey("last_device")
    }

    override val settings: Flow<RemoteSettings> = context.remoteStore.data.map { it.toSettings() }

    override suspend fun update(transform: (RemoteSettings) -> RemoteSettings) {
        context.remoteStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.navigation] = next.navigation.name
            prefs[Keys.minimal] = next.minimal
            prefs[Keys.enterSelects] = next.enterSelects
            prefs[Keys.notificationButtons] = next.notificationButtons
            prefs[Keys.mouseSpeed] = next.mouseSpeed.coerceIn(SPEED_RANGE)
            prefs[Keys.scrollSpeed] = next.scrollSpeed.coerceIn(SPEED_RANGE)
            prefs[Keys.invertScroll] = next.invertScroll
            prefs[Keys.airMouse] = next.airMouse
            prefs[Keys.volumeKeys] = next.volumeKeys
        }
    }

    override val deviceLayouts: Flow<Map<String, LayoutChoice>> = context.remoteStore.data.map { prefs ->
        prefs.asMap().mapNotNull { (key, value) ->
            val id = key.name.removePrefix(LAYOUT_PREFIX).takeIf { key.name.startsWith(LAYOUT_PREFIX) } ?: return@mapNotNull null
            val choice = LayoutChoice.entries.firstOrNull { it.name == value } ?: return@mapNotNull null
            id to choice
        }.toMap()
    }

    override suspend fun setDeviceLayout(id: String, choice: LayoutChoice) {
        val key = stringPreferencesKey(LAYOUT_PREFIX + id)
        context.remoteStore.edit { prefs ->
            // Automatic is the default, so it is stored as no entry at all.
            if (choice == LayoutChoice.AUTO) prefs.remove(key) else prefs[key] = choice.name
        }
    }

    override val deviceInputs: Flow<Map<String, RemoteInput>> = context.remoteStore.data.map { prefs ->
        prefs.asMap().mapNotNull { (key, value) ->
            val id = key.name.removePrefix(INPUT_PREFIX).takeIf { key.name.startsWith(INPUT_PREFIX) } ?: return@mapNotNull null
            val input = RemoteInput.entries.firstOrNull { it.name == value } ?: return@mapNotNull null
            id to input
        }.toMap()
    }

    override suspend fun setDeviceInput(id: String, input: RemoteInput) {
        context.remoteStore.edit { prefs -> prefs[stringPreferencesKey(INPUT_PREFIX + id)] = input.name }
    }

    override suspend fun lastDeviceId(): String? = context.remoteStore.data.first()[Keys.lastDevice]

    override suspend fun setLastDeviceId(id: String?) {
        context.remoteStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.lastDevice) else prefs[Keys.lastDevice] = id
        }
    }

    private fun Preferences.toSettings(): RemoteSettings {
        val defaults = RemoteSettings()
        return RemoteSettings(
            navigation = this[Keys.navigation]?.let { saved -> RemoteNavigation.entries.firstOrNull { it.name == saved } } ?: defaults.navigation,
            minimal = this[Keys.minimal] ?: defaults.minimal,
            enterSelects = this[Keys.enterSelects] ?: defaults.enterSelects,
            notificationButtons = this[Keys.notificationButtons] ?: defaults.notificationButtons,
            mouseSpeed = this[Keys.mouseSpeed] ?: defaults.mouseSpeed,
            scrollSpeed = this[Keys.scrollSpeed] ?: defaults.scrollSpeed,
            invertScroll = this[Keys.invertScroll] ?: defaults.invertScroll,
            airMouse = this[Keys.airMouse] ?: defaults.airMouse,
            volumeKeys = this[Keys.volumeKeys] ?: defaults.volumeKeys
        )
    }

    private companion object {
        val SPEED_RANGE = 0.5f..3f
        const val LAYOUT_PREFIX = "layout:"
        const val INPUT_PREFIX = "input:"
    }
}
