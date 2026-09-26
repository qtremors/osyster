package dev.qtremors.osyster.core.privilege.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.privilege.data.PowerAppsImpl
import dev.qtremors.osyster.core.privilege.data.PowerToolsStoreImpl
import dev.qtremors.osyster.core.privilege.data.PrivilegedShellImpl
import dev.qtremors.osyster.core.privilege.data.SystemSettingsImpl
import dev.qtremors.osyster.core.privilege.domain.PowerApps
import dev.qtremors.osyster.core.privilege.domain.PowerToolsStore
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SystemSettings

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PrivilegeDataModule {
    @Binds
    abstract fun shell(impl: PrivilegedShellImpl): PrivilegedShell

    @Binds
    abstract fun settings(impl: SystemSettingsImpl): SystemSettings

    @Binds
    abstract fun apps(impl: PowerAppsImpl): PowerApps

    @Binds
    abstract fun store(impl: PowerToolsStoreImpl): PowerToolsStore
}
