package dev.qtremors.osyster.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

val LocalTelemetryVisible = compositionLocalOf { true }

@Composable
fun <T> StateFlow<T>.collectAsVisibleState(): State<T> =
    if (LocalTelemetryVisible.current) {
        collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    } else {
        remember(this) { mutableStateOf(value) }
    }
