package dev.qtremors.osyster.core.system.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.system.domain.TorchController
import dev.qtremors.osyster.core.system.domain.TorchState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The back flash through CameraManager's torch API, which needs no camera permission. A torch
 * callback keeps [state] in sync when another app or Quick Settings changes it.
 */
@Singleton
internal class TorchControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TorchController {
    private val camera = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val prefs = context.getSharedPreferences("torch", Context.MODE_PRIVATE)
    private val cameraId: String? = camera?.let(::findFlash)
    private val maxLevel: Int = cameraId?.let(::maxStrength) ?: 1
    private var on = false

    private val _state = MutableStateFlow(
        TorchState(hasFlash = cameraId != null, available = cameraId != null, on = false, level = rememberedLevel(), maxLevel = maxLevel)
    )
    override val state: StateFlow<TorchState> = _state.asStateFlow()

    init {
        camera?.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id != cameraId) return
                on = enabled
                _state.value = _state.value.copy(on = enabled, available = true)
            }

            override fun onTorchModeUnavailable(id: String) {
                if (id == cameraId) _state.value = _state.value.copy(available = false, on = false)
            }

            override fun onTorchStrengthLevelChanged(id: String, newStrengthLevel: Int) {
                if (id == cameraId) _state.value = _state.value.copy(level = newStrengthLevel)
            }
        }, Handler(Looper.getMainLooper()))
    }

    override fun setOn(on: Boolean) {
        val id = cameraId ?: return
        runCatching {
            if (on && maxLevel > 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                camera?.turnOnTorchWithStrengthLevel(id, rememberedLevel())
            } else {
                camera?.setTorchMode(id, on)
            }
        }
    }

    override fun setLevel(level: Int) {
        val clamped = level.coerceIn(1, maxLevel)
        prefs.edit().putInt(KEY_LEVEL, clamped).apply()
        _state.value = _state.value.copy(level = clamped)
        val id = cameraId ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxLevel > 1) {
            runCatching { camera?.turnOnTorchWithStrengthLevel(id, clamped) }
        } else if (!on) {
            setOn(true)
        }
    }

    private fun rememberedLevel(): Int = prefs.getInt(KEY_LEVEL, maxLevel).coerceIn(1, maxLevel.coerceAtLeast(1))

    /** The back camera with a flash, or any camera with one. */
    private fun findFlash(manager: CameraManager): String? = runCatching {
        val withFlash = manager.cameraIdList.filter {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        withFlash.firstOrNull {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: withFlash.firstOrNull()
    }.getOrNull()

    private fun maxStrength(id: String): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { camera?.getCameraCharacteristics(id)?.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) }
                .getOrNull() ?: 1
        } else 1

    private companion object {
        const val KEY_LEVEL = "level"
    }
}
