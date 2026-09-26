package dev.qtremors.osyster.core.privilege.domain

/**
 * A shell command whose arguments are each single-quoted, so a value containing spaces, quotes,
 * or `;` stays one argument. Only the program name and fixed flags are left bare.
 */
class ShellCommand private constructor(val text: String) {
    override fun toString(): String = text
    override fun equals(other: Any?): Boolean = other is ShellCommand && other.text == text
    override fun hashCode(): Int = text.hashCode()

    companion object {
        /** [program] and [flags] must be fixed words from code; [args] may come from anywhere. */
        fun of(program: String, vararg args: String): ShellCommand {
            require(SAFE_WORD.matches(program)) { "Program must be a plain word" }
            return ShellCommand((listOf(program) + args.map(::quote)).joinToString(" "))
        }

        internal fun quote(arg: String): String =
            if (arg.isNotEmpty() && SAFE_WORD.matches(arg)) arg else "'" + arg.replace("'", "'\\''") + "'"

        private val SAFE_WORD = Regex("""[A-Za-z0-9_./:=@%+,-]+""")
    }
}

/** Android package names: two or more dot-separated parts, each starting with a letter. */
object PackageNames {
    private val PATTERN = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+""")

    fun isValid(name: String): Boolean = name.length <= 255 && PATTERN.matches(name)

    fun require(name: String): String {
        require(isValid(name)) { "Not a package name" }
        return name
    }
}

/** The commands the power tools run. Kept in one place so each is easy to review and test. */
object PowerCommands {
    private const val SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS"

    fun grantSecureSettings(selfPackage: String) =
        ShellCommand.of("pm", "grant", PackageNames.require(selfPackage), SECURE_SETTINGS)

    fun forceStop(pkg: String) = ShellCommand.of("am", "force-stop", PackageNames.require(pkg))

    /** Disables the app for the current user. Its data stays; [unfreeze] brings it back. */
    fun freeze(pkg: String) = ShellCommand.of("pm", "disable-user", "--user", "current", PackageNames.require(pkg))

    fun unfreeze(pkg: String) = ShellCommand.of("pm", "enable", "--user", "current", PackageNames.require(pkg))

    /** Lists every package disabled for the current user, one `package:name` per line. */
    val listFrozen = ShellCommand.of("pm", "list", "packages", "-d", "--user", "current")

    /** With no package, prints `package: bucket` for every app. */
    val standbyBuckets = ShellCommand.of("am", "get-standby-bucket")

    fun setStandbyBucket(pkg: String, bucket: StandbyBucket): ShellCommand {
        val name = requireNotNull(bucket.shellName) { "This bucket cannot be set" }
        return ShellCommand.of("am", "set-standby-bucket", PackageNames.require(pkg), name)
    }

    fun setDensity(dpi: Int): ShellCommand {
        require(dpi in MIN_DENSITY..MAX_DENSITY) { "Density out of range" }
        return ShellCommand.of("wm", "density", dpi.toString())
    }

    val resetDensity = ShellCommand.of("wm", "density", "reset")
    val readDensity = ShellCommand.of("wm", "density")

    fun putSetting(table: SettingsTable, key: String, value: String) =
        ShellCommand.of("settings", "put", table.shellName, SettingKeys.require(key), value)

    fun getSetting(table: SettingsTable, key: String) =
        ShellCommand.of("settings", "get", table.shellName, SettingKeys.require(key))

    fun deleteSetting(table: SettingsTable, key: String) =
        ShellCommand.of("settings", "delete", table.shellName, SettingKeys.require(key))

    /** Root only: SystemUI restarts itself within a second or two. */
    val restartSystemUi = ShellCommand.of("pkill", "-f", "com.android.systemui")

    /** Used to confirm root actually works, not just that `su` exists. */
    val whoAmI = ShellCommand.of("id", "-u")

    const val MIN_DENSITY = 120
    const val MAX_DENSITY = 800
}

/** Setting names are lowercase words joined by underscores; nothing else reaches `settings put`. */
object SettingKeys {
    private val PATTERN = Regex("""[a-z][a-z0-9_]*""")

    fun require(key: String): String {
        require(PATTERN.matches(key)) { "Not a setting key" }
        return key
    }
}

/** Parses `pm list packages` output into package names, skipping anything malformed. */
fun parsePackageList(output: String): Set<String> =
    output.lineSequence()
        .map(String::trim)
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:") }
        .filter(PackageNames::isValid)
        .toSet()

/** Parses `wm density`: "Physical density: 420" and, when changed, "Override density: 480". */
fun parseDensity(output: String): DensityInfo? {
    fun find(label: String) = Regex("""$label density:\s*(\d+)""").find(output)?.groupValues?.get(1)?.toIntOrNull()
    val physical = find("Physical") ?: return null
    return DensityInfo(physical = physical, override = find("Override"))
}

data class DensityInfo(val physical: Int, val override: Int?) {
    val current: Int get() = override ?: physical
}
