package dev.qtremors.osyster.feature.home

import android.os.Build
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.DevicePageGroup
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Static identity for the hero card. Live CPU, RAM, and battery readings join in v0.2.2. */
data class HeroIdentity(
    val name: String,
    val androidRelease: String,
    val apiLevel: Int,
    val securityPatch: String?,
    val chip: String?
)

data class HomeState(
    val hero: HeroIdentity,
    val groups: List<Pair<DevicePageGroup, List<DevicePage>>>
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(
        HomeState(
            hero = buildIdentity(),
            groups = DevicePage.entries.groupBy(DevicePage::group).toList()
        )
    )
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private fun buildIdentity(): HeroIdentity = HeroIdentity(
        name = displayName(Build.MANUFACTURER, Build.MODEL),
        androidRelease = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT,
        securityPatch = Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() },
        chip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
        } else {
            null
        }
    )

    companion object {
        /**
         * "Google" + "Pixel 7" reads "Pixel 7"; "samsung" + "SM-S918B" reads "Samsung SM-S918B".
         * Models that already start with the brand are not doubled.
         */
        fun displayName(manufacturer: String, model: String): String {
            val brand = manufacturer.trim().replaceFirstChar { it.titlecase() }
            val cleanModel = model.trim()
            return when {
                brand.isEmpty() -> cleanModel
                cleanModel.startsWith(brand, ignoreCase = true) -> cleanModel
                brand.equals("Google", ignoreCase = true) -> cleanModel
                else -> "$brand $cleanModel"
            }
        }
    }
}
