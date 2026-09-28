package dev.qtremors.osyster.core.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.qtremors.osyster.core.ui.R
import kotlin.math.roundToInt

/**
 * A short list whose rows move by dragging the handle (or long-pressing the row). Rows swap as the
 * dragged one passes the middle of a neighbor; [onMove] reports each swap so the caller keeps the
 * order. Meant for a dozen items, so it uses a plain Column rather than a lazy list.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, handle: @Composable () -> Unit) -> Unit
) {
    val haptics = rememberAppHaptics()
    var dragging by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    val latestItems by rememberUpdatedState(items)
    val latestMove by rememberUpdatedState(onMove)

    fun dragBy(delta: Float) {
        val id = dragging ?: return
        offset += delta
        val index = latestItems.indexOfFirst { key(it) == id }
        if (index < 0) return
        val shift = (offset / rowHeight).roundToInt()
        val target = (index + shift).coerceIn(0, latestItems.lastIndex)
        if (target != index) {
            latestMove(index, target)
            offset -= (target - index) * rowHeight
            haptics.tap()
        }
    }

    Column(modifier = modifier.clip(RoundedCornerShape(24.dp)), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEach { item ->
            key(key(item)) {
                val id = key(item)
                val active = dragging == id
                val start: () -> Unit = {
                    dragging = id
                    offset = 0f
                    haptics.longPress()
                }
                val end: () -> Unit = {
                    dragging = null
                    offset = 0f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { rowHeight = it.height.coerceAtLeast(1) }
                        .zIndex(if (active) 1f else 0f)
                        .graphicsLayer { translationY = if (active) offset else 0f }
                        .then(if (active) Modifier.shadow(8.dp) else Modifier)
                        .pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { start() },
                                onDragEnd = end,
                                onDragCancel = end,
                                onDrag = { change, amount -> change.consume(); dragBy(amount.y) }
                            )
                        }
                ) {
                    row(item) {
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = stringResource(R.string.action_reorder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .pointerInput(id) {
                                    detectVerticalDragGestures(
                                        onDragStart = { start() },
                                        onDragEnd = end,
                                        onDragCancel = end,
                                        onVerticalDrag = { change, amount -> change.consume(); dragBy(amount) }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}
