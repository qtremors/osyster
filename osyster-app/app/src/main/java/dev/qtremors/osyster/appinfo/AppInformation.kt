package dev.qtremors.osyster.appinfo

enum class AboutExternalLink(val url: String) {
    DEVELOPER("https://github.com/qtremors"),
    REPOSITORY("https://github.com/qtremors/osyster"),
    PRIVACY("https://github.com/qtremors/osyster/blob/main/PRIVACY.md"),
    RELEASES("https://github.com/qtremors/osyster/releases"),
    REPORT_ISSUE("https://github.com/qtremors/osyster/issues/new"),
    COMMUNITY_DISCORD("https://discord.gg/QgUjuNj9U8"),
    LICENSE("https://github.com/qtremors/osyster/blob/main/LICENSE.md")
}

data class AboutBuildInfo(
    val versionName: String,
    val applicationId: String,
    val buildType: String
) {
    val normalizedVersion: String get() = versionName.removeSuffix("-debug")
    val isDebug: Boolean
        get() = buildType.equals("debug", ignoreCase = true) || applicationId.endsWith(".debug")
    val displayVersion: String get() = if (isDebug) "$normalizedVersion (Debug)" else normalizedVersion
    val displayPackage: String get() = applicationId.ifBlank { "dev.qtremors.osyster" }
}

fun deviceDescription(
    manufacturer: String,
    model: String,
    androidRelease: String
): String {
    val deviceName = listOf(manufacturer.trim(), model.trim())
        .filter(String::isNotBlank)
        .distinctBy(String::lowercase)
        .joinToString(" ")
        .ifBlank { "Android device" }
    val release = androidRelease.trim().ifBlank { "unknown" }
    return "$deviceName (Android $release)"
}
