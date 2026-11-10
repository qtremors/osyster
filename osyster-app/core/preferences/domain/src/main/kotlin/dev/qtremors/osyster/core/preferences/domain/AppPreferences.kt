package dev.qtremors.osyster.core.preferences.domain

import kotlinx.coroutines.flow.Flow

/**
 * Moves Osyster's settings between phones as one JSON file the user picks: theme, Home layout,
 * the app stopper list, and tool options. Never includes usage history or device data.
 * [uri] is a content URI string from the system file picker.
 */
interface SettingsBackup {
    suspend fun exportTo(uri: String): Result<Unit>
    suspend fun importFrom(uri: String): Result<Unit>
}

/** Thrown when a picked file is not an Osyster settings export. */
class NotAnOsysterBackupException : IllegalArgumentException("Not an Osyster settings file")

/** First-run state: whether the welcome was finished. */
data class AppStartState(val onboardingDone: Boolean)

interface AppStartRepository {
    val state: Flow<AppStartState>
    suspend fun finishOnboarding()
    /** Shows the welcome again on next start, from Settings. */
    suspend fun resetOnboarding()
}

/** What to show when the app starts: the welcome until it is finished, then nothing. */
enum class StartScreen { WELCOME, NONE }

fun startScreenFor(state: AppStartState): StartScreen = if (state.onboardingDone) StartScreen.NONE else StartScreen.WELCOME
