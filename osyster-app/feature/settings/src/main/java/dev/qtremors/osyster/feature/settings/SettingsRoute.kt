package dev.qtremors.osyster.feature.settings

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.SettingsRoute

/** Where Settings wants to go next. The app shell maps these to intents or routes. */
sealed interface SettingsDestination {
    data class OpenUrl(val url: String) : SettingsDestination
}

fun NavGraphBuilder.registerSettingsRoute(
    appVersion: String,
    onNavigate: (SettingsDestination) -> Unit
) {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = hiltViewModel()
        val theme by viewModel.theme.collectAsStateWithLifecycle()
        SettingsScreen(
            theme = theme,
            appVersion = appVersion,
            actions = SettingsActions(
                onMode = viewModel::setMode,
                onOled = viewModel::setOled,
                onAccentSource = viewModel::setAccentSource,
                onPreset = viewModel::setPreset,
                onHaptics = viewModel::setHaptics,
                onReduceMotion = viewModel::setReduceMotion,
                onOpenUrl = { onNavigate(SettingsDestination.OpenUrl(it)) }
            )
        )
    }
}
