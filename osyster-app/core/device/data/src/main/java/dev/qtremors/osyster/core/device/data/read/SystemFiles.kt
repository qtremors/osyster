package dev.qtremors.osyster.core.device.data.read

import java.io.File

/**
 * Reads the few procfs and sysfs files Android still lets apps read. Every read returns null
 * when the file is missing, unreadable, or blocked by SELinux, so callers can say "restricted"
 * instead of showing a believable zero.
 */
internal object SystemFiles {
    fun text(path: String): String? = runCatching {
        File(path).takeIf { it.canRead() }?.readText()?.trim()?.takeIf(String::isNotEmpty)
    }.getOrNull()

    fun long(path: String): Long? = text(path)?.toLongOrNull()

    fun lines(path: String): List<String>? = runCatching {
        File(path).takeIf { it.canRead() }?.readLines()
    }.getOrNull()

    fun exists(path: String): Boolean = runCatching { File(path).exists() }.getOrDefault(false)

    fun listDirs(path: String, prefix: String): List<File> = runCatching {
        File(path).listFiles { file -> file.name.startsWith(prefix) }?.toList().orEmpty()
    }.getOrDefault(emptyList())
}
