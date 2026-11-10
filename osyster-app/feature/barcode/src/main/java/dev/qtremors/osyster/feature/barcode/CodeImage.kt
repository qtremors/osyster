package dev.qtremors.osyster.feature.barcode

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import dev.qtremors.osyster.core.barcode.domain.CodeLayout
import dev.qtremors.osyster.core.barcode.domain.CodeMatrix
import dev.qtremors.osyster.core.barcode.domain.CodeStyle
import dev.qtremors.osyster.core.barcode.domain.toSvg
import java.io.File
import kotlin.math.max

/** Draws created codes and hands them to Android: saved through the file picker, or shared. */
internal object CodeImage {
    /**
     * Draws [matrix] about [targetPx] wide. Each module gets a whole number of pixels so edges
     * stay sharp; the picture is then that many modules times the module size.
     */
    fun draw(matrix: CodeMatrix, style: CodeStyle, text: String, targetPx: Int): Bitmap {
        val layout = CodeLayout.of(matrix, style)
        val module = max(1, targetPx / layout.modulesWide)
        val bitmap = createBitmap(layout.modulesWide * module, layout.modulesHigh * module)
        val canvas = Canvas(bitmap)
        canvas.drawColor(style.background)
        val paint = Paint().apply { color = style.foreground }
        val m = style.margin * module
        if (matrix.oneD) {
            val bars = layout.barHeight * module.toFloat()
            for (x in 0 until matrix.width) {
                if (matrix[x, 0]) canvas.drawRect(m + x * module.toFloat(), m.toFloat(), m + (x + 1) * module.toFloat(), m + bars, paint)
            }
            if (style.showText) {
                paint.isAntiAlias = true
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.MONOSPACE
                paint.textSize = (layout.textHeight - 2) * module.toFloat()
                canvas.drawText(text, bitmap.width / 2f, m + bars + (layout.textHeight - 1) * module.toFloat(), paint)
            }
        } else {
            paint.isAntiAlias = style.rounded && matrix.qr
            val radius = module / 2f
            for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                if (!matrix[x, y]) continue
                val left = m + x * module.toFloat()
                val top = m + y * module.toFloat()
                if (matrix.dotAt(x, y, style)) {
                    canvas.drawCircle(left + radius, top + radius, radius, paint)
                } else {
                    canvas.drawRect(left, top, left + module, top + module, paint)
                }
            }
        }
        return bitmap
    }

    fun savePng(context: Context, uri: Uri, bitmap: Bitmap): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
    }.getOrDefault(false)

    fun saveSvg(context: Context, uri: Uri, matrix: CodeMatrix, style: CodeStyle, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(toSvg(matrix, style, text).toByteArray()) } != null
    }.getOrDefault(false)

    /**
     * Shares a PNG from the app's cache through its own file provider, so the other app can read
     * just this picture. Older shared pictures are cleared first; nothing builds up.
     */
    fun share(context: Context, bitmap: Bitmap, name: String): Intent? = runCatching {
        val folder = File(context.cacheDir, SHARE_FOLDER).apply { mkdirs() }
        folder.listFiles()?.forEach(File::delete)
        val file = File(folder, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(name, uri)
        Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }.getOrNull()

    /**
     * A file name from what the code holds: letters and digits only, without a link's scheme or
     * "www", and cut at a word so it never ends mid-word.
     */
    fun fileName(prefix: String, text: String): String {
        val plain = text.lineSequence().first().replace(Regex("^[A-Za-z][A-Za-z0-9+.-]*://"), "").removePrefix("www.")
        val words = plain.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')
        val short = if (words.length <= MAX_NAME) words else words.take(MAX_NAME).substringBeforeLast('-').ifEmpty { words.take(MAX_NAME) }
        return if (short.isEmpty()) prefix else "$prefix-$short"
    }

    private const val MAX_NAME = 32

    private const val SHARE_FOLDER = "codes"

    /** Matches the provider in this module's manifest. */
    private const val AUTHORITY_SUFFIX = ".codes"
}
