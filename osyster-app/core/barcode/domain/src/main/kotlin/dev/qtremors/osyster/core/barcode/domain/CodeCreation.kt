package dev.qtremors.osyster.core.barcode.domain

import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** How a form field is typed and checked; the screen picks the keyboard and control from it. */
enum class FieldType { TEXT, MULTILINE, URL, PHONE, EMAIL, PASSWORD, DIGITS, DECIMAL, PACKAGE, DATE_TIME, SWITCH, SECURITY }

/** Every field any creator form can have. Values travel as strings, so a form survives rotation. */
enum class CodeField(val type: FieldType) {
    TEXT(FieldType.MULTILINE),
    URL(FieldType.URL),
    SSID(FieldType.TEXT),
    PASSWORD(FieldType.PASSWORD),
    SECURITY(FieldType.SECURITY),
    HIDDEN(FieldType.SWITCH),
    NAME(FieldType.TEXT),
    ORGANIZATION(FieldType.TEXT),
    JOB_TITLE(FieldType.TEXT),
    PHONE(FieldType.PHONE),
    EMAIL(FieldType.EMAIL),
    ADDRESS(FieldType.MULTILINE),
    WEBSITE(FieldType.URL),
    NOTE(FieldType.MULTILINE),
    SUMMARY(FieldType.TEXT),
    START(FieldType.DATE_TIME),
    END(FieldType.DATE_TIME),
    ALL_DAY(FieldType.SWITCH),
    PLACE(FieldType.TEXT),
    SUBJECT(FieldType.TEXT),
    MESSAGE(FieldType.MULTILINE),
    LATITUDE(FieldType.DECIMAL),
    LONGITUDE(FieldType.DECIMAL),
    QUERY(FieldType.TEXT),
    PACKAGE(FieldType.PACKAGE),
    DIGITS(FieldType.DIGITS),
    CONTENT(FieldType.TEXT)
}

/** Wi-Fi security as it is written in a WIFI: code. */
enum class WifiSecurity(val code: String) { WPA("WPA"), WEP("WEP"), NONE("nopass") }

/**
 * What the creator can make: ten kinds of QR code that build structured text, and the other
 * formats, which take their text as typed within each format's own rules.
 */
enum class CreateKind(val format: CodeFormat, val fields: List<CodeField>, val required: Set<CodeField>) {
    TEXT(CodeFormat.QR_CODE, listOf(CodeField.TEXT), setOf(CodeField.TEXT)),
    LINK(CodeFormat.QR_CODE, listOf(CodeField.URL), setOf(CodeField.URL)),
    WIFI(CodeFormat.QR_CODE, listOf(CodeField.SSID, CodeField.SECURITY, CodeField.PASSWORD, CodeField.HIDDEN), setOf(CodeField.SSID)),
    CONTACT(
        CodeFormat.QR_CODE,
        listOf(CodeField.NAME, CodeField.PHONE, CodeField.EMAIL, CodeField.ORGANIZATION, CodeField.JOB_TITLE, CodeField.ADDRESS, CodeField.WEBSITE, CodeField.NOTE),
        setOf(CodeField.NAME)
    ),
    EVENT(
        CodeFormat.QR_CODE,
        listOf(CodeField.SUMMARY, CodeField.ALL_DAY, CodeField.START, CodeField.END, CodeField.PLACE, CodeField.NOTE),
        setOf(CodeField.SUMMARY, CodeField.START)
    ),
    SMS(CodeFormat.QR_CODE, listOf(CodeField.PHONE, CodeField.MESSAGE), setOf(CodeField.PHONE)),
    EMAIL(CodeFormat.QR_CODE, listOf(CodeField.EMAIL, CodeField.SUBJECT, CodeField.MESSAGE), setOf(CodeField.EMAIL)),
    PHONE(CodeFormat.QR_CODE, listOf(CodeField.PHONE), setOf(CodeField.PHONE)),
    LOCATION(CodeFormat.QR_CODE, listOf(CodeField.LATITUDE, CodeField.LONGITUDE, CodeField.QUERY), setOf(CodeField.LATITUDE, CodeField.LONGITUDE)),
    APP(CodeFormat.QR_CODE, listOf(CodeField.PACKAGE), setOf(CodeField.PACKAGE)),
    DATA_MATRIX(CodeFormat.DATA_MATRIX, listOf(CodeField.TEXT), setOf(CodeField.TEXT)),
    AZTEC(CodeFormat.AZTEC, listOf(CodeField.TEXT), setOf(CodeField.TEXT)),
    PDF_417(CodeFormat.PDF_417, listOf(CodeField.TEXT), setOf(CodeField.TEXT)),
    EAN_13(CodeFormat.EAN_13, listOf(CodeField.DIGITS), setOf(CodeField.DIGITS)),
    EAN_8(CodeFormat.EAN_8, listOf(CodeField.DIGITS), setOf(CodeField.DIGITS)),
    UPC_A(CodeFormat.UPC_A, listOf(CodeField.DIGITS), setOf(CodeField.DIGITS)),
    UPC_E(CodeFormat.UPC_E, listOf(CodeField.DIGITS), setOf(CodeField.DIGITS)),
    CODE_128(CodeFormat.CODE_128, listOf(CodeField.CONTENT), setOf(CodeField.CONTENT)),
    CODE_93(CodeFormat.CODE_93, listOf(CodeField.CONTENT), setOf(CodeField.CONTENT)),
    CODE_39(CodeFormat.CODE_39, listOf(CodeField.CONTENT), setOf(CodeField.CONTENT)),
    CODABAR(CodeFormat.CODABAR, listOf(CodeField.CONTENT), setOf(CodeField.CONTENT)),
    ITF(CodeFormat.ITF, listOf(CodeField.DIGITS), setOf(CodeField.DIGITS));

    val qr: Boolean get() = format == CodeFormat.QR_CODE

    companion object {
        fun named(name: String): CreateKind? = entries.firstOrNull { it.name == name }
        val qrKinds: List<CreateKind> get() = entries.filter { it.qr }
        val otherKinds: List<CreateKind> get() = entries.filterNot { it.qr }
    }
}

/** Why a form cannot make a code yet; the screen shows it under [field], or under the preview. */
sealed interface CreateProblem {
    val field: CodeField?

    data class Missing(override val field: CodeField) : CreateProblem
    data class DigitCount(override val field: CodeField, val counts: List<Int>) : CreateProblem
    data class Checksum(override val field: CodeField) : CreateProblem
    data class UpcESystem(override val field: CodeField) : CreateProblem
    data class EvenDigits(override val field: CodeField) : CreateProblem
    data class Characters(override val field: CodeField, val allowed: String) : CreateProblem
    data class Range(override val field: CodeField, val min: Double, val max: Double) : CreateProblem
    data class PackageName(override val field: CodeField) : CreateProblem
    data class EndBeforeStart(override val field: CodeField) : CreateProblem

    /** The writer refused the text, almost always because it is too long for the format. */
    data object TooLong : CreateProblem {
        override val field: CodeField? = null
    }
}

/** The text a code holds, or the first problem stopping it. */
sealed interface Encoded {
    data class Ready(val text: String) : Encoded
    data class Invalid(val problem: CreateProblem) : Encoded
}

/** Builds the text for [kind] from the form [values]. Blank optional fields are left out. */
fun encode(kind: CreateKind, values: Map<CodeField, String>, zone: ZoneId = ZoneId.systemDefault()): Encoded {
    val v = { field: CodeField -> values[field].orEmpty().trim() }
    kind.required.firstOrNull { v(it).isEmpty() }?.let { return Encoded.Invalid(CreateProblem.Missing(it)) }
    return when (kind) {
        CreateKind.TEXT, CreateKind.DATA_MATRIX, CreateKind.AZTEC, CreateKind.PDF_417 -> Encoded.Ready(values[CodeField.TEXT].orEmpty())
        CreateKind.LINK -> Encoded.Ready(withScheme(v(CodeField.URL)))
        CreateKind.WIFI -> Encoded.Ready(wifiText(values))
        CreateKind.CONTACT -> Encoded.Ready(vCard(values))
        CreateKind.EVENT -> eventText(values, zone)
        CreateKind.SMS -> Encoded.Ready(listOf("SMSTO", v(CodeField.PHONE), v(CodeField.MESSAGE)).joinToString(":").removeSuffix(":"))
        CreateKind.EMAIL -> Encoded.Ready(mailto(v(CodeField.EMAIL), v(CodeField.SUBJECT), v(CodeField.MESSAGE)))
        CreateKind.PHONE -> Encoded.Ready("tel:" + v(CodeField.PHONE).filterNot(Char::isWhitespace))
        CreateKind.LOCATION -> geo(v(CodeField.LATITUDE), v(CodeField.LONGITUDE), v(CodeField.QUERY))
        CreateKind.APP -> v(CodeField.PACKAGE).let { pkg ->
            if (PACKAGE.matches(pkg)) Encoded.Ready(PLAY_STORE + pkg) else Encoded.Invalid(CreateProblem.PackageName(CodeField.PACKAGE))
        }
        CreateKind.EAN_13 -> retailDigits(v(CodeField.DIGITS), 13)
        CreateKind.EAN_8 -> retailDigits(v(CodeField.DIGITS), 8)
        CreateKind.UPC_A -> retailDigits(v(CodeField.DIGITS), 12)
        CreateKind.UPC_E -> retailDigits(v(CodeField.DIGITS), 8, upcE = true)
        CreateKind.ITF -> v(CodeField.DIGITS).let { digits ->
            when {
                !digits.all(Char::isDigit) -> Encoded.Invalid(CreateProblem.Characters(CodeField.DIGITS, "0-9"))
                digits.length % 2 != 0 -> Encoded.Invalid(CreateProblem.EvenDigits(CodeField.DIGITS))
                else -> Encoded.Ready(digits)
            }
        }
        CreateKind.CODE_128, CreateKind.CODE_93, CreateKind.CODE_39 -> v(CodeField.CONTENT).let { text ->
            if (text.all { it.code in 32..126 }) Encoded.Ready(text) else Encoded.Invalid(CreateProblem.Characters(CodeField.CONTENT, "A-Z a-z 0-9 !\"#…~"))
        }
        CreateKind.CODABAR -> v(CodeField.CONTENT).uppercase().let { text ->
            val body = text.removeSurrounding(text.take(1), text.takeLast(1)).takeIf { text.length > 1 && text.first() in "ABCD" && text.last() in "ABCD" } ?: text
            if (body.isNotEmpty() && body.all { it in CODABAR_CHARS }) Encoded.Ready(text) else Encoded.Invalid(CreateProblem.Characters(CodeField.CONTENT, "0-9 - $ : / . +"))
        }
    }
}

/** The check digit EAN and UPC codes end with, for the digits before it. */
fun retailCheckDigit(body: String): Int {
    val sum = body.reversed().mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 3 else 1 }.sum()
    return (10 - sum % 10) % 10
}

/**
 * EAN and UPC take their digits without the check digit (it is added) or with it (it must be
 * right). UPC-E's digits are its number system (0 or 1) and six digits; its check digit covers
 * the UPC-A number it stands for.
 */
private fun retailDigits(digits: String, full: Int, upcE: Boolean = false): Encoded {
    if (!digits.all(Char::isDigit)) return Encoded.Invalid(CreateProblem.Characters(CodeField.DIGITS, "0-9"))
    if (upcE && digits.isNotEmpty() && digits.first() !in "01") return Encoded.Invalid(CreateProblem.UpcESystem(CodeField.DIGITS))
    val checked = { body: String -> retailCheckDigit(if (upcE) upcEToA(body) else body) }
    return when (digits.length) {
        full - 1 -> Encoded.Ready(digits + checked(digits))
        full -> if (digits.last() - '0' == checked(digits.dropLast(1))) Encoded.Ready(digits) else Encoded.Invalid(CreateProblem.Checksum(CodeField.DIGITS))
        else -> Encoded.Invalid(CreateProblem.DigitCount(CodeField.DIGITS, listOf(full - 1, full)))
    }
}

/** Expands UPC-E's number system and six digits to the eleven UPC-A digits its check digit covers. */
internal fun upcEToA(body: String): String {
    val system = body[0]
    val d = body.substring(1, 7)
    val middle = when (d[5]) {
        '0', '1', '2' -> d.substring(0, 2) + d[5] + "0000" + d.substring(2, 5)
        '3' -> d.substring(0, 3) + "00000" + d.substring(3, 5)
        '4' -> d.substring(0, 4) + "00000" + d[4]
        else -> d.substring(0, 5) + "0000" + d[5]
    }
    return system + middle
}

private fun withScheme(url: String): String = if (SCHEME.containsMatchIn(url)) url else "https://$url"

/** Backslash before the characters a WIFI: or MECARD-style field treats as structure. */
private fun escapeWifi(text: String): String = text.replace(Regex("""([\\;,:"])"""), """\\$1""")

private fun wifiText(values: Map<CodeField, String>): String {
    val security = values[CodeField.SECURITY]?.let { name -> WifiSecurity.entries.firstOrNull { it.name == name } } ?: WifiSecurity.WPA
    return buildString {
        append("WIFI:T:").append(security.code).append(';')
        append("S:").append(escapeWifi(values[CodeField.SSID].orEmpty().trim())).append(';')
        val password = values[CodeField.PASSWORD].orEmpty()
        if (security != WifiSecurity.NONE && password.isNotEmpty()) append("P:").append(escapeWifi(password)).append(';')
        if (values[CodeField.HIDDEN] == TRUE) append("H:true;")
        append(';')
    }
}

/** vCard 3.0 text escaping: backslash, comma, semicolon, and line breaks. */
private fun escapeVCard(text: String): String =
    text.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\r\n", "\\n").replace("\n", "\\n")

private fun vCard(values: Map<CodeField, String>): String {
    val v = { field: CodeField -> values[field].orEmpty().trim() }
    val name = v(CodeField.NAME)
    val family = name.substringAfterLast(' ', "")
    val given = if (family.isEmpty()) name else name.substringBeforeLast(' ')
    return buildList {
        add("BEGIN:VCARD")
        add("VERSION:3.0")
        add("N:${escapeVCard(family)};${escapeVCard(given)}")
        add("FN:${escapeVCard(name)}")
        v(CodeField.ORGANIZATION).takeIf(String::isNotEmpty)?.let { add("ORG:${escapeVCard(it)}") }
        v(CodeField.JOB_TITLE).takeIf(String::isNotEmpty)?.let { add("TITLE:${escapeVCard(it)}") }
        v(CodeField.PHONE).takeIf(String::isNotEmpty)?.let { add("TEL:${escapeVCard(it)}") }
        v(CodeField.EMAIL).takeIf(String::isNotEmpty)?.let { add("EMAIL:${escapeVCard(it)}") }
        v(CodeField.ADDRESS).takeIf(String::isNotEmpty)?.let { add("ADR:;;${escapeVCard(it)};;;;") }
        v(CodeField.WEBSITE).takeIf(String::isNotEmpty)?.let { add("URL:${escapeVCard(withScheme(it))}") }
        v(CodeField.NOTE).takeIf(String::isNotEmpty)?.let { add("NOTE:${escapeVCard(it)}") }
        add("END:VCARD")
    }.joinToString("\n")
}

private fun eventText(values: Map<CodeField, String>, zone: ZoneId): Encoded {
    val v = { field: CodeField -> values[field].orEmpty().trim() }
    val start = v(CodeField.START).toLongOrNull() ?: return Encoded.Invalid(CreateProblem.Missing(CodeField.START))
    val end = v(CodeField.END).toLongOrNull()
    if (end != null && end < start) return Encoded.Invalid(CreateProblem.EndBeforeStart(CodeField.END))
    val allDay = values[CodeField.ALL_DAY] == TRUE
    // All-day dates are the day the person picked where they are; timed events are written in UTC.
    val stamp = { millis: Long ->
        if (allDay) {
            ";VALUE=DATE:" + DAY.format(Instant.ofEpochMilli(millis).atZone(zone))
        } else {
            ":" + MOMENT.format(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC))
        }
    }
    val text = buildList {
        add("BEGIN:VEVENT")
        add("SUMMARY:${escapeVCard(v(CodeField.SUMMARY))}")
        add("DTSTART" + stamp(start))
        end?.let { add("DTEND" + stamp(it)) }
        v(CodeField.PLACE).takeIf(String::isNotEmpty)?.let { add("LOCATION:${escapeVCard(it)}") }
        v(CodeField.NOTE).takeIf(String::isNotEmpty)?.let { add("DESCRIPTION:${escapeVCard(it)}") }
        add("END:VEVENT")
    }.joinToString("\n")
    return Encoded.Ready(text)
}

private fun mailto(to: String, subject: String, body: String): String {
    val query = listOfNotNull(
        subject.takeIf(String::isNotEmpty)?.let { "subject=" + percent(it) },
        body.takeIf(String::isNotEmpty)?.let { "body=" + percent(it) }
    ).joinToString("&")
    return "mailto:$to" + if (query.isEmpty()) "" else "?$query"
}

private fun percent(text: String): String = URLEncoder.encode(text, Charsets.UTF_8).replace("+", "%20")

private fun geo(latitude: String, longitude: String, query: String): Encoded {
    val lat = latitude.replace(',', '.').toDoubleOrNull()
    if (lat == null || lat !in -90.0..90.0) return Encoded.Invalid(CreateProblem.Range(CodeField.LATITUDE, -90.0, 90.0))
    val lon = longitude.replace(',', '.').toDoubleOrNull()
    if (lon == null || lon !in -180.0..180.0) return Encoded.Invalid(CreateProblem.Range(CodeField.LONGITUDE, -180.0, 180.0))
    val point = "${latitude.replace(',', '.')},${longitude.replace(',', '.')}"
    return Encoded.Ready("geo:$point" + if (query.isEmpty()) "" else "?q=" + percent(query))
}

/** Switch values in a form. */
const val TRUE = "true"

private const val PLAY_STORE = "https://play.google.com/store/apps/details?id="
private const val CODABAR_CHARS = "0123456789-$:/.+"
private val PACKAGE = Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$""")
private val SCHEME = Regex("""^[A-Za-z][A-Za-z0-9+.-]*:""")
private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
private val MOMENT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
