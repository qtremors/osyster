package dev.qtremors.osyster.ui.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncWorkTest {
    @Test
    fun producerStopsWithoutSubscribersAndRestartsWhenVisible() = runTest {
        val state = MutableStateFlow(0)
        var reads = 0
        var stops = 0
        val producer = backgroundScope.launchWhileSubscribed(state) {
            try {
                while (true) {
                    reads++
                    delay(1000)
                }
            } finally { stops++ }
        }
        runCurrent()
        assertEquals(0, reads)
        val first = backgroundScope.launch { state.collect {} }
        val second = backgroundScope.launch { state.collect {} }
        runCurrent()
        assertEquals(1, reads)
        first.cancel()
        runCurrent()
        assertEquals(0, stops)
        second.cancel()
        runCurrent()
        assertEquals(1, stops)
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(1, reads)
        backgroundScope.launch { state.collect {} }
        runCurrent()
        assertEquals(2, reads)
        producer.cancel()
    }

    @Test
    fun obsoleteResultCannotOverwriteNewSelectionEvenWhenReadIgnoresCancellation() = runTest {
        val request = LatestRequest(backgroundScope)
        val published = mutableListOf<String>()
        request.submit(load = { withContext(NonCancellable) { delay(1000); "old" } }, publish = published::add)
        runCurrent()
        request.submit(load = { "new" }, publish = published::add)
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(listOf("new"), published)
    }

    @Test
    fun cancellingOnPermissionLossDoesNotPublishPendingResult() = runTest {
        val request = LatestRequest(backgroundScope)
        val published = mutableListOf<Int>()
        request.submit(load = { withContext(NonCancellable) { delay(1000); 42 } }, publish = published::add)
        runCurrent()
        request.cancel()
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(emptyList<Int>(), published)
    }
}
