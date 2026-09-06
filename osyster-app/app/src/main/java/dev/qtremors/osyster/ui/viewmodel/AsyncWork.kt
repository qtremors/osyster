package dev.qtremors.osyster.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Producers stop immediately when their last visible UI consumer leaves. */
internal fun CoroutineScope.launchWhileSubscribed(
    state: MutableStateFlow<*>,
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    state.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { active ->
        if (active) coroutineScope(block)
    }
}

/** Confined to the owning ViewModel's main thread. */
internal class LatestRequest(private val scope: CoroutineScope) {
    private var job: Job? = null
    private var generation = 0L

    fun cancel() {
        generation++
        job?.cancel()
        job = null
    }

    fun <T> submit(load: suspend () -> T, publish: (T) -> Unit) {
        cancel()
        val request = generation
        job = scope.launch {
            val result = load()
            if (isActive && request == generation) publish(result)
        }
    }
}
