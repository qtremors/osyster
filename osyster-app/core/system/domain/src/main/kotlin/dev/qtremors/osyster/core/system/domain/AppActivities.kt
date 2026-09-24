package dev.qtremors.osyster.core.system.domain

/** An app that has at least one activity. */
data class AppEntry(val packageName: String, val label: String, val activityCount: Int)

/**
 * One screen inside an app. Only [exported] activities can be opened by other apps; the rest are
 * listed for completeness and shown as locked.
 */
data class ActivityEntry(
    val packageName: String,
    val className: String,
    val label: String,
    val exported: Boolean
) {
    /** "com.example.app.SettingsActivity" shown as ".SettingsActivity" when it sits in the app's package. */
    val shortName: String
        get() = if (className.startsWith(packageName)) className.removePrefix(packageName) else className
}

interface AppActivities {
    suspend fun apps(): List<AppEntry>
    suspend fun activities(packageName: String): List<ActivityEntry>
    /** False if the app or activity is gone or refused to open. */
    fun launch(activity: ActivityEntry): Boolean
    /** Asks the launcher to pin a shortcut; false when the launcher does not support pinning. */
    fun pinShortcut(activity: ActivityEntry): Boolean
}

/** An app that can open a link. */
data class LinkHandler(val packageName: String, val className: String, val label: String)

interface LinkHandlers {
    /** Apps that open [url], Osyster excluded, browsers after dedicated apps. */
    suspend fun handlersFor(url: String): List<LinkHandler>
    fun open(url: String, handler: LinkHandler): Boolean
    /** Whether Osyster is the default app for web links right now. */
    fun isDefaultForLinks(): Boolean
}

/**
 * The one-time WRITE_SECURE_SETTINGS grant behind Stay awake while charging and the power tools.
 * [adbCommand] already contains this build's package name.
 */
interface SecureSettingsAccess {
    fun isGranted(): Boolean
    val adbCommand: String
}
