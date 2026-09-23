package dev.qtremors.osyster.core.usage.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.usage.data.AppStopperStore
import dev.qtremors.osyster.core.usage.data.DataUsageRepository
import dev.qtremors.osyster.core.usage.data.ScreenUsageRepository
import dev.qtremors.osyster.core.usage.domain.AppStopperRepository
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.ScreenUsageSource

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UsageDataModule {
    @Binds
    abstract fun dataUsage(repository: DataUsageRepository): DataUsageSource

    @Binds
    abstract fun screenUsage(repository: ScreenUsageRepository): ScreenUsageSource

    @Binds
    abstract fun appStopper(store: AppStopperStore): AppStopperRepository
}
