package dev.qtremors.osyster.feature.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.qtremors.osyster.core.barcode.domain.CodeFormat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.roundToInt

/** A code read from the camera or a picture. */
data class ReadCode(val text: String, val format: CodeFormat)

/**
 * Reads codes with ZXing, the way Barcode Scanner by Atharok does: camera frames use only their
 * brightness, cropped to the square in the middle of the viewfinder, and every image is tried
 * again inverted so light codes on dark backgrounds read too.
 */
internal object CodeReader {
    private val formats = mapOf(DecodeHintType.POSSIBLE_FORMATS to CodeFormat.entries.map { it.zxing })
    private val pictureHints = formats + (DecodeHintType.TRY_HARDER to true)
    private val pureHints = pictureHints + (DecodeHintType.PURE_BARCODE to true)

    /** Share of the shorter side the camera scans, matching the frame drawn on screen. */
    const val FRAME_SHARE = 0.7f

    private fun read(source: LuminanceSource, hints: Map<DecodeHintType, Any>): ReadCode? {
        val reader = MultiFormatReader().apply { setHints(hints) }
        for (candidate in listOf(source, source.invert())) {
            try {
                val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(candidate)))
                val format = CodeFormat.of(result.barcodeFormat) ?: continue
                if (result.text.isNotEmpty()) return ReadCode(result.text, format)
            } catch (_: ReaderException) {
                // Not this way; try the next.
            } finally {
                reader.reset()
            }
        }
        return null
    }

    /**
     * Reads a picture. A code that fills a photo often reads better smaller, so a miss is tried
     * again at three quarters, half, and a quarter of the size.
     */
    fun read(bitmap: Bitmap): ReadCode? {
        read(luminance(bitmap), pictureHints)?.let { return it }
        for (scale in listOf(0.75f, 0.5f, 0.25f)) {
            val w = (bitmap.width * scale).roundToInt()
            val h = (bitmap.height * scale).roundToInt()
            if (w < MIN_SIDE || h < MIN_SIDE) break
            val smaller = Bitmap.createScaledBitmap(bitmap, w, h, true)
            read(luminance(smaller), pictureHints)?.let { return it }
        }
        // A clean, straight code, such as a saved or screenshotted one, can trip ZXing's finder
        // search on some module patterns; its pure-barcode mode reads those directly.
        return read(luminance(bitmap), pureHints)
    }

    private fun luminance(bitmap: Bitmap): LuminanceSource {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
    }

    /**
     * Reads one camera frame's brightness plane, inside the on-screen frame. The square is cut out
     * and turned to match the screen first: frames arrive sideways in portrait, and ZXing's 1D
     * readers cannot turn a camera frame themselves, so a level barcode would otherwise never read.
     */
    fun read(image: ImageProxy): ReadCode? {
        val plane = image.planes[0]
        val buffer = plane.buffer.duplicate().apply { rewind() }
        val data = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val size = (minOf(image.width, image.height) * FRAME_SHARE).roundToInt()
        val square = cutSquare(data, plane.rowStride, (image.width - size) / 2, (image.height - size) / 2, size, image.imageInfo.rotationDegrees)
        return read(PlanarYUVLuminanceSource(square, size, size, 0, 0, size, size, false), formats)
    }

    /** The [size] square at [left], [top], turned clockwise by [rotation] degrees (0, 90, 180, 270). */
    private fun cutSquare(data: ByteArray, rowStride: Int, left: Int, top: Int, size: Int, rotation: Int): ByteArray {
        val out = ByteArray(size * size)
        for (y in 0 until size) {
            val row = (top + y) * rowStride + left
            for (x in 0 until size) {
                val (nx, ny) = when (rotation) {
                    90 -> (size - 1 - y) to x
                    180 -> (size - 1 - x) to (size - 1 - y)
                    270 -> y to (size - 1 - x)
                    else -> x to y
                }
                out[ny * size + nx] = data[row + x]
            }
        }
        return out
    }

    /** Opens a picture at a size the reader handles quickly, or null when it cannot be read. */
    fun load(context: Context, uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    private const val MAX_SIDE = 2048
    private const val MIN_SIDE = 120
}

/**
 * Hands every camera frame to [CodeReader] until one reads, then stays quiet until [resume], so a
 * code held in view is reported once rather than many times a second.
 */
internal class CodeAnalyzer(private val onRead: (ReadCode) -> Unit) : ImageAnalysis.Analyzer {
    private val paused = AtomicBoolean(false)

    fun resume() = paused.set(false)

    override fun analyze(image: ImageProxy) {
        image.use {
            if (paused.get()) return
            val code = runCatching { CodeReader.read(it) }.getOrNull() ?: return
            if (paused.compareAndSet(false, true)) onRead(code)
        }
    }
}
