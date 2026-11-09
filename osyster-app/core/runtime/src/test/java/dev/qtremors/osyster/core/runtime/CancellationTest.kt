package dev.qtremors.osyster.core.runtime

import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellationTest {
    @Test
    fun successIsWrapped() {
        assertEquals(4, runCatchingCancellable { 2 + 2 }.getOrNull())
    }

    @Test
    fun failureIsWrapped() {
        val result = runCatchingCancellable { error("broken") }
        assertTrue(result.isFailure)
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsRethrown() {
        runCatchingCancellable { throw CancellationException("left the screen") }
    }
}
