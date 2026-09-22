package dev.qtremors.osyster.core.device.data.format

import java.util.Locale

/** Pure calculations behind the Display and Network pages, kept apart so they can be tested. */
internal object DeviceMath {
    tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    /** "20:9" for 1080 × 2400; a decimal ratio when the fraction is unwieldy (odd panel sizes). */
    fun aspectRatio(width: Int, height: Int, locale: Locale = Locale.getDefault()): String? {
        if (width <= 0 || height <= 0) return null
        val short = minOf(width, height)
        val long = maxOf(width, height)
        val divisor = gcd(long, short)
        val a = long / divisor
        val b = short / divisor
        return if (a <= 64) "$a:$b" else String.format(locale, "%.2f:1", long.toFloat() / short)
    }

    /** Android's density bucket for a dpi value, rounding up to the nearest bucket. */
    fun densityBucket(dpi: Int): String = when {
        dpi <= 120 -> "ldpi"
        dpi <= 160 -> "mdpi"
        dpi <= 240 -> "hdpi"
        dpi <= 320 -> "xhdpi"
        dpi <= 480 -> "xxhdpi"
        else -> "xxxhdpi"
    }

    /** The Wi-Fi band a channel frequency belongs to. */
    fun wifiBand(mhz: Int): String = when (mhz) {
        in 2400..2500 -> "2.4 GHz"
        in 4900..5900 -> "5 GHz"
        in 5925..7125 -> "6 GHz"
        in 57000..71000 -> "60 GHz"
        else -> "$mhz MHz"
    }

    /** ScanResult.WIFI_STANDARD_* values as marketing names. */
    fun wifiStandard(standard: Int): String? = when (standard) {
        1 -> "Legacy (802.11a/b/g)"
        4 -> "Wi-Fi 4 (802.11n)"
        5 -> "Wi-Fi 5 (802.11ac)"
        6 -> "Wi-Fi 6 (802.11ax)"
        7 -> "802.11ad"
        8 -> "Wi-Fi 7 (802.11be)"
        else -> null
    }
}

/**
 * Reads fields from an RFC 2253 distinguished name, such as "CN=ISRG Root X1,O=Internet Security
 * Research Group,C=US". Commas inside quotes or escaped with a backslash stay part of the value.
 */
internal object DistinguishedName {
    fun field(rfc2253: String, name: String): String? = split(rfc2253)
        .firstOrNull { it.startsWith("$name=", ignoreCase = true) }
        ?.substringAfter('=')
        ?.trim()
        ?.removeSurrounding("\"")
        ?.replace("\\,", ",")
        ?.ifBlank { null }

    /** The most readable name: common name, then unit, then organization, then the raw text. */
    fun displayName(rfc2253: String): String =
        field(rfc2253, "CN") ?: field(rfc2253, "OU") ?: field(rfc2253, "O") ?: rfc2253

    private fun split(rfc2253: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var escaped = false
        for (char in rfc2253) {
            when {
                escaped -> { current.append(char); escaped = false }
                char == '\\' -> { current.append(char); escaped = true }
                char == '"' -> { current.append(char); quoted = !quoted }
                char == ',' && !quoted -> { parts += current.toString().trim(); current.clear() }
                else -> current.append(char)
            }
        }
        if (current.isNotBlank()) parts += current.toString().trim()
        return parts
    }
}
