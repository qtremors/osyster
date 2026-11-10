package dev.qtremors.osyster.core.privilege.domain

/**
 * Android's app standby buckets, from most to least allowed to run in the background.
 * [shellName] is what `am set-standby-bucket` accepts; EXEMPTED and NEVER are read-only.
 */
enum class StandbyBucket(val value: Int, val shellName: String?) {
    EXEMPTED(5, null),
    ACTIVE(10, "active"),
    WORKING_SET(20, "working_set"),
    FREQUENT(30, "frequent"),
    RARE(40, "rare"),
    RESTRICTED(45, "restricted"),
    NEVER(50, null);

    val settable: Boolean get() = shellName != null

    companion object {
        private val byValue = entries.associateBy(StandbyBucket::value)
        fun fromValue(value: Int): StandbyBucket? = byValue[value]
        val Settable: List<StandbyBucket> = entries.filter(StandbyBucket::settable)
    }
}

/** Parses `am get-standby-bucket` with no package: one `package: bucket` per line. */
fun parseStandbyBuckets(output: String): Map<String, StandbyBucket> =
    output.lineSequence()
        .mapNotNull { line ->
            val separator = line.lastIndexOf(':')
            if (separator <= 0) return@mapNotNull null
            val pkg = line.substring(0, separator).trim()
            val bucket = line.substring(separator + 1).trim().toIntOrNull()?.let(StandbyBucket::fromValue)
            if (bucket == null || !PackageNames.isValid(pkg)) null else pkg to bucket
        }
        .toMap()

/**
 * The battery saver policy string, `key=value,key=value`. Unknown keys are kept as they are, so
 * editing one policy never drops what another tool or the manufacturer set.
 */
class BatterySaverConstants private constructor(private val values: LinkedHashMap<String, String>) {
    operator fun get(key: String): String? = values[key]

    fun flag(policy: SaverPolicy): Boolean = values[policy.key]?.let { it == "true" || it == "1" } ?: policy.systemDefault

    fun with(policy: SaverPolicy, on: Boolean): BatterySaverConstants {
        val next = LinkedHashMap(values)
        if (on == policy.systemDefault) next.remove(policy.key) else next[policy.key] = on.toString()
        return BatterySaverConstants(next)
    }

    fun isEmpty(): Boolean = values.isEmpty()

    /** Null when nothing is overridden, so the caller deletes the setting instead of writing "". */
    fun format(): String? = if (values.isEmpty()) null else values.entries.joinToString(",") { "${it.key}=${it.value}" }

    override fun equals(other: Any?): Boolean = other is BatterySaverConstants && other.values == values
    override fun hashCode(): Int = values.hashCode()

    companion object {
        fun parse(raw: String?): BatterySaverConstants {
            val values = LinkedHashMap<String, String>()
            raw.orEmpty().split(',').forEach { pair ->
                val key = pair.substringBefore('=', "").trim()
                val value = pair.substringAfter('=', "").trim()
                if (key.isNotEmpty() && value.isNotEmpty()) values[key] = value
            }
            return BatterySaverConstants(values)
        }
    }
}

/** One switch on the Power saving page. [systemDefault] is AOSP's value when the key is absent. */
enum class SaverPolicy(val key: String, val systemDefault: Boolean, val group: SaverGroup) {
    DISABLE_ANIMATION("disable_animation", false, SaverGroup.DISPLAY),
    DISABLE_AOD("disable_aod", true, SaverGroup.DISPLAY),
    ENABLE_NIGHT_MODE("enable_night_mode", true, SaverGroup.DISPLAY),
    DISABLE_LAUNCH_BOOST("disable_launch_boost", true, SaverGroup.PERFORMANCE),
    ENABLE_QUICK_DOZE("enable_quick_doze", true, SaverGroup.PERFORMANCE),
    FORCE_ALL_APPS_STANDBY("force_all_apps_standby", true, SaverGroup.PERFORMANCE),
    FORCE_BACKGROUND_CHECK("force_background_check", true, SaverGroup.PERFORMANCE),
    ENABLE_DATASAVER("enable_datasaver", false, SaverGroup.NETWORK),
    ENABLE_FIREWALL("enable_firewall", true, SaverGroup.NETWORK),
    DEFER_FULL_BACKUP("defer_full_backup", true, SaverGroup.NETWORK),
    DEFER_KEYVALUE_BACKUP("defer_keyvalue_backup", true, SaverGroup.NETWORK),
    DISABLE_VIBRATION("disable_vibration", true, SaverGroup.SOUND),
    DISABLE_OPTIONAL_SENSORS("disable_optional_sensors", true, SaverGroup.SOUND)
}

enum class SaverGroup { DISPLAY, PERFORMANCE, NETWORK, SOUND }

/**
 * SystemUI's list of hidden status bar icons. Entries Osyster does not know (manufacturer icons)
 * are kept when the list is rewritten.
 */
data class IconBlacklist(val hidden: Set<String>) {
    fun isHidden(icon: StatusIcon): Boolean = icon.slot in hidden

    fun with(icon: StatusIcon, hide: Boolean): IconBlacklist =
        IconBlacklist(if (hide) hidden + icon.slot else hidden - icon.slot)

    /** Null when nothing is hidden, so the setting is deleted and SystemUI uses its defaults. */
    fun format(): String? = if (hidden.isEmpty()) null else hidden.sorted().joinToString(",")

    companion object {
        fun parse(raw: String?): IconBlacklist =
            IconBlacklist(raw.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty).toSet())
    }
}

/** Status bar slots that AOSP SystemUI honors in icon_blacklist. */
enum class StatusIcon(val slot: String) {
    ALARM("alarm_clock"),
    BLUETOOTH("bluetooth"),
    CAST("cast"),
    HOTSPOT("hotspot"),
    VPN("vpn"),
    HEADSET("headset"),
    ROTATE("rotate"),
    DATA_SAVER("data_saver"),
    AIRPLANE("airplane"),
    MOBILE("mobile"),
    WIFI("wifi"),
    ETHERNET("ethernet"),
    ZEN("zen"),
    VOLUME("volume"),
    NFC("nfc"),
    LOCATION("location"),
    CLOCK("clock")
}

/** Private DNS as the Settings app offers it: off, automatic, or a named provider. */
enum class PrivateDnsMode(val value: String) {
    OFF("off"),
    AUTOMATIC("opportunistic"),
    HOSTNAME("hostname");

    companion object {
        /** Android treats a missing value as automatic. */
        fun parse(raw: String?): PrivateDnsMode = entries.firstOrNull { it.value == raw } ?: AUTOMATIC
    }
}

/** The tile's next mode: off, automatic, then the saved provider if there is one, then off. */
fun nextPrivateDnsMode(current: PrivateDnsMode, hasHost: Boolean): PrivateDnsMode = when (current) {
    PrivateDnsMode.OFF -> PrivateDnsMode.AUTOMATIC
    PrivateDnsMode.AUTOMATIC -> if (hasHost) PrivateDnsMode.HOSTNAME else PrivateDnsMode.OFF
    PrivateDnsMode.HOSTNAME -> PrivateDnsMode.OFF
}

/**
 * The widget's switch: any on mode turns off; off turns on to the saved provider when there is
 * one (that is what the user set up), otherwise to automatic.
 */
fun togglePrivateDnsMode(current: PrivateDnsMode, hasHost: Boolean): PrivateDnsMode = when (current) {
    PrivateDnsMode.OFF -> if (hasHost) PrivateDnsMode.HOSTNAME else PrivateDnsMode.AUTOMATIC
    PrivateDnsMode.AUTOMATIC, PrivateDnsMode.HOSTNAME -> PrivateDnsMode.OFF
}

/** Hostnames a Private DNS provider may use: letters, digits, hyphens, and dots. */
fun isValidDnsHost(host: String): Boolean =
    host.length in 4..253 && Regex("""([A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}""").matches(host)

/**
 * Debugging switches as they were before a quiet launch, so Restore puts back exactly what the
 * user had rather than turning everything on.
 */
data class DebugSnapshot(val developerOptions: Boolean, val usbDebugging: Boolean, val wirelessDebugging: Boolean) {
    val anyOn: Boolean get() = developerOptions || usbDebugging || wirelessDebugging

    fun encode(): String = listOf(developerOptions, usbDebugging, wirelessDebugging).joinToString(",") { if (it) "1" else "0" }

    companion object {
        fun decode(raw: String?): DebugSnapshot? {
            val parts = raw?.split(',')?.map { it == "1" } ?: return null
            if (parts.size != 3) return null
            return DebugSnapshot(parts[0], parts[1], parts[2])
        }
    }
}
