package dev.qtremors.osyster.core.barcode.domain

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.CalendarParsedResult
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.GeoParsedResult
import com.google.zxing.client.result.ISBNParsedResult
import com.google.zxing.client.result.ProductParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.WifiParsedResult

/** Barcode formats, by the names Osyster stores and shows; [zxing] is the reader's own name. */
enum class CodeFormat(val zxing: BarcodeFormat, val twoD: Boolean) {
    QR_CODE(BarcodeFormat.QR_CODE, true),
    DATA_MATRIX(BarcodeFormat.DATA_MATRIX, true),
    AZTEC(BarcodeFormat.AZTEC, true),
    PDF_417(BarcodeFormat.PDF_417, true),
    EAN_13(BarcodeFormat.EAN_13, false),
    EAN_8(BarcodeFormat.EAN_8, false),
    UPC_A(BarcodeFormat.UPC_A, false),
    UPC_E(BarcodeFormat.UPC_E, false),
    CODE_128(BarcodeFormat.CODE_128, false),
    CODE_93(BarcodeFormat.CODE_93, false),
    CODE_39(BarcodeFormat.CODE_39, false),
    CODABAR(BarcodeFormat.CODABAR, false),
    ITF(BarcodeFormat.ITF, false);

    companion object {
        fun of(format: BarcodeFormat): CodeFormat? = entries.firstOrNull { it.zxing == format }
        fun named(name: String): CodeFormat? = entries.firstOrNull { it.name == name }
    }
}

/** What a scanned code holds, read the same way Barcode Scanner by Atharok reads it (with ZXing). */
sealed interface CodeContent {
    /** The raw text, for copying and sharing. */
    val raw: String

    data class Link(override val raw: String, val url: String) : CodeContent
    data class Email(override val raw: String, val to: String, val subject: String?, val body: String?) : CodeContent
    data class Phone(override val raw: String, val number: String) : CodeContent
    data class Sms(override val raw: String, val number: String, val body: String?) : CodeContent
    data class Location(override val raw: String, val latitude: Double, val longitude: Double, val query: String?) : CodeContent
    data class Contact(
        override val raw: String,
        val name: String?,
        val organization: String?,
        val title: String?,
        val phones: List<String>,
        val emails: List<String>,
        val address: String?,
        val website: String?,
        val note: String?
    ) : CodeContent
    data class Event(
        override val raw: String,
        val summary: String?,
        val startMillis: Long,
        val endMillis: Long,
        val allDay: Boolean,
        val location: String?,
        val description: String?
    ) : CodeContent

    /** Wi-Fi details are shown and the password can be copied; Osyster does not join networks. */
    data class Wifi(override val raw: String, val ssid: String, val password: String?, val security: String?, val hidden: Boolean) : CodeContent

    /** A product or book number (EAN, UPC, ISBN), for the search sites. */
    data class Product(override val raw: String, val code: String, val isbn: Boolean) : CodeContent
    data class Text(override val raw: String) : CodeContent
}

/** Reads what [text], scanned as [format], holds: a link, a contact, an event, and so on. */
fun parseContent(text: String, format: CodeFormat): CodeContent =
    when (val parsed = ResultParser.parseResult(Result(text, null, null, format.zxing))) {
        is URIParsedResult -> CodeContent.Link(text, parsed.uri)
        is EmailAddressParsedResult -> CodeContent.Email(text, parsed.tos?.firstOrNull().orEmpty(), parsed.subject, parsed.body)
        is TelParsedResult -> CodeContent.Phone(text, parsed.number)
        is SMSParsedResult -> CodeContent.Sms(text, parsed.numbers?.firstOrNull().orEmpty(), parsed.body)
        is GeoParsedResult -> CodeContent.Location(text, parsed.latitude, parsed.longitude, geoQuery(parsed.query))
        is AddressBookParsedResult -> CodeContent.Contact(
            raw = text,
            name = parsed.names?.firstOrNull(),
            organization = parsed.org,
            title = parsed.title,
            phones = parsed.phoneNumbers?.toList().orEmpty(),
            emails = parsed.emails?.toList().orEmpty(),
            address = parsed.addresses?.firstOrNull(),
            website = parsed.urLs?.firstOrNull(),
            note = parsed.note
        )
        is CalendarParsedResult -> CodeContent.Event(
            raw = text,
            summary = parsed.summary,
            startMillis = parsed.startTimestamp,
            endMillis = parsed.endTimestamp.takeIf { it > 0 } ?: parsed.startTimestamp,
            allDay = parsed.isStartAllDay,
            location = parsed.location,
            description = parsed.description
        )
        is WifiParsedResult -> CodeContent.Wifi(text, parsed.ssid, parsed.password?.takeIf { it.isNotEmpty() }, parsed.networkEncryption, parsed.isHidden)
        is ProductParsedResult -> CodeContent.Product(text, parsed.productID, isbn = false)
        is ISBNParsedResult -> CodeContent.Product(text, parsed.isbn, isbn = true)
        else -> CodeContent.Text(text)
    }

/** ZXing hands back everything after a geo: code's "?", such as "q=Tour%20Eiffel"; keep the place. */
internal fun geoQuery(query: String?): String? {
    val raw = query?.split('&')?.firstOrNull { it.startsWith("q=") }?.removePrefix("q=") ?: return query?.takeIf(String::isNotBlank)
    return runCatching { java.net.URLDecoder.decode(raw, Charsets.UTF_8) }.getOrDefault(raw).takeIf(String::isNotBlank)
}
