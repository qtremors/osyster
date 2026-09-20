package dev.qtremors.osyster.core.device.domain

/**
 * The truthful availability contract shared by every collector and the UI.
 *
 * A value that Android does not give us is never shown as a believable zero or a blank. Each
 * failure case says why, so the UI can show "Restricted by Android", "Permission required",
 * "Not supported", or "Temporarily unavailable" instead.
 *
 * [Available.sampledAt] is Unix epoch time in milliseconds. [Available.source] names the Android
 * API or readable system file that supplied the value, shown in the value's help sheet.
 */
sealed interface Availability<out T> {
    data class Available<out T>(
        val value: T,
        val source: String,
        val sampledAt: Long
    ) : Availability<T>

    /** A normal runtime or special-access permission would unlock the value. */
    data class PermissionRequired(val permission: PermissionKind) : Availability<Nothing>

    /** Android blocks the value for regular apps on this version. */
    data class Restricted(val reason: String) : Availability<Nothing>

    /** The device does not have the hardware or API. */
    data class Unsupported(val reason: String) : Availability<Nothing>

    /** The value exists but could not be read right now, or the vendor does not report it. */
    data class Unavailable(val reason: String, val retryable: Boolean) : Availability<Nothing>

    /** Reading failed unexpectedly. [safeDiagnostic] never contains identifiers or paths. */
    data class Failed(val safeDiagnostic: String, val retryable: Boolean) : Availability<Nothing>

    val isAvailable: Boolean get() = this is Available
}

/** The few permissions Osyster may ask for, each explained before it is requested. */
enum class PermissionKind {
    USAGE_ACCESS,
    LOCATION,
    PHONE_STATE,
    BLUETOOTH,
    NOTIFICATIONS,
    NOTIFICATION_ACCESS,
    WRITE_SECURE_SETTINGS
}

/** What the UI should say instead of a value. */
enum class AvailabilityLabel {
    PERMISSION_REQUIRED,
    RESTRICTED_BY_ANDROID,
    NOT_SUPPORTED,
    NOT_REPORTED,
    TEMPORARILY_UNAVAILABLE,
    COLLECTION_FAILED
}

/** The label for a value that is not available. Returns null for [Availability.Available]. */
val Availability<*>.label: AvailabilityLabel?
    get() = when (this) {
        is Availability.Available -> null
        is Availability.PermissionRequired -> AvailabilityLabel.PERMISSION_REQUIRED
        is Availability.Restricted -> AvailabilityLabel.RESTRICTED_BY_ANDROID
        is Availability.Unsupported -> AvailabilityLabel.NOT_SUPPORTED
        is Availability.Unavailable ->
            if (retryable) AvailabilityLabel.TEMPORARILY_UNAVAILABLE else AvailabilityLabel.NOT_REPORTED
        is Availability.Failed ->
            if (retryable) AvailabilityLabel.TEMPORARILY_UNAVAILABLE else AvailabilityLabel.COLLECTION_FAILED
    }

fun <T> Availability<T>.getOrNull(): T? = (this as? Availability.Available)?.value

inline fun <T, R> Availability<T>.map(transform: (T) -> R): Availability<R> = when (this) {
    is Availability.Available -> Availability.Available(transform(value), source, sampledAt)
    is Availability.PermissionRequired -> this
    is Availability.Restricted -> this
    is Availability.Unsupported -> this
    is Availability.Unavailable -> this
    is Availability.Failed -> this
}
