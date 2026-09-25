package dev.qtremors.osyster.core.system.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.system.data.AppActivitiesImpl
import dev.qtremors.osyster.core.system.data.FlashlightPulseImpl
import dev.qtremors.osyster.core.system.domain.FlashlightPulse
import dev.qtremors.osyster.core.system.data.LinkHandlersImpl
import dev.qtremors.osyster.core.system.data.SecureSettingsAccessImpl
import dev.qtremors.osyster.core.system.data.StayAwakeControllerImpl
import dev.qtremors.osyster.core.system.data.TorchControllerImpl
import dev.qtremors.osyster.core.system.domain.AppActivities
import dev.qtremors.osyster.core.system.domain.LinkHandlers
import dev.qtremors.osyster.core.system.domain.SecureSettingsAccess
import dev.qtremors.osyster.core.system.domain.StayAwakeController
import dev.qtremors.osyster.core.system.domain.TorchController

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SystemDataModule {
    @Binds abstract fun stayAwake(impl: StayAwakeControllerImpl): StayAwakeController
    @Binds abstract fun torch(impl: TorchControllerImpl): TorchController
    @Binds abstract fun activities(impl: AppActivitiesImpl): AppActivities
    @Binds abstract fun links(impl: LinkHandlersImpl): LinkHandlers
    @Binds abstract fun secureSettings(impl: SecureSettingsAccessImpl): SecureSettingsAccess
    @Binds abstract fun pulse(impl: FlashlightPulseImpl): FlashlightPulse
}
