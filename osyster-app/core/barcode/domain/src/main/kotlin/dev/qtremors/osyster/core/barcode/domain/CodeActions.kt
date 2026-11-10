package dev.qtremors.osyster.core.barcode.domain

import java.net.URLEncoder
import kotlinx.coroutines.flow.Flow

/**
 * What can be done with a scanned code. Every action hands off to another app (browser, dialer,
 * Contacts, Calendar), so Osyster needs no internet, contacts, or calendar permission.
 */
sealed interface CodeAction {
    data class OpenLink(val url: String) : CodeAction
    data class Call(val number: String) : CodeAction
    data class SendSms(val number: String, val body: String?) : CodeAction
    data class SendEmail(val to: String, val subject: String?, val body: String?) : CodeAction
    data class OpenMap(val latitude: Double, val longitude: Double, val query: String?) : CodeAction
    data class AddContact(val contact: CodeContent.Contact) : CodeAction
    data class AddEvent(val event: CodeContent.Event) : CodeAction
    data class Search(val site: SearchSite, val url: String) : CodeAction
    data class CopyPassword(val password: String) : CodeAction
    data class Copy(val text: String) : CodeAction
    data class Share(val text: String) : CodeAction
}

/** The actions for [content], most useful first; Copy and Share always come last. */
fun actionsFor(content: CodeContent, sites: List<SearchSite>): List<CodeAction> {
    val main: List<CodeAction> = when (content) {
        is CodeContent.Link -> listOf(CodeAction.OpenLink(content.url))
        is CodeContent.Email -> listOf(CodeAction.SendEmail(content.to, content.subject, content.body))
        is CodeContent.Phone -> listOf(CodeAction.Call(content.number))
        is CodeContent.Sms -> listOf(CodeAction.SendSms(content.number, content.body))
        is CodeContent.Location -> listOf(CodeAction.OpenMap(content.latitude, content.longitude, content.query))
        is CodeContent.Contact -> listOfNotNull(
            CodeAction.AddContact(content),
            content.phones.firstOrNull()?.let { CodeAction.Call(it) },
            content.emails.firstOrNull()?.let { CodeAction.SendEmail(it, null, null) },
            content.website?.let { CodeAction.OpenLink(it) }
        )
        is CodeContent.Event -> listOf(CodeAction.AddEvent(content))
        is CodeContent.Wifi -> listOfNotNull(content.password?.let { CodeAction.CopyPassword(it) })
        is CodeContent.Product -> sites.map { CodeAction.Search(it, it.urlFor(content.code)) }
        is CodeContent.Text -> sites.map { CodeAction.Search(it, it.urlFor(content.raw)) }
    }
    return main + CodeAction.Copy(content.raw) + CodeAction.Share(content.raw)
}

/**
 * A site to look codes up on, added by the user: [template] is a web address with %s where the
 * code goes, such as https://www.example.com/search?q=%s. Osyster opens it in the browser.
 */
data class SearchSite(val id: Long, val name: String, val template: String) {
    fun urlFor(query: String): String = template.replace(PLACEHOLDER, URLEncoder.encode(query, "UTF-8"))

    companion object {
        const val PLACEHOLDER = "%s"

        /** A web address (http or https) with exactly one %s, and no spaces. */
        fun isValidTemplate(template: String): Boolean {
            val t = template.trim()
            return (t.startsWith("https://") || t.startsWith("http://")) &&
                t.split(PLACEHOLDER).size == 2 && t.none { it.isWhitespace() } && t.length > "https://%s".length
        }
    }
}

/** One scan in the history. [name] is an optional label the user gave it. */
data class ScanRecord(val id: Long, val text: String, val format: CodeFormat, val scannedAt: Long, val name: String?)

/** Past scans, kept on the phone only, newest first. */
interface ScanHistory {
    val records: Flow<List<ScanRecord>>

    /** Saves a scan; the same text scanned again moves to the top instead of repeating. */
    suspend fun add(text: String, format: CodeFormat, scannedAt: Long): Long
    suspend fun rename(id: Long, name: String?)
    suspend fun delete(id: Long)
    suspend fun clear()
}

/** The user's search sites. */
interface SearchSiteStore {
    val sites: Flow<List<SearchSite>>
    suspend fun add(name: String, template: String)
    suspend fun update(site: SearchSite)
    suspend fun delete(id: Long)
}

data class ScannerSettings(
    /** A short beep when a code is read. */
    val beep: Boolean = true,
    /** A short vibration when a code is read; follows Osyster's own haptics setting too. */
    val vibrate: Boolean = true
)

interface ScannerSettingsStore {
    val settings: Flow<ScannerSettings>
    suspend fun update(transform: (ScannerSettings) -> ScannerSettings)
}
