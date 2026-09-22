package dev.qtremors.osyster.core.device.data.pages

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.RoundedCorner
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.format.DeviceMath
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** The Display page: panel size and density, refresh rates, HDR, color, and cutouts. */
internal class DisplayPageCollector(private val context: Context, private val text: DeviceText) {

    fun collect(): List<InfoSection> {
        val display = (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return listOf(section(text, "panel", R.string.section_panel, "DisplayManager") {
                row("resolution", R.string.row_resolution, Availability.Unavailable("No default display", retryable = true))
            })
        return listOf(panel(display), refresh(display), color(display), shape(display))
    }

    @Suppress("DEPRECATION")
    private fun panel(display: Display) = section(text, "panel", R.string.section_panel, "Display") {
        val mode = display.mode
        val width = minOf(mode.physicalWidth, mode.physicalHeight)
        val height = maxOf(mode.physicalWidth, mode.physicalHeight)
        val metrics = DisplayMetrics().also(display::getRealMetrics)
        row("resolution", R.string.row_resolution, "$width × $height px")
        row("size", R.string.row_screen_size, diagonal(metrics)?.let { String.format(Locale.getDefault(), "%.2f in", it) }, copyable = false)
        row("aspect", R.string.row_aspect_ratio, DeviceMath.aspectRatio(width, height), copyable = false)
        row("density", R.string.row_density, "${metrics.densityDpi} dpi (${DeviceMath.densityBucket(metrics.densityDpi)})")
        row("ppi", R.string.row_pixel_density, if (metrics.xdpi > 0 && metrics.ydpi > 0) {
            String.format(Locale.getDefault(), "%.0f × %.0f ppi", metrics.xdpi, metrics.ydpi)
        } else null, copyable = false)
        row("name", R.string.row_display_name, display.name, advanced = true)
    }

    private fun refresh(display: Display) = section(text, "refresh", R.string.section_refresh, "Display.Mode") {
        val rates = display.supportedModes.map { it.refreshRate.roundToInt() }.distinct().sorted()
        row("current_rate", R.string.row_current_refresh, "${display.refreshRate.roundToInt()} Hz", copyable = false)
        row("supported_rates", R.string.row_supported_refresh, rates.joinToString(", ") { "$it Hz" })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alternatives = display.mode.alternativeRefreshRates.map { it.roundToInt() }.distinct().sorted()
            row("seamless", R.string.row_seamless_refresh, if (alternatives.isEmpty()) text[R.string.value_none] else alternatives.joinToString(", ") { "$it Hz" }, advanced = true)
        }
        row("modes", R.string.row_display_modes, display.supportedModes.joinToString("\n") {
            "${it.physicalWidth} × ${it.physicalHeight} · ${String.format(Locale.getDefault(), "%.2f", it.refreshRate)} Hz"
        }, advanced = true)
    }

    private fun color(display: Display) = section(text, "color", R.string.section_color_hdr, "Display") {
        val hdrTypes = hdrTypes(display)
        row("hdr", R.string.row_hdr_formats, if (hdrTypes.isEmpty()) text[R.string.value_not_supported] else hdrTypes.joinToString(", "))
        yesNo("wide_gamut", R.string.row_wide_color_gamut, display.isWideColorGamut)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && display.isHdrSdrRatioAvailable) {
            row("hdr_ratio", R.string.row_hdr_headroom, String.format(Locale.getDefault(), "%.2f×", display.hdrSdrRatio), copyable = false)
        }
        @Suppress("DEPRECATION")
        val caps = display.hdrCapabilities
        if (caps != null && caps.desiredMaxLuminance > 0f) {
            row("luminance", R.string.row_hdr_luminance, String.format(Locale.getDefault(), "%.0f nits peak · %.0f nits average", caps.desiredMaxLuminance, caps.desiredMaxAverageLuminance), advanced = true)
        }
    }

    private fun shape(display: Display) = section(text, "shape", R.string.section_shape, "Display") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cutout = display.cutout
            row("cutout", R.string.row_cutout, when {
                cutout == null -> text[R.string.value_none]
                cutout.boundingRects.size > 1 -> text.format(R.string.value_cutouts, cutout.boundingRects.size)
                else -> cutout.boundingRects.firstOrNull()?.let { text.format(R.string.value_cutout_size, it.width(), it.height()) }
            }, copyable = false)
        } else {
            row("cutout", R.string.row_cutout, Availability.Unsupported("Needs Android 10"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val radius = display.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius
            row("corners", R.string.row_rounded_corners, radius?.let { text.format(R.string.value_corner_radius, it) } ?: text[R.string.value_none], copyable = false)
        }
    }

    private fun hdrTypes(display: Display): List<String> {
        val types = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            display.mode.supportedHdrTypes
        } else {
            @Suppress("DEPRECATION")
            display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
        }
        return types.distinct().map { type ->
            when (type) {
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                else -> "HDR type $type"
            }
        }
    }

    private fun diagonal(metrics: DisplayMetrics): Double? {
        if (metrics.xdpi <= 0f || metrics.ydpi <= 0f) return null
        val w = metrics.widthPixels / metrics.xdpi
        val h = metrics.heightPixels / metrics.ydpi
        return sqrt((w * w + h * h).toDouble())
    }
}
