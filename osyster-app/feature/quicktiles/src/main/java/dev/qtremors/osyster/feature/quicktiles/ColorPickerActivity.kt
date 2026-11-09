package dev.qtremors.osyster.feature.quicktiles

import androidx.compose.material3.ButtonDefaults
import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.theme.OsysterTheme
import dev.qtremors.osyster.core.ui.theme.ThemeOptions
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Asks for one screen capture, then shows it full screen: drag anywhere to move the magnifier and
 * copy the color under it. The capture file is deleted when this screen closes.
 */
class ColorPickerActivity : ComponentActivity() {
    private var image by mutableStateOf<android.graphics.Bitmap?>(null)

    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode != RESULT_OK || data == null) {
            finish()
            return@registerForActivityResult
        }
        startForegroundService(
            Intent(this, ColorCaptureService::class.java)
                .putExtra(ColorCaptureService.EXTRA_CODE, result.resultCode)
                .putExtra(ColorCaptureService.EXTRA_DATA, data)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_READY, false)) {
            load()
        } else if (savedInstanceState == null) {
            consent.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())
        }
        setContent {
            OsysterTheme(options = ThemeOptions()) {
                image?.let { bitmap -> PickerScreen(bitmap.asImageBitmap(), bitmap, onDone = ::finish) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_READY, false)) load()
    }

    private fun load() {
        image = runCatching { BitmapFactory.decodeFile(ColorCaptureService.captureFile(this).path) }.getOrNull()
        if (image == null) finish()
    }

    override fun onDestroy() {
        if (isFinishing) ColorCaptureService.captureFile(this).delete()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_READY = "ready"
    }
}

@Composable
private fun PickerScreen(image: ImageBitmap, pixels: android.graphics.Bitmap, onDone: () -> Unit) {
    var viewSize by remember { mutableStateOf(IntSize(1, 1)) }
    var point by remember { mutableStateOf(Offset(pixels.width / 2f, pixels.height / 2f)) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val argb = pixels.getPixel(point.x.roundToInt().coerceIn(0, pixels.width - 1), point.y.roundToInt().coerceIn(0, pixels.height - 1))
    val hex = colorHex(argb)

    fun moveTo(position: Offset) {
        val sx = pixels.width.toFloat() / viewSize.width
        val sy = pixels.height.toFloat() / viewSize.height
        point = Offset((position.x * sx).coerceIn(0f, pixels.width - 1f), (position.y * sy).coerceIn(0f, pixels.height - 1f))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            bitmap = image,
            contentDescription = stringResource(R.string.color_capture_description),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .pointerInput(Unit) { detectTapGestures { moveTo(it) } }
                .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); moveTo(change.position) } }
        )
        Loupe(argb, point, pixels, viewSize)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(32.dp).background(Color(argb), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                Text(hex, style = MaterialTheme.typography.titleMedium)
                Button(shapes = ButtonDefaults.shapes(), onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("color", hex))) } }) {
                    Text(stringResource(R.string.color_copy))
                }
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDone) { Text(stringResource(R.string.color_done)) }
            }
        }
    }
}

/** A magnified circle of the pixels around the point, drawn above the finger so it stays visible. */
@Composable
private fun Loupe(argb: Int, point: Offset, pixels: android.graphics.Bitmap, viewSize: IntSize) {
    val viewX = point.x * viewSize.width / pixels.width
    val viewY = point.y * viewSize.height / pixels.height
    val sizePx = 220f
    Box(
        modifier = Modifier
            .offset { IntOffset((viewX - sizePx / 2).roundToInt(), (viewY - sizePx * 1.4f).roundToInt().coerceAtLeast(0)) }
            .size(110.dp)
            .border(3.dp, Color(argb), CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cells = LOUPE_CELLS
            val cell = size.width / cells
            val half = cells / 2
            for (dx in 0 until cells) {
                for (dy in 0 until cells) {
                    val x = (point.x.roundToInt() + dx - half).coerceIn(0, pixels.width - 1)
                    val y = (point.y.roundToInt() + dy - half).coerceIn(0, pixels.height - 1)
                    drawRect(Color(pixels.getPixel(x, y)), topLeft = Offset(dx * cell, dy * cell), size = androidx.compose.ui.geometry.Size(cell, cell))
                }
            }
            drawRect(Color.White, topLeft = Offset(half * cell, half * cell), size = androidx.compose.ui.geometry.Size(cell, cell), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        }
    }
}

private const val LOUPE_CELLS = 11

/** "#RRGGBB", uppercase, without alpha; screen pixels are always opaque. */
internal fun colorHex(argb: Int): String = String.format(java.util.Locale.ROOT, "#%06X", argb and 0xFFFFFF)
