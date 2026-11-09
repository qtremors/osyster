package dev.qtremors.osyster.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.preferences.domain.AppStartRepository
import dev.qtremors.osyster.core.preferences.domain.AppStartState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appStart: DataStore<Preferences> by preferencesDataStore("app_start")

@Singleton
class AppStartStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppStartRepository {
    private val onboardingDone = booleanPreferencesKey("onboarding_done")
    private val lastSeenVersion = stringPreferencesKey("last_seen_version")

    override val state: Flow<AppStartState> = context.appStart.data.map { prefs ->
        AppStartState(onboardingDone = prefs[onboardingDone] ?: false, lastSeenVersion = prefs[lastSeenVersion])
    }

    override suspend fun finishOnboarding(version: String) {
        context.appStart.edit {
            it[onboardingDone] = true
            // Someone who just walked through the welcome does not need "what's new" too.
            it[lastSeenVersion] = version
        }
    }

    override suspend fun markVersionSeen(version: String) {
        context.appStart.edit { it[lastSeenVersion] = version }
    }

    override suspend fun resetOnboarding() {
        context.appStart.edit { it[onboardingDone] = false }
    }
}
