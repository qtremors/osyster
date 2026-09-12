package dev.qtremors.osyster.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoutes {
    @Serializable
    data object Onboarding : AppRoutes

    @Serializable
    data object Bento : AppRoutes

    @Serializable
    data object Cpu : AppRoutes

    @Serializable
    data object Memory : AppRoutes

    @Serializable
    data object Processes : AppRoutes

    @Serializable
    data object DeviceInfo : AppRoutes

    @Serializable
    data object Settings : AppRoutes

    @Serializable
    data object About : AppRoutes

    @Serializable
    data object Licenses : AppRoutes

    @Serializable
    data object Network : AppRoutes

    @Serializable
    data object AppStopper : AppRoutes

    @Serializable
    data object Gpu : AppRoutes

    @Serializable
    data object Storage : AppRoutes

    @Serializable
    data object Battery : AppRoutes

    @Serializable
    data object Display : AppRoutes

    @Serializable
    data object System : AppRoutes

    @Serializable
    data object Camera : AppRoutes

    @Serializable
    data object Sensors : AppRoutes

    @Serializable
    data object Drm : AppRoutes
}
