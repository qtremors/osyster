package dev.qtremors.osyster.core.apps.domain

/** How much an app can see or do with what it has been allowed. */
enum class Risk { LOW, MEDIUM, HIGH }

/** The sections permissions are shown in, as Android's own permission manager groups them. */
enum class PermissionGroup { PHONE_SMS_CONTACTS, LOCATION, FILES_MEDIA, NOTIFICATIONS, CALENDAR, SENSORS }

/**
 * What a runtime permission gives access to, by the name people know. Several permissions can
 * share one area (fine and coarse location are both Location).
 */
enum class PermissionArea(val group: PermissionGroup, val risk: Risk) {
    CONTACTS(PermissionGroup.PHONE_SMS_CONTACTS, Risk.MEDIUM),
    SMS(PermissionGroup.PHONE_SMS_CONTACTS, Risk.HIGH),
    CALL_LOG(PermissionGroup.PHONE_SMS_CONTACTS, Risk.HIGH),
    PHONE(PermissionGroup.PHONE_SMS_CONTACTS, Risk.MEDIUM),
    LOCATION(PermissionGroup.LOCATION, Risk.MEDIUM),
    BACKGROUND_LOCATION(PermissionGroup.LOCATION, Risk.HIGH),
    NEARBY_DEVICES(PermissionGroup.LOCATION, Risk.LOW),
    FILES(PermissionGroup.FILES_MEDIA, Risk.MEDIUM),
    PHOTOS_VIDEOS(PermissionGroup.FILES_MEDIA, Risk.MEDIUM),
    MUSIC_AUDIO(PermissionGroup.FILES_MEDIA, Risk.LOW),
    NOTIFICATIONS(PermissionGroup.NOTIFICATIONS, Risk.LOW),
    CALENDAR(PermissionGroup.CALENDAR, Risk.MEDIUM),
    CAMERA(PermissionGroup.SENSORS, Risk.MEDIUM),
    MICROPHONE(PermissionGroup.SENSORS, Risk.MEDIUM),
    BODY_SENSORS(PermissionGroup.SENSORS, Risk.MEDIUM),
    PHYSICAL_ACTIVITY(PermissionGroup.SENSORS, Risk.LOW);

    companion object {
        /** The area of a runtime permission, or null for install-time and unknown ones. */
        fun of(permission: String): PermissionArea? = AREAS[permission]
    }
}

/**
 * Access people grant in Settings rather than in a prompt. These reach furthest, so most of
 * them make an app high risk on their own.
 */
enum class SpecialAccess(val risk: Risk) {
    ACCESSIBILITY(Risk.HIGH),
    NOTIFICATION_LISTENER(Risk.HIGH),
    DEVICE_ADMIN(Risk.HIGH),
    ALL_FILES(Risk.HIGH),
    INSTALL_APPS(Risk.HIGH),
    DRAW_OVER_APPS(Risk.MEDIUM),
    USAGE_ACCESS(Risk.MEDIUM),
    MODIFY_SETTINGS(Risk.MEDIUM),
    EXACT_ALARMS(Risk.LOW),
    UNRESTRICTED_BATTERY(Risk.LOW)
}

/** One permission an app asks for, and whether it has it. [area] is null for install-time ones. */
data class RequestedPermission(val name: String, val area: PermissionArea?, val granted: Boolean)

/** An installed app as the list and the permission explorer see it. */
data class AppSummary(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val system: Boolean,
    val enabled: Boolean,
    val installer: String?,
    val installedAt: Long,
    val updatedAt: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val apkBytes: Long,
    val permissions: List<RequestedPermission>,
    val special: Set<SpecialAccess>,
    val launchable: Boolean
) {
    /** Runtime areas this app has been allowed. */
    val allowedAreas: Set<PermissionArea> get() = permissions.filter { it.granted }.mapNotNull { it.area }.toSet()

    /** Runtime areas this app asks for, allowed or not. */
    val requestedAreas: Set<PermissionArea> get() = permissions.mapNotNull { it.area }.toSet()

    /** The highest risk among what it has been allowed; an app that asks but was refused stays low. */
    val risk: Risk
        get() = (allowedAreas.map { it.risk } + special.map { it.risk }).maxOrNull() ?: Risk.LOW
}

/** Lists of an app's parts, by class name. */
data class AppComponents(
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>
)

/** Space an app takes, from Android's storage stats (needs usage access). */
data class AppStorage(val appBytes: Long, val dataBytes: Long, val cacheBytes: Long) {
    val totalBytes: Long get() = appBytes + dataBytes + cacheBytes
}

/** Everything the app page shows beyond the summary. */
data class AppDetails(
    val summary: AppSummary,
    /** Native code ABIs packaged in the APK; empty for apps without native code. */
    val abis: List<String>,
    val nativeLibraries: List<String>,
    val components: AppComponents,
    /** SHA-256 of the signing certificate, as colon-separated hex. */
    val signerSha256: String?,
    val storage: AppStorage?,
    val apkPaths: List<String>,
    val permissionLabels: Map<String, String>
)

/** How many apps ask for an area, and how many were allowed it. */
data class AreaCount(val area: PermissionArea, val requested: Int, val allowed: Int)

/** Counts for every area, in the order of [PermissionArea], over [apps]. */
fun areaCounts(apps: List<AppSummary>): List<AreaCount> = PermissionArea.entries.map { area ->
    AreaCount(area, requested = apps.count { area in it.requestedAreas }, allowed = apps.count { area in it.allowedAreas })
}

/** How many of [apps] have each special access. */
fun specialCounts(apps: List<AppSummary>): Map<SpecialAccess, Int> =
    SpecialAccess.entries.associateWith { access -> apps.count { access in it.special } }

/** Apps grouped by [AppSummary.risk]. */
fun riskCounts(apps: List<AppSummary>): Map<Risk, Int> = Risk.entries.associateWith { risk -> apps.count { it.risk == risk } }

/** Ways to sort the app list. */
enum class AppSort { NAME, UPDATED, INSTALLED, SIZE }

fun sortApps(apps: List<AppSummary>, sort: AppSort): List<AppSummary> = when (sort) {
    AppSort.NAME -> apps.sortedBy { it.label.lowercase() }
    AppSort.UPDATED -> apps.sortedByDescending { it.updatedAt }
    AppSort.INSTALLED -> apps.sortedByDescending { it.installedAt }
    AppSort.SIZE -> apps.sortedByDescending { it.apkBytes }
}

/** Matches a search against the name and package. */
fun AppSummary.matches(query: String): Boolean =
    query.isBlank() || label.contains(query.trim(), ignoreCase = true) || packageName.contains(query.trim(), ignoreCase = true)

interface InstalledApps {
    /**
     * Every installed app, with its permissions and special access. Kept for a short while;
     * [fresh] reads again, for after an app was installed, updated, or removed.
     */
    suspend fun apps(fresh: Boolean = false): List<AppSummary>

    suspend fun details(packageName: String): AppDetails?
}

/** Android 6.0 and newer runtime permissions, by area. */
private val AREAS: Map<String, PermissionArea> = buildMap {
    fun put(area: PermissionArea, vararg names: String) = names.forEach { put("android.permission.$it", area) }
    put(PermissionArea.CONTACTS, "READ_CONTACTS", "WRITE_CONTACTS", "GET_ACCOUNTS")
    put(PermissionArea.SMS, "READ_SMS", "SEND_SMS", "RECEIVE_SMS", "RECEIVE_MMS", "RECEIVE_WAP_PUSH")
    put(PermissionArea.CALL_LOG, "READ_CALL_LOG", "WRITE_CALL_LOG", "PROCESS_OUTGOING_CALLS")
    put(PermissionArea.PHONE, "READ_PHONE_STATE", "READ_PHONE_NUMBERS", "CALL_PHONE", "ANSWER_PHONE_CALLS", "ADD_VOICEMAIL", "USE_SIP", "ACCEPT_HANDOVER")
    put(PermissionArea.LOCATION, "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION")
    put(PermissionArea.BACKGROUND_LOCATION, "ACCESS_BACKGROUND_LOCATION")
    put(PermissionArea.NEARBY_DEVICES, "BLUETOOTH_SCAN", "BLUETOOTH_CONNECT", "BLUETOOTH_ADVERTISE", "NEARBY_WIFI_DEVICES", "UWB_RANGING")
    put(PermissionArea.FILES, "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE")
    put(PermissionArea.PHOTOS_VIDEOS, "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_MEDIA_VISUAL_USER_SELECTED", "ACCESS_MEDIA_LOCATION")
    put(PermissionArea.MUSIC_AUDIO, "READ_MEDIA_AUDIO")
    put(PermissionArea.NOTIFICATIONS, "POST_NOTIFICATIONS")
    put(PermissionArea.CALENDAR, "READ_CALENDAR", "WRITE_CALENDAR")
    put(PermissionArea.CAMERA, "CAMERA")
    put(PermissionArea.MICROPHONE, "RECORD_AUDIO")
    put(PermissionArea.BODY_SENSORS, "BODY_SENSORS", "BODY_SENSORS_BACKGROUND")
    put(PermissionArea.PHYSICAL_ACTIVITY, "ACTIVITY_RECOGNITION")
}
