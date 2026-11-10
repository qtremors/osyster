package dev.qtremors.osyster.core.barcode.domain

import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.max
import kotlin.math.pow

/** How much of a QR code can be damaged and still read; more recovery makes a denser code. */
enum class Recovery(val zxing: ErrorCorrectionLevel) { LOW(ErrorCorrectionLevel.L), MEDIUM(ErrorCorrectionLevel.M), QUARTILE(ErrorCorrectionLevel.Q), HIGH(ErrorCorrectionLevel.H) }

/**
 * The look of a created code. Colors are ARGB. [margin] is the quiet zone in modules, the empty
 * border scanners need; [rounded] draws a QR code's modules as dots, all but its corner squares.
 */
data class CodeStyle(
    val foreground: Int = BLACK,
    val background: Int = WHITE,
    val margin: Int = 4,
    val rounded: Boolean = false,
    val showText: Boolean = true,
    val recovery: Recovery = Recovery.MEDIUM
) {
    companion object {
        const val BLACK: Int = 0xFF000000.toInt()
        const val WHITE: Int = 0xFFFFFFFF.toInt()
        const val TRANSPARENT: Int = 0

        /** 1D barcodes need a wider quiet zone than 2D codes. */
        fun defaultsFor(format: CodeFormat): CodeStyle = CodeStyle(margin = if (format.twoD) 4 else 10)
    }
}

/**
 * A code's modules with no border: one cell per module for 2D codes, one row for 1D barcodes
 * (they are drawn taller). PDF 417 rows come already stretched to its usual 1:4 shape.
 */
class CodeMatrix(val width: Int, val height: Int, private val bits: BooleanArray, val oneD: Boolean, val qr: Boolean = false) {
    operator fun get(x: Int, y: Int): Boolean = bits[y * width + x]

    /** Inside one of a QR code's three corner squares, which stay solid so scanners find them. */
    fun inFinder(x: Int, y: Int): Boolean =
        qr && ((x < FINDER && y < FINDER) || (x >= width - FINDER && y < FINDER) || (x < FINDER && y >= height - FINDER))

    /** Whether module ([x], [y]) is drawn as a dot under [style]. */
    fun dotAt(x: Int, y: Int, style: CodeStyle): Boolean = style.rounded && qr && !inFinder(x, y)

    companion object {
        private const val FINDER = 7

        fun of(matrix: BitMatrix, oneD: Boolean, qr: Boolean = false): CodeMatrix {
            val rows = if (oneD) 1 else matrix.height
            val bits = BooleanArray(matrix.width * rows) { matrix[it % matrix.width, it / matrix.width] }
            return CodeMatrix(matrix.width, rows, bits, oneD, qr)
        }
    }
}

/** Writes [text] as [format]; null when the format cannot hold it (too long, bad characters). */
fun encodeMatrix(text: String, format: CodeFormat, recovery: Recovery = Recovery.MEDIUM): CodeMatrix? {
    val hints = buildMap<EncodeHintType, Any> {
        put(EncodeHintType.MARGIN, 0)
        if (format == CodeFormat.QR_CODE) put(EncodeHintType.ERROR_CORRECTION, recovery.zxing)
        // Plain Latin text stays in the default encoding, which every scanner reads; anything else
        // is marked UTF-8 so accents, scripts, and emoji survive.
        if (format.twoD && !text.all { it.code < 256 }) put(EncodeHintType.CHARACTER_SET, "UTF-8")
    }
    return try {
        CodeMatrix.of(MultiFormatWriter().encode(text, format.zxing, 0, 0, hints), oneD = !format.twoD, qr = format == CodeFormat.QR_CODE)
    } catch (_: WriterException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * Where everything sits in a drawing, in modules: [barHeight] is a 1D barcode's bar height and
 * [textHeight] the room under it for the digits.
 */
data class CodeLayout(val modulesWide: Int, val modulesHigh: Int, val barHeight: Int, val textHeight: Int) {
    companion object {
        fun of(matrix: CodeMatrix, style: CodeStyle): CodeLayout {
            val wide = matrix.width + style.margin * 2
            if (!matrix.oneD) return CodeLayout(wide, matrix.height + style.margin * 2, matrix.height, 0)
            val bars = max(matrix.width * BAR_SHARE / 100, MIN_BAR)
            val text = if (style.showText) TEXT_MODULES else 0
            return CodeLayout(wide, bars + text + style.margin * 2, bars, text)
        }
    }
}

/** An SVG of the code at [modulePx] pixels a module, for printing at any size. */
fun toSvg(matrix: CodeMatrix, style: CodeStyle, text: String, modulePx: Int = 10): String {
    val layout = CodeLayout.of(matrix, style)
    val m = style.margin
    return buildString {
        append("""<svg xmlns="http://www.w3.org/2000/svg" version="1.1" """)
        append("""viewBox="0 0 ${layout.modulesWide} ${layout.modulesHigh}" width="${layout.modulesWide * modulePx}" height="${layout.modulesHigh * modulePx}" shape-rendering="${if (style.rounded && matrix.qr) "geometricPrecision" else "crispEdges"}">""")
        append('\n')
        if (alpha(style.background) > 0) {
            append("""<rect width="100%" height="100%" fill="${hex(style.background)}"${opacity(style.background)}/>""").append('\n')
        }
        append("""<g fill="${hex(style.foreground)}"${opacity(style.foreground)}>""").append('\n')
        if (matrix.oneD) {
            // One rectangle per run of dark modules keeps the file small.
            var x = 0
            while (x < matrix.width) {
                if (!matrix[x, 0]) { x++; continue }
                val start = x
                while (x < matrix.width && matrix[x, 0]) x++
                append("""<rect x="${start + m}" y="$m" width="${x - start}" height="${layout.barHeight}"/>""").append('\n')
            }
        } else {
            for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                if (!matrix[x, y]) continue
                if (matrix.dotAt(x, y, style)) {
                    append("""<circle cx="${x + m}.5" cy="${y + m}.5" r="0.5"/>""")
                } else {
                    append("""<rect x="${x + m}" y="${y + m}" width="1" height="1"/>""")
                }
            }
            append('\n')
        }
        append("</g>\n")
        if (matrix.oneD && style.showText) {
            val baseline = m + layout.barHeight + layout.textHeight - 1
            append("""<text x="${layout.modulesWide / 2.0}" y="$baseline" text-anchor="middle" font-family="sans-serif" font-size="${layout.textHeight - 2}" fill="${hex(style.foreground)}"${opacity(style.foreground)}>${xml(text)}</text>""")
            append('\n')
        }
        append("</svg>\n")
    }
}

/** WCAG contrast ratio between two opaque colors, 1 (none) to 21 (black on white). */
fun contrast(a: Int, b: Int): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** What could stop scanners reading a style, worst first; null when it should read anywhere. */
enum class StyleWarning { LOW_CONTRAST, INVERTED }

fun warningFor(style: CodeStyle): StyleWarning? {
    // A see-through background is judged on white, where it is usually printed or shown.
    val background = if (alpha(style.background) < 128) CodeStyle.WHITE else style.background
    return when {
        contrast(style.foreground, background) < MIN_CONTRAST -> StyleWarning.LOW_CONTRAST
        luminance(style.foreground) > luminance(background) -> StyleWarning.INVERTED
        else -> null
    }
}

fun luminance(color: Int): Double {
    val channel = { shift: Int ->
        val c = (color shr shift and 0xFF) / 255.0
        if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

private fun alpha(color: Int): Int = color ushr 24

private fun hex(color: Int): String = "#%06X".format(color and 0xFFFFFF)

private fun opacity(color: Int): String = alpha(color).let { if (it == 255) "" else """ fill-opacity="${"%.3f".format(java.util.Locale.ROOT, it / 255.0)}"""" }

private fun xml(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

/** Bars are this share of the barcode's width tall, but never shorter than [MIN_BAR] modules. */
private const val BAR_SHARE = 35
private const val MIN_BAR = 24
private const val TEXT_MODULES = 12
private const val MIN_CONTRAST = 3.0
