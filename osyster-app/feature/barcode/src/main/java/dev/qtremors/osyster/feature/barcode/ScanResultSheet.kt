package dev.qtremors.osyster.feature.barcode

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.barcode.domain.CodeAction
import dev.qtremors.osyster.core.barcode.domain.CodeContent
import dev.qtremors.osyster.core.barcode.domain.SearchSite
import dev.qtremors.osyster.core.barcode.domain.actionsFor
import dev.qtremors.osyster.core.barcode.domain.parseContent
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet

/** What a scanned code holds and what can be done with it. */
@Composable
internal fun ScanResultSheet(code: ReadCode, sites: List<SearchSite>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val content = remember(code) { parseContent(code.text, code.format) }
    val actions = remember(content, sites) { actionsFor(content, sites) }

    OsysterBottomSheet(onDismissRequest = onDismiss, title = stringResource(content.kindLabel)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            SelectionContainer {
                Text(content.headline(context), style = MaterialTheme.typography.titleLarge, maxLines = 6)
            }
            content.details(context)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                stringResource(R.string.scan_format_line, stringResource(content.kindLabel), code.format.label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if ((content is CodeContent.Product || content is CodeContent.Text) && sites.isEmpty()) {
            Text(
                stringResource(R.string.scan_add_sites_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        ConnectedGroup(modifier = Modifier.weight(1f, fill = false)) {
            actions.forEach { action ->
                val (icon, label) = action.display()
                NavigationRow(
                    title = label,
                    icon = icon,
                    badgeKey = "scan-${action::class.simpleName}",
                    onClick = { perform(context, action) }
                )
            }
        }
    }
}

internal val CodeContent.kindLabel: Int
    get() = when (this) {
        is CodeContent.Link -> R.string.scan_kind_link
        is CodeContent.Email -> R.string.scan_kind_email
        is CodeContent.Phone -> R.string.scan_kind_phone
        is CodeContent.Sms -> R.string.scan_kind_sms
        is CodeContent.Location -> R.string.scan_kind_location
        is CodeContent.Contact -> R.string.scan_kind_contact
        is CodeContent.Event -> R.string.scan_kind_event
        is CodeContent.Wifi -> R.string.scan_kind_wifi
        is CodeContent.Product -> if (isbn) R.string.scan_kind_book else R.string.scan_kind_product
        is CodeContent.Text -> R.string.scan_kind_text
    }

/** The main line: the link, number, name, or text itself. Never a Wi-Fi password. */
internal fun CodeContent.headline(context: Context): String = when (this) {
    is CodeContent.Link -> url
    is CodeContent.Email -> to.ifBlank { raw }
    is CodeContent.Phone -> number
    is CodeContent.Sms -> number
    is CodeContent.Location -> query ?: context.getString(R.string.scan_location_value, latitude, longitude)
    is CodeContent.Contact -> name ?: organization ?: phones.firstOrNull() ?: raw
    is CodeContent.Event -> summary ?: raw
    is CodeContent.Wifi -> ssid
    is CodeContent.Product -> code
    is CodeContent.Text -> raw
}

/** A second line where it helps: a message, contact details, an event's time and place. */
private fun CodeContent.details(context: Context): String? = when (this) {
    is CodeContent.Email -> listOfNotNull(subject, body).joinToString("\n").ifBlank { null }
    is CodeContent.Sms -> body
    is CodeContent.Contact -> (listOfNotNull(title, organization.takeIf { name != null }) + phones + emails + listOfNotNull(address)).joinToString("\n").ifBlank { null }
    is CodeContent.Event -> listOfNotNull(
        android.text.format.DateUtils.formatDateRange(context, startMillis, endMillis, android.text.format.DateUtils.FORMAT_SHOW_DATE or if (allDay) 0 else android.text.format.DateUtils.FORMAT_SHOW_TIME),
        location,
        description
    ).joinToString("\n")
    is CodeContent.Wifi -> password?.let { context.getString(R.string.scan_wifi_password, it) } ?: context.getString(R.string.scan_wifi_open)
    else -> null
}

/** The format as people write it: QR Code, EAN-13, Code 128. */
internal val dev.qtremors.osyster.core.barcode.domain.CodeFormat.label: String
    get() = name.split('_').joinToString(" ") { part -> if (part.any(Char::isDigit)) part else part.lowercase().replaceFirstChar(Char::uppercase) }
        .replace("Qr Code", "QR Code").replace("Pdf 417", "PDF 417").replace("Ean ", "EAN-").replace("Upc ", "UPC-").replace("Itf", "ITF")

@Composable
private fun CodeAction.display(): Pair<ImageVector, String> = when (this) {
    is CodeAction.OpenLink -> Icons.AutoMirrored.Rounded.OpenInNew to stringResource(R.string.scan_action_open)
    is CodeAction.Call -> Icons.Rounded.Call to stringResource(R.string.scan_action_call)
    is CodeAction.SendSms -> Icons.Rounded.Sms to stringResource(R.string.scan_action_sms)
    is CodeAction.SendEmail -> Icons.Rounded.Email to stringResource(R.string.scan_action_email)
    is CodeAction.OpenMap -> Icons.Rounded.Map to stringResource(R.string.scan_action_map)
    is CodeAction.AddContact -> Icons.Rounded.PersonAdd to stringResource(R.string.scan_action_contact)
    is CodeAction.AddEvent -> Icons.Rounded.Event to stringResource(R.string.scan_action_event)
    is CodeAction.Search -> Icons.Rounded.Search to stringResource(R.string.scan_action_search, site.name)
    is CodeAction.CopyPassword -> Icons.Rounded.Key to stringResource(R.string.scan_action_password)
    is CodeAction.Copy -> Icons.Rounded.ContentCopy to stringResource(R.string.scan_action_copy)
    is CodeAction.Share -> Icons.Rounded.Share to stringResource(R.string.scan_action_share)
}

/** Hands [action] to the app that does it; copying stays in Osyster. */
private fun perform(context: Context, action: CodeAction) {
    val intent: Intent = when (action) {
        is CodeAction.OpenLink -> Intent(Intent.ACTION_VIEW, Uri.parse(withScheme(action.url)))
        is CodeAction.Search -> Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
        is CodeAction.Call -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + action.number))
        is CodeAction.SendSms -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + action.number)).putExtra("sms_body", action.body.orEmpty())
        is CodeAction.SendEmail -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(action.to))
            .putExtra(Intent.EXTRA_SUBJECT, action.subject.orEmpty())
            .putExtra(Intent.EXTRA_TEXT, action.body.orEmpty())
        is CodeAction.OpenMap -> {
            val point = "${action.latitude},${action.longitude}"
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$point?q=" + Uri.encode(action.query?.let { "$point($it)" } ?: point)))
        }
        is CodeAction.AddContact -> contactIntent(action.contact)
        is CodeAction.AddEvent -> Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, action.event.summary.orEmpty())
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, action.event.startMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, action.event.endMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, action.event.allDay)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, action.event.location.orEmpty())
            .putExtra(CalendarContract.Events.DESCRIPTION, action.event.description.orEmpty())
        is CodeAction.Copy -> return copy(context, action.text)
        is CodeAction.CopyPassword -> return copy(context, action.password)
        is CodeAction.Share -> Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, action.text), null)
    }
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.scan_no_app, Toast.LENGTH_SHORT).show()
    }
}

/** Contacts' own add screen, filled in; Osyster never reads or writes contacts itself. */
private fun contactIntent(contact: CodeContent.Contact): Intent {
    return Intent(ContactsContract.Intents.Insert.ACTION).setType(ContactsContract.RawContacts.CONTENT_TYPE).apply {
        contact.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
        contact.organization?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
        contact.title?.let { putExtra(ContactsContract.Intents.Insert.JOB_TITLE, it) }
        contact.address?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
        contact.note?.let { putExtra(ContactsContract.Intents.Insert.NOTES, it) }
        listOf(ContactsContract.Intents.Insert.PHONE, ContactsContract.Intents.Insert.SECONDARY_PHONE, ContactsContract.Intents.Insert.TERTIARY_PHONE)
            .zip(contact.phones).forEach { (key, value) -> putExtra(key, value) }
        listOf(ContactsContract.Intents.Insert.EMAIL, ContactsContract.Intents.Insert.SECONDARY_EMAIL, ContactsContract.Intents.Insert.TERTIARY_EMAIL)
            .zip(contact.emails).forEach { (key, value) -> putExtra(key, value) }
    }
}

private fun copy(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, text))
    Toast.makeText(context, R.string.scan_copied, Toast.LENGTH_SHORT).show()
}

/** Links scanned without http (www.example.com) still open in the browser. */
private fun withScheme(url: String): String = if (url.contains("://")) url else "https://$url"
