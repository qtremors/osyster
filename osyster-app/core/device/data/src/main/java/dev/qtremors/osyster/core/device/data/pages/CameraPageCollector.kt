package dev.qtremors.osyster.core.device.data.pages

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection
import java.util.Locale

/**
 * The Cameras page, from Camera2 characteristics only. No camera is opened, so the camera
 * permission is never needed and the privacy indicator never lights up.
 */
internal class CameraPageCollector(private val context: Context, private val text: DeviceText) {

    fun collect(): List<InfoSection> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val ids = runCatching { manager?.cameraIdList?.toList() }.getOrNull().orEmpty()
        if (manager == null || ids.isEmpty()) {
            return listOf(section(text, "cameras", R.string.section_cameras_summary, "CameraManager") {
                row("count", R.string.row_camera_count, Availability.Unsupported("No cameras reported"))
            })
        }
        val cameras = ids.mapNotNull { id -> runCatching { id to manager.getCameraCharacteristics(id) }.getOrNull() }
        return listOf(summary(manager, cameras.size)) + cameras.map { (id, chars) -> camera(id, chars) }
    }

    private fun summary(manager: CameraManager, count: Int) = section(text, "cameras", R.string.section_cameras_summary, "CameraManager") {
        row("count", R.string.row_camera_count, count.toString(), copyable = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pairs = runCatching { manager.concurrentCameraIds.size }.getOrDefault(0)
            row("concurrent", R.string.row_concurrent_cameras, if (pairs > 0) text.format(R.string.value_combinations, pairs) else text[R.string.value_not_supported], copyable = false)
        }
    }

    private fun camera(id: String, c: CameraCharacteristics): InfoSection {
        val facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_BACK -> R.string.camera_back
            CameraCharacteristics.LENS_FACING_FRONT -> R.string.camera_front
            else -> R.string.camera_external
        }
        val builder = SectionBuilder(text, "Camera2")
        val pixels = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        builder.row("megapixels", R.string.row_megapixels, pixels?.let { String.format(Locale.getDefault(), "%.1f MP", it.width * it.height / 1_000_000f) }, copyable = false)
        builder.row("resolution", R.string.row_resolution, pixels?.let { "${it.width} × ${it.height}" })
        builder.row("aperture", R.string.row_aperture, c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.joinToString(", ") { String.format(Locale.getDefault(), "f/%.1f", it) })
        builder.row("focal", R.string.row_focal_length, c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.joinToString(", ") { String.format(Locale.getDefault(), "%.2f mm", it) })
        builder.yesNo("ois", R.string.row_ois, c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true)
        builder.yesNo("video_stabilization", R.string.row_video_stabilization, c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.any { it != CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF } == true)
        builder.yesNo("flash", R.string.row_flash, c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true)
        builder.row("level", R.string.row_camera2_level, text[hardwareLevel(c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL))], copyable = false)
        builder.row("sensor_size", R.string.row_sensor_size, c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.let { String.format(Locale.getDefault(), "%.2f × %.2f mm", it.width, it.height) }, advanced = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.row("zoom", R.string.row_zoom_range, c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.let { String.format(Locale.getDefault(), "%.1f× to %.1f×", it.lower, it.upper) }, advanced = true)
        }
        builder.row("digital_zoom", R.string.row_max_digital_zoom, c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?.let { String.format(Locale.getDefault(), "%.1f×", it) }, advanced = true)
        builder.row("iso", R.string.row_iso_range, c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { "ISO ${it.lower} to ${it.upper}" }, advanced = true)
        builder.row("fps", R.string.row_fps_ranges, c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.joinToString(", ") { "${it.lower}–${it.upper}" }, advanced = true)
        builder.row("capabilities", R.string.row_capabilities, capabilities(c), advanced = true)
        builder.row("orientation", R.string.row_sensor_orientation, c.get(CameraCharacteristics.SENSOR_ORIENTATION)?.let { "$it°" }, advanced = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val physical = c.physicalCameraIds
            if (physical.isNotEmpty()) builder.row("physical", R.string.row_physical_cameras, physical.sorted().joinToString(", "), advanced = true)
        }
        return builder.build("camera_$id", text.format(R.string.section_camera, text[facing], id))
    }

    private fun capabilities(c: CameraCharacteristics): String? = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        ?.toList()
        ?.mapNotNull { capability: Int ->
            when (capability) {
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> text[R.string.capability_manual]
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> text[R.string.capability_burst]
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> text[R.string.capability_depth]
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> text[R.string.capability_high_speed]
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> text[R.string.capability_logical]
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME -> text[R.string.capability_monochrome]
                else -> null
            }
        }?.takeIf { it.isNotEmpty() }?.joinToString(", ")

    private fun hardwareLevel(level: Int?): Int = when (level) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> R.string.camera_level_legacy
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> R.string.camera_level_limited
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> R.string.camera_level_full
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> R.string.camera_level_3
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> R.string.camera_level_external
        else -> R.string.value_unknown
    }
}
