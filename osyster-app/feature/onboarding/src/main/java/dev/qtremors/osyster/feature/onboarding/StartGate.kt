package dev.qtremors.osyster.feature.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.preferences.domain.StartScreen

/** System screens the welcome can send the user to. */
sealed interface OnboardingDestination {
    data object UsageAccess : OnboardingDestination
    data object NotificationAccess : OnboardingDestination
}

/**
 * Wraps the app: shows the welcome on first launch and "what's new" once after an update, on top
 * of [content]. The app underneath keeps running, so finishing the welcome lands straight on Home.
 */
@Composable
fun StartGate(
    appVersion: String,
    onNavigate: (OnboardingDestination) -> Unit,
    content: @Composable () -> Unit
) {
    val viewModel: StartGateViewModel = hiltViewModel()
    LaunchedEffect(appVersion) { viewModel.bind(appVersion) }
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshAccess()
        onPauseOrDispose { }
    }
    Box {
        content()
        when (screen) {
            StartScreen.WELCOME -> WelcomeScreen(access = access, onNavigate = onNavigate, onFinish = viewModel::finishWelcome)
            StartScreen.WHATS_NEW -> WhatsNewSheet(version = appVersion, onDismiss = viewModel::dismissWhatsNew)
            StartScreen.NONE, null -> Unit
        }
    }
}
