package dev.qtremors.osyster.core.notifications.domain

/**
 * An entry about to be saved. [fingerprint] identifies its content: with the time, it makes the
 * same notification or message posted again (apps repost on every update) land on the same row.
 */
data class NewEntry(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val postedAt: Long,
    val title: String?,
    val text: String?,
    val subText: String?,
    val sender: String?,
    val conversation: String?,
    val details: String?,
    val replies: List<String>,
    val actions: List<String>,
    val progress: Int?,
    /** Written to a file only once the entry is known to be new. */
    val picture: PictureSource?,
    val channelId: String?,
    val category: String?,
    val fingerprint: String
)

/**
 * Whether [notification] is worth keeping: history is on, it is not from Osyster or an excluded
 * app, it is not a media player or a group summary (its children carry the same content), it is
 * not ongoing unless asked, and it has some text.
 */
fun shouldRecord(notification: CapturedNotification, settings: HistorySettings, selfPackage: String): Boolean = when {
    !settings.enabled -> false
    notification.packageName == selfPackage -> false
    notification.packageName in settings.excluded -> false
    notification.media -> false
    notification.groupSummary -> false
    notification.ongoing && !settings.keepOngoing -> false
    else -> notification.messages.isNotEmpty() || notification.picture != null || notification.lines.isNotEmpty() ||
        listOf(notification.title, notification.text, notification.bigText).any { !it.isNullOrBlank() }
}

/**
 * The entries one posting becomes. A chat notification becomes one entry per message, so its
 * history reads as a conversation and a message the app later takes back is still kept. Anything
 * else becomes one entry with its fullest text: the expanded text, then inbox lines.
 */
fun entriesFor(notification: CapturedNotification): List<NewEntry> {
    val n = notification
    if (n.messages.isNotEmpty()) {
        // The chat's identity must not change between posts: Android rebuilds the title from the
        // latest sender ("Trip: Ana", then "Trip: Bo"), so it is only used to show the chat.
        val conversation = n.conversationTitle?.clean() ?: n.conversationId?.clean() ?: n.key
        val label = n.conversationTitle?.clean() ?: n.title?.clean() ?: conversation
        return n.messages.filter { it.text.isNotBlank() }.map { message ->
            val sender = message.sender?.clean()
            NewEntry(
                key = n.key,
                packageName = n.packageName,
                appLabel = n.appLabel,
                postedAt = message.timestamp.takeIf { it > 0 } ?: n.postedAt,
                title = label,
                text = message.text.trim(),
                subText = n.subText?.clean(),
                sender = sender,
                conversation = conversation,
                details = null,
                replies = emptyList(),
                actions = emptyList(),
                progress = null,
                picture = null,
                channelId = n.channelId,
                category = n.category,
                fingerprint = fingerprint(n.packageName, conversation, sender.orEmpty(), message.text.trim())
            )
        }
    }
    val body = (listOfNotNull((n.bigText ?: n.text)?.clean()) + n.lines.mapNotNull { it.clean() }).distinct()
    val text = body.joinToString("\n").ifEmpty { null }
    val title = n.title?.clean()
    return listOf(
        NewEntry(
            key = n.key,
            packageName = n.packageName,
            appLabel = n.appLabel,
            postedAt = n.postedAt,
            title = title,
            text = text,
            subText = n.subText?.clean(),
            sender = null,
            conversation = null,
            details = listOfNotNull(n.summaryText?.clean(), n.infoText?.clean()).distinct().filter { it != text && it != title }.joinToString("\n").ifEmpty { null },
            replies = n.replies.mapNotNull { it.clean() },
            actions = n.actions.mapNotNull { it.clean() }.distinct(),
            progress = n.progress?.coerceIn(0, 100),
            picture = n.picture,
            channelId = n.channelId,
            category = n.category,
            // Progress is left out, so a download ticking along is one entry, not hundreds.
            fingerprint = fingerprint(n.packageName, n.key, title.orEmpty(), text.orEmpty(), n.replies.joinToString("\n"))
        )
    )
}

/** Entries posted before this time have outlived [retention]. */
fun retentionCutoff(now: Long, retention: Retention): Long = now - retention.days * DAY_MS

/** A short, stable hash of [parts] (64-bit FNV-1a), the same on every run and device. */
fun fingerprint(vararg parts: String): String {
    var hash = -0x340d631b7bdddcdbL
    for (part in parts) {
        for (char in part) {
            hash = hash xor char.code.toLong()
            hash *= 0x100000001b3L
        }
        // A separator, so ("ab", "c") and ("a", "bc") differ.
        hash = hash xor 0x1F
        hash *= 0x100000001b3L
    }
    return java.lang.Long.toHexString(hash)
}

private fun String.clean(): String? = trim().takeIf { it.isNotEmpty() }

const val DAY_MS = 86_400_000L

/** The most entries kept at once, whatever the retention, so storage stays small. */
const val MAX_ENTRIES = 50_000
