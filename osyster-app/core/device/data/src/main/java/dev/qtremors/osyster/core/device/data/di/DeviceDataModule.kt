package dev.qtremors.osyster.core.device.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.device.data.DevicePageRepository
import dev.qtremors.osyster.core.device.data.DeviceSummaryRepository
import dev.qtremors.osyster.core.device.data.DeviceReportRepository
import dev.qtremors.osyster.core.device.data.HomeLayoutStore
import dev.qtremors.osyster.core.device.data.SensorReadingRepository
import dev.qtremors.osyster.core.device.data.DeviceGlanceRepository
import dev.qtremors.osyster.core.device.data.ReadingPreferencesStore
import dev.qtremors.osyster.core.device.domain.DeviceGlanceSource
import dev.qtremors.osyster.core.device.domain.DevicePageSource
import dev.qtremors.osyster.core.device.domain.ReadingPreferences
import dev.qtremors.osyster.core.device.domain.DeviceReportSource
import dev.qtremors.osyster.core.device.domain.SensorReadingSource
import dev.qtremors.osyster.core.device.domain.DeviceSummarySource
import dev.qtremors.osyster.core.device.domain.HomeLayoutRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DeviceDataModule {
    @Binds
    abstract fun pages(repository: DevicePageRepository): DevicePageSource

    @Binds
    abstract fun summary(repository: DeviceSummaryRepository): DeviceSummarySource

    @Binds
    abstract fun reading(store: ReadingPreferencesStore): ReadingPreferences

    @Binds
    abstract fun glances(repository: DeviceGlanceRepository): DeviceGlanceSource

    @Binds
    abstract fun homeLayout(store: HomeLayoutStore): HomeLayoutRepository

    @Binds
    abstract fun sensors(repository: SensorReadingRepository): SensorReadingSource

    @Binds
    abstract fun report(repository: DeviceReportRepository): DeviceReportSource
}
