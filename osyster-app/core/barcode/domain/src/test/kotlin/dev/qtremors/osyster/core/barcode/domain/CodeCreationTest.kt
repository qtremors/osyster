package dev.qtremors.osyster.core.barcode.domain

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeCreationTest {

    private fun ready(kind: CreateKind, vararg values: Pair<CodeField, String>): String {
        val result = encode(kind, values.toMap(), ZoneOffset.UTC)
        assertTrue("$kind: $result", result is Encoded.Ready)
        return (result as Encoded.Ready).text
    }

    private fun problem(kind: CreateKind, vararg values: Pair<CodeField, String>): CreateProblem =
        (encode(kind, values.toMap(), ZoneOffset.UTC) as Encoded.Invalid).problem

    /** Draws [text] as [format] with a quiet zone and reads it back the way the scanner does. */
    private fun roundTrip(text: String, format: CodeFormat, rounded: Boolean = false): String {
        val matrix = encodeMatrix(text, format) ?: error("$format cannot hold $text")
        val style = CodeStyle.defaultsFor(format).copy(rounded = rounded)
        val layout = CodeLayout.of(matrix, style)
        val scale = 8
        val w = layout.modulesWide * scale
        val h = layout.modulesHigh * scale
        val pixels = IntArray(w * h) { CodeStyle.WHITE }
        for (py in 0 until h) for (px in 0 until w) {
            val mx = px / scale - style.margin
            val my = if (matrix.oneD) 0 else py / scale - style.margin
            val inBars = if (matrix.oneD) py / scale - style.margin in 0 until layout.barHeight else my in 0 until matrix.height
            if (mx in 0 until matrix.width && inBars && matrix[mx, my]) {
                // Dots fill the circle inside their module, as the app draws them.
                val dx = px % scale - scale / 2 + 0.5
                val dy = py % scale - scale / 2 + 0.5
                val inside = !matrix.dotAt(mx, my, style) || dx * dx + dy * dy <= (scale / 2.0) * (scale / 2.0)
                if (inside) pixels[py * w + px] = CodeStyle.BLACK
            }
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(w, h, pixels)))
        // A drawn code is a perfect, straight image: what ZXing calls a pure barcode.
        val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(format.zxing), DecodeHintType.TRY_HARDER to true, DecodeHintType.PURE_BARCODE to true)
        return MultiFormatReader().decode(bitmap, hints).text
    }

    @Test
    fun `qr kinds read back as what they hold`() {
        val wifi = ready(CreateKind.WIFI, CodeField.SSID to "Café; Net", CodeField.PASSWORD to "p:a,ss\\", CodeField.HIDDEN to TRUE)
        val parsedWifi = parseContent(roundTrip(wifi, CodeFormat.QR_CODE), CodeFormat.QR_CODE) as CodeContent.Wifi
        assertEquals("Café; Net", parsedWifi.ssid)
        assertEquals("p:a,ss\\", parsedWifi.password)
        assertTrue(parsedWifi.hidden)

        val contact = ready(
            CreateKind.CONTACT,
            CodeField.NAME to "Jane Q Doe",
            CodeField.PHONE to "+15550001111",
            CodeField.EMAIL to "jane@example.com",
            CodeField.ORGANIZATION to "Acme, Inc."
        )
        val parsedContact = parseContent(roundTrip(contact, CodeFormat.QR_CODE), CodeFormat.QR_CODE) as CodeContent.Contact
        assertEquals("Jane Q Doe", parsedContact.name)
        assertEquals(listOf("+15550001111"), parsedContact.phones)
        assertEquals("Acme, Inc.", parsedContact.organization)

        val mail = parseContent(ready(CreateKind.EMAIL, CodeField.EMAIL to "me@example.com", CodeField.SUBJECT to "Hi there"), CodeFormat.QR_CODE) as CodeContent.Email
        assertEquals("me@example.com", mail.to)
        assertEquals("Hi there", mail.subject)

        val sms = parseContent(ready(CreateKind.SMS, CodeField.PHONE to "5551234", CodeField.MESSAGE to "On my way"), CodeFormat.QR_CODE) as CodeContent.Sms
        assertEquals("5551234", sms.number)
        assertEquals("On my way", sms.body)

        val place = parseContent(ready(CreateKind.LOCATION, CodeField.LATITUDE to "48,8584", CodeField.LONGITUDE to "2.2945", CodeField.QUERY to "Tour Eiffel"), CodeFormat.QR_CODE) as CodeContent.Location
        assertEquals(48.8584, place.latitude, 1e-9)
        assertEquals("Tour Eiffel", place.query)

        assertEquals("https://example.com", ready(CreateKind.LINK, CodeField.URL to "example.com"))
        assertEquals("tel:+15551234", ready(CreateKind.PHONE, CodeField.PHONE to "+1 555 1234"))
        assertEquals("https://play.google.com/store/apps/details?id=dev.qtremors.osyster", ready(CreateKind.APP, CodeField.PACKAGE to "dev.qtremors.osyster"))
        assertEquals("Grüße 👋", roundTrip(ready(CreateKind.TEXT, CodeField.TEXT to "Grüße 👋"), CodeFormat.QR_CODE))
    }

    @Test
    fun `events keep their times`() {
        val start = 1_800_000_000_000L
        val text = ready(CreateKind.EVENT, CodeField.SUMMARY to "Launch", CodeField.START to "$start", CodeField.END to "${start + 3_600_000}", CodeField.PLACE to "Hall 2")
        val event = parseContent(text, CodeFormat.QR_CODE) as CodeContent.Event
        assertEquals("Launch", event.summary)
        assertEquals(start, event.startMillis)
        assertEquals(start + 3_600_000, event.endMillis)
        assertEquals("Hall 2", event.location)
        assertTrue(ready(CreateKind.EVENT, CodeField.SUMMARY to "Day off", CodeField.START to "$start", CodeField.ALL_DAY to TRUE).contains("DTSTART;VALUE=DATE:20270115"))
        assertEquals(CreateProblem.EndBeforeStart(CodeField.END), problem(CreateKind.EVENT, CodeField.SUMMARY to "x", CodeField.START to "$start", CodeField.END to "${start - 1}"))
    }

    @Test
    fun `retail codes add or check the check digit`() {
        assertEquals(1, retailCheckDigit("400638133393"))
        assertEquals("4006381333931", ready(CreateKind.EAN_13, CodeField.DIGITS to "400638133393"))
        assertEquals("4006381333931", ready(CreateKind.EAN_13, CodeField.DIGITS to "4006381333931").also { assertEquals(it, roundTrip(it, CodeFormat.EAN_13)) })
        assertEquals(CreateProblem.Checksum(CodeField.DIGITS), problem(CreateKind.EAN_13, CodeField.DIGITS to "4006381333932"))
        assertEquals(CreateProblem.DigitCount(CodeField.DIGITS, listOf(12, 13)), problem(CreateKind.EAN_13, CodeField.DIGITS to "123"))
        assertEquals("96385074", ready(CreateKind.EAN_8, CodeField.DIGITS to "9638507").also { assertEquals(it, roundTrip(it, CodeFormat.EAN_8)) })
        assertEquals("036000291452", ready(CreateKind.UPC_A, CodeField.DIGITS to "03600029145").also { assertEquals(it, roundTrip(it, CodeFormat.UPC_A)) })
        // UPC-E 0123456 stands for UPC-A 01234500006, whose check digit is 5.
        assertEquals("01234565", ready(CreateKind.UPC_E, CodeField.DIGITS to "0123456").also { assertEquals(it, roundTrip(it, CodeFormat.UPC_E)) })
        assertEquals(CreateProblem.UpcESystem(CodeField.DIGITS), problem(CreateKind.UPC_E, CodeField.DIGITS to "2123456"))
    }

    @Test
    fun `other formats check their characters`() {
        assertEquals(CreateProblem.EvenDigits(CodeField.DIGITS), problem(CreateKind.ITF, CodeField.DIGITS to "123"))
        // Scanners read ITF at 6 to 14 digits by default.
        assertEquals("123456", roundTrip(ready(CreateKind.ITF, CodeField.DIGITS to "123456"), CodeFormat.ITF))
        assertEquals("OSY-0.3.6", roundTrip(ready(CreateKind.CODE_128, CodeField.CONTENT to "OSY-0.3.6"), CodeFormat.CODE_128))
        assertEquals("CODE 39", roundTrip(ready(CreateKind.CODE_39, CodeField.CONTENT to "CODE 39"), CodeFormat.CODE_39))
        assertEquals("CODE-93", roundTrip(ready(CreateKind.CODE_93, CodeField.CONTENT to "CODE-93"), CodeFormat.CODE_93))
        assertTrue(problem(CreateKind.CODE_128, CodeField.CONTENT to "naïve") is CreateProblem.Characters)
        assertEquals("A12-34B", ready(CreateKind.CODABAR, CodeField.CONTENT to "a12-34b"))
        assertTrue(problem(CreateKind.CODABAR, CodeField.CONTENT to "12x") is CreateProblem.Characters)
        for (format in listOf(CodeFormat.DATA_MATRIX, CodeFormat.AZTEC, CodeFormat.PDF_417)) {
            assertEquals(format.name, "Hello, Osyster", roundTrip("Hello, Osyster", format))
        }
        assertEquals(CreateProblem.Missing(CodeField.SSID), problem(CreateKind.WIFI, CodeField.SSID to "  "))
        assertEquals(CreateProblem.PackageName(CodeField.PACKAGE), problem(CreateKind.APP, CodeField.PACKAGE to "not a package"))
        assertTrue(problem(CreateKind.LOCATION, CodeField.LATITUDE to "91", CodeField.LONGITUDE to "0") is CreateProblem.Range)
    }

    @Test
    fun `round dots keep the corner squares solid and still read`() {
        val matrix = encodeMatrix("https://github.com/qtremors/osyster", CodeFormat.QR_CODE)!!
        val style = CodeStyle(rounded = true)
        assertTrue(matrix.inFinder(0, 0) && matrix.inFinder(matrix.width - 1, 0) && matrix.inFinder(0, matrix.height - 1))
        assertTrue(!matrix.inFinder(matrix.width - 1, matrix.height - 1))
        assertTrue(!matrix.dotAt(0, 0, style))
        assertEquals("https://github.com/qtremors/osyster", roundTrip("https://github.com/qtremors/osyster", CodeFormat.QR_CODE, rounded = true))
        // Other 2D codes never get dots.
        val dataMatrix = encodeMatrix("Osyster", CodeFormat.DATA_MATRIX)!!
        assertTrue((0 until dataMatrix.width).none { dataMatrix.dotAt(it, 0, style) })
    }

    @Test
    fun `too much text is refused, not crashed on`() {
        assertNull(encodeMatrix("x".repeat(8_000), CodeFormat.QR_CODE))
        assertNull(encodeMatrix("12", CodeFormat.EAN_13))
    }

    @Test
    fun `svg and contrast`() {
        val matrix = encodeMatrix("4006381333931", CodeFormat.EAN_13)!!
        val svg = toSvg(matrix, CodeStyle.defaultsFor(CodeFormat.EAN_13), "4006381333931")
        assertTrue(svg.startsWith("<svg"))
        assertTrue(svg.contains(">4006381333931</text>"))
        val dots = toSvg(encodeMatrix("hi", CodeFormat.QR_CODE)!!, CodeStyle(rounded = true, background = CodeStyle.TRANSPARENT), "hi")
        assertTrue(dots.contains("<circle"))
        assertTrue(!dots.contains("<rect width=\"100%\""))
        assertEquals(21.0, contrast(CodeStyle.BLACK, CodeStyle.WHITE), 0.01)
        assertNull(warningFor(CodeStyle()))
        assertEquals(StyleWarning.INVERTED, warningFor(CodeStyle(foreground = CodeStyle.WHITE, background = CodeStyle.BLACK)))
        assertEquals(StyleWarning.LOW_CONTRAST, warningFor(CodeStyle(foreground = 0xFFBBBBBB.toInt())))
    }
}
