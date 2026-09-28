package dev.qtremors.osyster.preferences

import dev.qtremors.osyster.core.device.domain.ReadingPreferences
import dev.qtremors.osyster.core.device.domain.RefreshSpeed
import dev.qtremors.osyster.core.device.domain.TemperatureUnit
import dev.qtremors.osyster.core.privilege.domain.PowerToolsStore
import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.HomeLayout
import dev.qtremors.osyster.core.device.domain.HomeLayoutRepository
import dev.qtremors.osyster.core.device.domain.PinKey
import dev.qtremors.osyster.core.preferences.domain.NotAnOsysterBackupException
import dev.qtremors.osyster.core.preferences.domain.SettingsBackup
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.system.domain.FlashlightPulse
import dev.qtremors.osyster.core.ui.theme.ThemeOptions
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import dev.qtremors.osyster.core.usage.domain.AppStopperRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The settings file format. [format] identifies the file; [version] allows future migrations. */
@Serializable
internal data class SettingsFile(
    val format: String = FORMAT,
    /** 2 added readings and the power tool lists; version 1 files still import, with defaults. */
    val version: Int = 2,
    val theme: ThemeEntry,
    val home: HomeEntry,
    val appStopper: List<String>,
    val flashlightPulse: Boolean,
    val readings: ReadingsEntry = ReadingsEntry(),
    val quietApps: List<String> = emptyList(),
    val freezeList: List<String> = emptyList()
) {
    companion object {
        const val FORMAT = "osyster-settings"
    }
}

@Serializable
internal data class ThemeEntry(
    val mode: String,
    val oled: Boolean,
    val accentSource: String,
    val preset: String,
    val customAccent: Int,
    val haptics: Boolean,
    val reduceMotion: Boolean,
    val blockScreenshots: Boolean = false
)

@Serializable
internal data class ReadingsEntry(val temperatureUnit: String = "CELSIUS", val refresh: String = "NORMAL")

@Serializable
internal data class HomeEntry(val order: List<String>, val hidden: List<String>, val pinned: List<String>)

/**
 * Writes and reads the settings file through the document the user picked. Unknown names in an
 * imported file (from a newer or older version) fall back to defaults instead of failing.
 */
@Singleton
class AppSettingsBackup @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
    private val theme: ThemeSettings,
    private val home: HomeLayoutRepository,
    private val stopper: AppStopperRepository,
    private val pulse: FlashlightPulse,
    private val reading: ReadingPreferences,
    private val powerStore: PowerToolsStore
) : SettingsBackup {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun exportTo(uri: String): Result<Unit> = withContext(dispatchers.io) {
        runCatchingCancellable {
            val file = snapshot()
            context.contentResolver.openOutputStream(Uri.parse(uri), "wt")?.use { out ->
                out.write(json.encodeToString(SettingsFile.serializer(), file).toByteArray())
            } ?: error("No output stream")
            Unit
        }
    }

    override suspend fun importFrom(uri: String): Result<Unit> = withContext(dispatchers.io) {
        runCatchingCancellable {
            val text = context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes().decodeToString() }
                ?: error("No input stream")
            val file = runCatching { json.decodeFromString(SettingsFile.serializer(), text) }.getOrNull()
            if (file == null || file.format != SettingsFile.FORMAT) throw NotAnOsysterBackupException()
            apply(file)
        }
    }

    private suspend fun snapshot(): SettingsFile {
        val options = theme.options.first()
        val layout = home.layout.first()
        return SettingsFile(
            theme = ThemeEntry(
                mode = options.mode.name,
                oled = options.oled,
                accentSource = options.accentSource.name,
                preset = options.preset.name,
                customAccent = options.customAccent.toArgb(),
                haptics = options.hapticsEnabled,
                reduceMotion = options.reduceMotion,
                blockScreenshots = options.blockScreenshots
            ),
            home = HomeEntry(
                order = layout.pageOrder.map(DevicePage::id),
                hidden = layout.hiddenPages.map(DevicePage::id),
                pinned = layout.pinned.map(PinKey::raw)
            ),
            appStopper = stopper.apps.first().map { it.packageName },
            flashlightPulse = pulse.enabled.value,
            readings = reading.prefs.value.let { ReadingsEntry(it.temperatureUnit.name, it.refresh.name) },
            quietApps = powerStore.quietApps.value.sorted(),
            freezeList = powerStore.freezeList.value.sorted()
        )
    }

    private suspend fun apply(file: SettingsFile) {
        val defaults = ThemeOptions()
        val t = file.theme
        theme.update {
            ThemeOptions(
                mode = enumOr(t.mode, defaults.mode),
                oled = t.oled,
                accentSource = enumOr(t.accentSource, defaults.accentSource),
                preset = enumOr(t.preset, defaults.preset),
                customAccent = Color(t.customAccent),
                hapticsEnabled = t.haptics,
                reduceMotion = t.reduceMotion,
                blockScreenshots = t.blockScreenshots
            )
        }
        home.update {
            HomeLayout(
                pageOrder = DevicePage.restoreOrder(file.home.order),
                hiddenPages = file.home.hidden.mapNotNull(DevicePage::fromId).toSet(),
                pinned = file.home.pinned.mapNotNull(PinKey::parse).distinct()
            )
        }
        stopper.add(file.appStopper)
        pulse.setEnabled(file.flashlightPulse)
        reading.setTemperatureUnit(enumOr(file.readings.temperatureUnit, TemperatureUnit.CELSIUS))
        reading.setRefresh(enumOr(file.readings.refresh, RefreshSpeed.NORMAL))
        // Lists merge with what is already there, like the app stopper list.
        powerStore.setQuietApps(powerStore.quietApps.value + file.quietApps)
        powerStore.setFreezeList(powerStore.freezeList.value + file.freezeList)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
