package dev.qtremors.osyster.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.AboutRoute
import dev.qtremors.osyster.core.navigation.LicensesRoute
import dev.qtremors.osyster.core.navigation.PrivacyRoute
import dev.qtremors.osyster.core.navigation.SettingsRoute

/** Where Settings wants to go next. The app shell maps these to intents or routes. */
sealed interface SettingsDestination {
    data class OpenUrl(val url: String) : SettingsDestination
    data object About : SettingsDestination
    data object Privacy : SettingsDestination
    data object Licenses : SettingsDestination
}

fun NavGraphBuilder.registerSettingsRoute(
    appVersion: String,
    onNavigate: (SettingsDestination) -> Unit
) {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = hiltViewModel()
        val theme by viewModel.theme.collectAsStateWithLifecycle()
        val message by viewModel.message.collectAsStateWithLifecycle()
        // The user picks where the export goes and which file to import; Osyster gets access to
        // that one file only.
        val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { viewModel.exportTo(it.toString()) }
        }
        val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importFrom(it.toString()) }
        }
        val reading by viewModel.reading.collectAsStateWithLifecycle()
        SettingsScreen(
            reading = reading,
            theme = theme,
            message = message,
            actions = SettingsActions(
                onThemeChoice = viewModel::setThemeChoice,
                onAccentSource = viewModel::setAccentSource,
                onPreset = viewModel::setPreset,
                onHaptics = viewModel::setHaptics,
                onReduceMotion = viewModel::setReduceMotion,
                onBlockScreenshots = viewModel::setBlockScreenshots,
                onTemperatureUnit = viewModel::setTemperatureUnit,
                onRefresh = viewModel::setRefresh,
                onExport = { exporter.launch("osyster-settings-$appVersion.json") },
                onImport = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                onShowWelcome = viewModel::showWelcomeAgain,
                onMessageShown = viewModel::messageShown,
                onNavigate = onNavigate
            )
        )
    }
    composable<AboutRoute> {
        AboutScreen(
            appVersion = appVersion,
            onOpenUrl = { onNavigate(SettingsDestination.OpenUrl(it)) },
            onPrivacy = { onNavigate(SettingsDestination.Privacy) },
            onLicenses = { onNavigate(SettingsDestination.Licenses) }
        )
    }
    composable<PrivacyRoute> { PrivacyScreen() }
    composable<LicensesRoute> { LicensesScreen() }
}
