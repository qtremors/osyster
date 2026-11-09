package dev.qtremors.osyster.core.device.data.pages

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaDrm
import android.os.Build
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection
import java.util.UUID

/**
 * The Media and DRM page: Widevine and ClearKey details, then every codec Android lists. The DRM
 * device unique ID is never read.
 */
internal class MediaPageCollector(private val text: DeviceText) {

    fun collect(): List<InfoSection> = listOf(widevine(), clearKey()) + codecs()

    private fun widevine() = section(text, "widevine", R.string.section_widevine, "MediaDrm") {
        if (!MediaDrm.isCryptoSchemeSupported(WIDEVINE)) {
            row("supported", R.string.row_drm_supported, Availability.Unsupported("Widevine is not installed"))
            return@section
        }
        val drm = runCatching { MediaDrm(WIDEVINE) }.getOrNull()
        if (drm == null) {
            row("supported", R.string.row_drm_supported, Availability.Failed("Widevine did not open", retryable = true))
            return@section
        }
        try {
            fun property(name: String) = runCatching { drm.getPropertyString(name) }.getOrNull()
            yesNo("supported", R.string.row_drm_supported, true)
            row("level", R.string.row_security_level, property("securityLevel"))
            row("version", R.string.row_drm_version, property("version"))
            row("vendor", R.string.row_drm_vendor, property("vendor"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                row("hdcp", R.string.row_hdcp, hdcp(runCatching { drm.connectedHdcpLevel }.getOrDefault(MediaDrm.HDCP_LEVEL_UNKNOWN)), copyable = false)
                row("hdcp_max", R.string.row_hdcp_max, hdcp(runCatching { drm.maxHdcpLevel }.getOrDefault(MediaDrm.HDCP_LEVEL_UNKNOWN)), copyable = false)
                row("sessions", R.string.row_max_sessions, runCatching { drm.maxSessionCount }.getOrNull()?.toString(), advanced = true, copyable = false)
            }
            row("algorithms", R.string.row_algorithms, property("algorithms"), advanced = true)
            row("system_id", R.string.row_system_id, property("systemId"), advanced = true)
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) drm.close() else @Suppress("DEPRECATION") drm.release()
        }
    }

    private fun clearKey() = section(text, "clearkey", R.string.section_clearkey, "MediaDrm") {
        yesNo("supported", R.string.row_drm_supported, MediaDrm.isCryptoSchemeSupported(CLEARKEY))
    }

    private fun codecs(): List<InfoSection> {
        val infos = runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.toList() }.getOrDefault(emptyList())
        val groups = listOf(
            Triple("video_decoders", R.string.section_video_decoders) { c: MediaCodecInfo -> !c.isEncoder && c.supportedTypes.any { it.startsWith("video/") } },
            Triple("video_encoders", R.string.section_video_encoders) { c: MediaCodecInfo -> c.isEncoder && c.supportedTypes.any { it.startsWith("video/") } },
            Triple("audio_decoders", R.string.section_audio_decoders) { c: MediaCodecInfo -> !c.isEncoder && c.supportedTypes.any { it.startsWith("audio/") } },
            Triple("audio_encoders", R.string.section_audio_encoders) { c: MediaCodecInfo -> c.isEncoder && c.supportedTypes.any { it.startsWith("audio/") } }
        )
        return groups.mapNotNull { (key, title, filter) ->
            val codecs = infos.filter(filter).sortedWith(compareByDescending<MediaCodecInfo> { hardware(it) }.thenBy { it.name })
            if (codecs.isEmpty()) return@mapNotNull null
            val builder = SectionBuilder(text, "MediaCodecList")
            codecs.forEach { codec ->
                val kind = text[if (hardware(codec)) R.string.value_hardware else R.string.value_software]
                builder.row(
                    key = codec.name,
                    label = codec.supportedTypes.joinToString(", "),
                    value = builder.available("${codec.name} · $kind")
                )
            }
            builder.foldAfter(FOLD_AFTER)
            builder.build(key, text.format(R.string.section_codec_count, text[title], codecs.size))
        }
    }

    private fun hardware(codec: MediaCodecInfo): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) codec.isHardwareAccelerated
        else !codec.name.startsWith("OMX.google.") && !codec.name.startsWith("c2.android.")

    private fun hdcp(level: Int): String = when (level) {
        MediaDrm.HDCP_NONE -> text[R.string.value_none]
        MediaDrm.HDCP_V1 -> "HDCP 1.0"
        MediaDrm.HDCP_V2 -> "HDCP 2.0"
        MediaDrm.HDCP_V2_1 -> "HDCP 2.1"
        MediaDrm.HDCP_V2_2 -> "HDCP 2.2"
        MediaDrm.HDCP_V2_3 -> "HDCP 2.3"
        MediaDrm.HDCP_NO_DIGITAL_OUTPUT -> text[R.string.value_no_digital_output]
        else -> text[R.string.value_unknown]
    }

    private companion object {
        val WIDEVINE: UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        val CLEARKEY: UUID = UUID(-0x1d8e62a7567a4c37L, 0x781AB030AF78D30EL)
        const val FOLD_AFTER = 6
    }
}
