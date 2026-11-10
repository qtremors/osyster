package dev.qtremors.osyster.core.barcode.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.barcode.domain.ScannerSettings
import dev.qtremors.osyster.core.barcode.domain.ScannerSettingsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.scannerStore: DataStore<Preferences> by preferencesDataStore("scanner")

@Singleton
internal class ScannerSettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ScannerSettingsStore {
    private val beep = booleanPreferencesKey("beep")
    private val vibrate = booleanPreferencesKey("vibrate")

    override val settings: Flow<ScannerSettings> = context.scannerStore.data.map {
        ScannerSettings(beep = it[beep] ?: true, vibrate = it[vibrate] ?: true)
    }

    override suspend fun update(transform: (ScannerSettings) -> ScannerSettings) {
        context.scannerStore.edit { prefs ->
            val next = transform(ScannerSettings(beep = prefs[beep] ?: true, vibrate = prefs[vibrate] ?: true))
            prefs[beep] = next.beep
            prefs[vibrate] = next.vibrate
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ScannerSettingsModule {
    @Binds
    abstract fun settings(impl: ScannerSettingsDataStore): ScannerSettingsStore
}
