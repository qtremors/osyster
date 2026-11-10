package dev.qtremors.osyster.core.hid.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.hid.data.AndroidDeviceScanner
import dev.qtremors.osyster.core.hid.data.AndroidHidRemote
import dev.qtremors.osyster.core.hid.data.RemoteSettingsDataStore
import dev.qtremors.osyster.core.hid.domain.DeviceScanner
import dev.qtremors.osyster.core.hid.domain.HidRemote
import dev.qtremors.osyster.core.hid.domain.RemoteSettingsStore

@Module
@InstallIn(SingletonComponent::class)
internal abstract class HidDataModule {
    @Binds
    abstract fun remote(impl: AndroidHidRemote): HidRemote

    @Binds
    abstract fun scanner(impl: AndroidDeviceScanner): DeviceScanner

    @Binds
    abstract fun settings(impl: RemoteSettingsDataStore): RemoteSettingsStore
}
