package dev.qtremors.osyster.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    override val state: Flow<AppStartState> = context.appStart.data.map { prefs ->
        AppStartState(onboardingDone = prefs[onboardingDone] ?: false)
    }

    override suspend fun finishOnboarding() {
        context.appStart.edit { it[onboardingDone] = true }
    }

    override suspend fun resetOnboarding() {
        context.appStart.edit { it[onboardingDone] = false }
    }
}
