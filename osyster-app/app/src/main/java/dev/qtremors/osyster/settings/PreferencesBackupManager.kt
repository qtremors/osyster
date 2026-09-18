package dev.qtremors.osyster.settings

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class OsysterBackupEnvelope(
    val appName: String = "Osyster",
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val preferences: OsysterBackupData
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class OsysterBackupData(
    val themeMode: String = ThemeMode.SYSTEM.name,
    val accentPalette: String = AccentPalette.CYAN.name,
    val dynamicColor: Boolean = false,
    val hapticFeedback: Boolean = true,
    val diagnosticsInterval: String = DiagnosticsInterval.INTERVAL_1000MS.name,
    val temperatureUnit: String = TemperatureUnit.CELSIUS.name,
    val showKernelThreads: Boolean = false,
    val appStopperGridColumns: Int = 4,
    val blockScreenCapture: Boolean = false,
    val screenTimeTargetMinutes: Int = 0,
    val preferSystemUsageHistory: Boolean = true
)

data class PreferencesBackupPreview(
    val items: List<PreferencesBackupItemPreview>
)

data class PreferencesBackupItemPreview(
    val title: String,
    val description: String
)

class PreferencesBackupManager(
    private val context: Context,
    private val preferencesManager: OsysterPreferencesManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentState = preferencesManager.state.value
            val backupData = OsysterBackupData(
                themeMode = currentState.themeMode.name,
                accentPalette = currentState.accentPalette.name,
                dynamicColor = currentState.dynamicColor,
                hapticFeedback = currentState.hapticFeedback,
                diagnosticsInterval = currentState.diagnosticsInterval.name,
                temperatureUnit = currentState.temperatureUnit.name,
                showKernelThreads = currentState.showKernelThreads,
                appStopperGridColumns = currentState.appStopperGridColumns,
                blockScreenCapture = currentState.blockScreenCapture,
                screenTimeTargetMinutes = currentState.screenTimeTargetMinutes,
                preferSystemUsageHistory = currentState.preferSystemUsageHistory
            )
            val envelope = OsysterBackupEnvelope(preferences = backupData)
            val jsonString = json.encodeToString(envelope)

            val outputStream: OutputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open destination file")
            outputStream.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        }
    }

    suspend fun preview(uri: Uri): Result<PreferencesBackupPreview> = withContext(Dispatchers.IO) {
        runCatching {
            val envelope = readEnvelope(uri)
            val prefs = envelope.preferences
            val items = listOf(
                PreferencesBackupItemPreview("Theme Mode", prefs.themeMode),
                PreferencesBackupItemPreview("Accent Palette", prefs.accentPalette),
                PreferencesBackupItemPreview("Dynamic Color", if (prefs.dynamicColor) "Enabled" else "Disabled"),
                PreferencesBackupItemPreview("Haptic Feedback", if (prefs.hapticFeedback) "Enabled" else "Disabled"),
                PreferencesBackupItemPreview("Diagnostics Interval", prefs.diagnosticsInterval),
                PreferencesBackupItemPreview("Temperature Unit", prefs.temperatureUnit),
                PreferencesBackupItemPreview("Show Kernel Threads", if (prefs.showKernelThreads) "Enabled" else "Disabled"),
                PreferencesBackupItemPreview("Block Screen Capture", if (prefs.blockScreenCapture) "Enabled" else "Disabled"),
                PreferencesBackupItemPreview("Screen Time Target", if (prefs.screenTimeTargetMinutes > 0) "${prefs.screenTimeTargetMinutes / 60}h ${prefs.screenTimeTargetMinutes % 60}m" else "None"),
                PreferencesBackupItemPreview("System Usage History", if (prefs.preferSystemUsageHistory) "Enabled" else "Disabled")
            )
            PreferencesBackupPreview(items = items)
        }
    }

    suspend fun restoreFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val envelope = readEnvelope(uri)
            val prefs = envelope.preferences

            runCatching { ThemeMode.valueOf(prefs.themeMode) }.getOrNull()?.let {
                preferencesManager.setThemeMode(it)
            }
            runCatching { AccentPalette.valueOf(prefs.accentPalette) }.getOrNull()?.let {
                preferencesManager.setAccentPalette(it)
            }
            preferencesManager.setDynamicColor(prefs.dynamicColor)
            preferencesManager.setHapticFeedback(prefs.hapticFeedback)
            runCatching { DiagnosticsInterval.valueOf(prefs.diagnosticsInterval) }.getOrNull()?.let {
                preferencesManager.setDiagnosticsInterval(it)
            }
            runCatching { TemperatureUnit.valueOf(prefs.temperatureUnit) }.getOrNull()?.let {
                preferencesManager.setTemperatureUnit(it)
            }
            preferencesManager.setShowKernelThreads(prefs.showKernelThreads)
            preferencesManager.setBlockScreenCapture(prefs.blockScreenCapture)
            preferencesManager.setScreenTimeTargetMinutes(prefs.screenTimeTargetMinutes)
            preferencesManager.setPreferSystemUsageHistory(prefs.preferSystemUsageHistory)
        }
    }

    private fun readEnvelope(uri: Uri): OsysterBackupEnvelope {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not read backup file")
        val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val envelope = json.decodeFromString<OsysterBackupEnvelope>(jsonString)
        require(envelope.appName.equals("Osyster", ignoreCase = true)) {
            "Invalid backup file: expected Osyster backup"
        }
        return envelope
    }
}
