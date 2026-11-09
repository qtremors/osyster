package dev.qtremors.osyster.core.runtime

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like runCatching, but cancellation still cancels: a CancellationException is rethrown instead
 * of being turned into a failure. Use this in coroutine code instead of runCatching.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        Result.failure(error)
    }
