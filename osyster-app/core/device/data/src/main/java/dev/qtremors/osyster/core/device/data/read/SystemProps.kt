package dev.qtremors.osyster.core.device.data.read

import java.util.concurrent.TimeUnit

/**
 * Reads read-only system properties through the `getprop` tool every app may run. This avoids
 * the hidden SystemProperties API; a property SELinux hides simply comes back empty.
 */
internal object SystemProps {
    fun get(name: String): String? = runCatching {
        val process = ProcessBuilder("getprop", name).redirectErrorStream(true).start()
        try {
            if (!process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) return null
            process.inputStream.bufferedReader().readText().trim().takeIf(String::isNotEmpty)
        } finally {
            process.destroy()
        }
    }.getOrNull()

    private const val TIMEOUT_MS = 1_500L
}
