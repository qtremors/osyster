package dev.qtremors.osyster.preferences

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.preferences.domain.AppStartRepository
import dev.qtremors.osyster.core.preferences.domain.SettingsBackup

/**
 * Backup and first-run state live in the app module because they touch every feature's stores;
 * features see only the contracts.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds abstract fun backup(impl: AppSettingsBackup): SettingsBackup
    @Binds abstract fun appStart(impl: AppStartStore): AppStartRepository
}
