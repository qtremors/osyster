package dev.qtremors.osyster.feature.barcode

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/** Which part of the frame a drag holds. */
private enum class Grip { MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * For pictures where the code did not read on its own: drag the frame (or its corners) around
 * the code and scan just that part. Built in Compose, so it looks like the rest of Osyster.
 */
@Composable
internal fun CropSheet(bitmap: Bitmap, onFound: (ReadCode) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    // The frame, as shares of the picture's width and height.
    var frame by remember { mutableStateOf(Rect(0.15f, 0.15f, 0.85f, 0.85f)) }
    var reading by remember { mutableStateOf(false) }
    var missed by remember { mutableStateOf(false) }
    val grabPx = with(LocalDensity.current) { 36.dp.toPx() }
    val accent = MaterialTheme.colorScheme.primary

    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.scan_crop_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(if (missed) R.string.scan_crop_none else R.string.scan_crop_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio((bitmap.width.toFloat() / bitmap.height).coerceIn(0.55f, 1.8f))
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var grip: Grip? = null
                            detectDragGestures(
                                onDragStart = { at -> grip = gripAt(frame, at, Size(size.width.toFloat(), size.height.toFloat()), grabPx) },
                                onDragEnd = { grip = null },
                                onDrag = { change, delta ->
                                    val held = grip ?: return@detectDragGestures
                                    change.consume()
                                    frame = dragged(frame, held, delta.x / size.width, delta.y / size.height)
                                    missed = false
                                }
                            )
                        }
                ) {
                    val box = Rect(frame.left * size.width, frame.top * size.height, frame.right * size.width, frame.bottom * size.height)
                    val hole = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(box, CornerRadius(16.dp.toPx()))) }
                    clipPath(hole, clipOp = ClipOp.Difference) { drawRect(Color.Black.copy(alpha = 0.55f)) }
                    drawRoundRect(accent, box.topLeft, box.size, CornerRadius(16.dp.toPx()), style = Stroke(3.dp.toPx()))
                    listOf(box.topLeft, box.topRight, box.bottomLeft, box.bottomRight).forEach { drawCircle(accent, 8.dp.toPx(), it) }
                }
            }
            Button(
                shapes = ButtonDefaults.shapes(),
                enabled = !reading,
                onClick = {
                    reading = true
                    scope.launch {
                        val code = withContext(Dispatchers.Default) { CodeReader.read(cropped(bitmap, frame)) }
                        reading = false
                        if (code != null) onFound(code) else missed = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (reading) LoadingIndicator() else Text(stringResource(R.string.scan_crop_scan), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun gripAt(frame: Rect, at: Offset, size: Size, grabPx: Float): Grip? {
    val box = Rect(frame.left * size.width, frame.top * size.height, frame.right * size.width, frame.bottom * size.height)
    fun near(p: Offset) = abs(p.x - at.x) < grabPx && abs(p.y - at.y) < grabPx
    return when {
        near(box.topLeft) -> Grip.TOP_LEFT
        near(box.topRight) -> Grip.TOP_RIGHT
        near(box.bottomLeft) -> Grip.BOTTOM_LEFT
        near(box.bottomRight) -> Grip.BOTTOM_RIGHT
        box.contains(at) -> Grip.MOVE
        else -> null
    }
}

/** The frame after dragging [grip] by [dx], [dy] (shares of the picture), kept inside it and not too small. */
private fun dragged(frame: Rect, grip: Grip, dx: Float, dy: Float): Rect {
    val min = MIN_FRAME
    return when (grip) {
        Grip.MOVE -> {
            val x = dx.coerceIn(-frame.left, 1f - frame.right)
            val y = dy.coerceIn(-frame.top, 1f - frame.bottom)
            frame.translate(x, y)
        }
        Grip.TOP_LEFT -> frame.copy(left = (frame.left + dx).coerceIn(0f, frame.right - min), top = (frame.top + dy).coerceIn(0f, frame.bottom - min))
        Grip.TOP_RIGHT -> frame.copy(right = (frame.right + dx).coerceIn(frame.left + min, 1f), top = (frame.top + dy).coerceIn(0f, frame.bottom - min))
        Grip.BOTTOM_LEFT -> frame.copy(left = (frame.left + dx).coerceIn(0f, frame.right - min), bottom = (frame.bottom + dy).coerceIn(frame.top + min, 1f))
        Grip.BOTTOM_RIGHT -> frame.copy(right = (frame.right + dx).coerceIn(frame.left + min, 1f), bottom = (frame.bottom + dy).coerceIn(frame.top + min, 1f))
    }
}

private fun cropped(bitmap: Bitmap, frame: Rect): Bitmap {
    val left = (frame.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
    val top = (frame.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
    val width = (frame.width * bitmap.width).roundToInt().coerceIn(1, bitmap.width - left)
    val height = (frame.height * bitmap.height).roundToInt().coerceIn(1, bitmap.height - top)
    return Bitmap.createBitmap(bitmap, left, top, width, height)
}

private const val MIN_FRAME = 0.12f
