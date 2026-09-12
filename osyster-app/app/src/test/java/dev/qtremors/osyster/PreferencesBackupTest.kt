package dev.qtremors.osyster

import dev.qtremors.osyster.settings.AccentPalette
import dev.qtremors.osyster.settings.DiagnosticsInterval
import dev.qtremors.osyster.settings.OsysterBackupData
import dev.qtremors.osyster.settings.OsysterBackupEnvelope
import dev.qtremors.osyster.settings.TemperatureUnit
import dev.qtremors.osyster.settings.ThemeMode
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesBackupTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun backupEnvelope_serializesAndDeserializesCorrectly() {
        val backupData = OsysterBackupData(
            themeMode = ThemeMode.DARK.name,
            accentPalette = AccentPalette.AMBER.name,
            dynamicColor = true,
            hapticFeedback = false,
            diagnosticsInterval = DiagnosticsInterval.INTERVAL_500MS.name,
            temperatureUnit = TemperatureUnit.FAHRENHEIT.name,
            showKernelThreads = true,
            appStopperGridColumns = 5,
            blockScreenCapture = true
        )
        val envelope = OsysterBackupEnvelope(
            appName = "Osyster",
            schemaVersion = 1,
            preferences = backupData
        )

        val jsonString = json.encodeToString(envelope)
        val decoded = json.decodeFromString<OsysterBackupEnvelope>(jsonString)

        assertEquals("Osyster", decoded.appName)
        assertEquals(1, decoded.schemaVersion)
        assertEquals(ThemeMode.DARK.name, decoded.preferences.themeMode)
        assertEquals(AccentPalette.AMBER.name, decoded.preferences.accentPalette)
        assertTrue(decoded.preferences.dynamicColor)
        assertEquals(false, decoded.preferences.hapticFeedback)
        assertEquals(DiagnosticsInterval.INTERVAL_500MS.name, decoded.preferences.diagnosticsInterval)
        assertEquals(TemperatureUnit.FAHRENHEIT.name, decoded.preferences.temperatureUnit)
        assertTrue(decoded.preferences.showKernelThreads)
        assertEquals(5, decoded.preferences.appStopperGridColumns)
        assertTrue(decoded.preferences.blockScreenCapture)
    }

    @Test
    fun backupEnvelope_defaultSchemaVersionIsOne() {
        val envelope = OsysterBackupEnvelope(
            preferences = OsysterBackupData()
        )
        assertEquals(1, envelope.schemaVersion)
        assertEquals("Osyster", envelope.appName)
    }
}
