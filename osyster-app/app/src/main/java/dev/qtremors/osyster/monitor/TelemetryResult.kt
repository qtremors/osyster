package dev.qtremors.osyster.monitor

// =========================================================================
// Section Comment: Sealed Telemetry Result Contract
// =========================================================================

sealed interface TelemetryResult<out T> {
    data class Available<out T>(val value: T) : TelemetryResult<T>
    data object Restricted : TelemetryResult<Nothing>

    val isAvailable: Boolean
        get() = this is Available

    val isRestricted: Boolean
        get() = this is Restricted
}

fun <T> TelemetryResult<T>.getOrNull(): T? = when (this) {
    is TelemetryResult.Available -> value
    is TelemetryResult.Restricted -> null
}

fun <T> TelemetryResult<T>.getOrDefault(default: T): T = when (this) {
    is TelemetryResult.Available -> value
    is TelemetryResult.Restricted -> default
}
