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

/** First-run and update state: whether the welcome was finished and which version was last seen. */
data class AppStartState(val onboardingDone: Boolean, val lastSeenVersion: String?)

interface AppStartRepository {
    val state: Flow<AppStartState>
    suspend fun finishOnboarding(version: String)
    suspend fun markVersionSeen(version: String)
    /** Shows the welcome again on next start, from Settings. */
    suspend fun resetOnboarding()
}

/** What to show when the app starts, decided purely from state and the running version. */
enum class StartScreen { WELCOME, WHATS_NEW, NONE }

fun startScreenFor(state: AppStartState, currentVersion: String): StartScreen = when {
    !state.onboardingDone -> StartScreen.WELCOME
    state.lastSeenVersion != currentVersion -> StartScreen.WHATS_NEW
    else -> StartScreen.NONE
}
