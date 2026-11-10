package dev.qtremors.osyster.core.notifications.domain

import kotlinx.coroutines.flow.Flow

/**
 * A picture a notification carries, written only if the notification is saved, so a
 * notification that is skipped costs no encoding. Returns false when it cannot be written.
 */
fun interface PictureSource {
    fun write(out: java.io.OutputStream): Boolean
}

/** One message inside a chat notification (Android's MessagingStyle). */
data class CapturedMessage(val sender: String?, val text: String, val timestamp: Long)

/**
 * A notification as it was posted, read once from Android and kept as plain text. Messaging
 * notifications carry their [messages]; everything else uses [title], [text], [bigText], and
 * [lines] (inbox-style lists).
 */
data class CapturedNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val postedAt: Long,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val lines: List<String> = emptyList(),
    val conversationTitle: String? = null,
    /** The app's own id for the chat (its conversation shortcut), when it gives one. */
    val conversationId: String? = null,
    val messages: List<CapturedMessage> = emptyList(),
    /** The summary and info lines some apps add under the text. */
    val summaryText: String? = null,
    val infoText: String? = null,
    /** What the person already replied from the notification itself. */
    val replies: List<String> = emptyList(),
    /** The notification's buttons, by label ("Reply", "Mark as read"). */
    val actions: List<String> = emptyList(),
    /** Percent done, for notifications with a progress bar. */
    val progress: Int? = null,
    val picture: PictureSource? = null,
    val channelId: String? = null,
    val category: String? = null,
    val ongoing: Boolean = false,
    val groupSummary: Boolean = false,
    val media: Boolean = false
)

/** How a notification left the shade, when Android says. */
enum class RemovalReason { DISMISSED, OPENED, APP_REMOVED, OTHER }

/**
 * One saved entry: a whole notification, or one message of a chat notification (then [sender]
 * and [conversation] are set). [text] is the fullest text the notification had.
 */
data class HistoryEntry(
    val id: Long,
    val key: String,
    val packageName: String,
    val appLabel: String,
    val postedAt: Long,
    val title: String?,
    val text: String?,
    val subText: String?,
    val sender: String?,
    val conversation: String?,
    /** Summary and info lines, one per line. */
    val details: String?,
    val replies: List<String>,
    val actions: List<String>,
    val progress: Int?,
    /** The saved picture's file, when the notification had one. */
    val picturePath: String?,
    val channelId: String?,
    val category: String?,
    val removedAt: Long?,
    val removal: RemovalReason?
)

/** An app that has entries, for the filter row and the excluded list. */
data class HistoryApp(val packageName: String, val label: String, val count: Int, val lastAt: Long)

/** What the history list shows: [packageName] narrows to one app, [search] matches any text. */
data class HistoryQuery(val packageName: String? = null, val search: String? = null, val limit: Int = PAGE)

const val PAGE = 200

/** How long entries are kept before they are cleared on their own. */
enum class Retention(val days: Int) { WEEK(7), MONTH(30), QUARTER(90), YEAR(365) }

/**
 * History is off until the person turns it on. [keepOngoing] also saves ongoing notifications
 * (downloads, navigation, timers), which most people do not want listed.
 */
data class HistorySettings(
    val enabled: Boolean = false,
    val keepOngoing: Boolean = false,
    val retention: Retention = Retention.MONTH,
    val excluded: Set<String> = emptySet()
)

interface HistorySettingsStore {
    val settings: Flow<HistorySettings>
    suspend fun update(transform: (HistorySettings) -> HistorySettings)
}

/**
 * Called from the notification listener. Both calls return at once; the work happens later on a
 * background thread, in batches, so the listener never waits on storage.
 */
interface NotificationRecorder {
    /** False while history is off, so the listener can skip reading the notification at all. */
    val recording: Boolean

    fun posted(notification: CapturedNotification)

    fun removed(key: String, packageName: String, at: Long, reason: RemovalReason)
}

interface NotificationHistory {
    fun entries(query: HistoryQuery): Flow<List<HistoryEntry>>

    /** Every message saved from one chat, oldest first. */
    fun conversation(packageName: String, conversation: String): Flow<List<HistoryEntry>>

    val apps: Flow<List<HistoryApp>>

    /** Whether Osyster has notification access; without it nothing can be saved. */
    fun hasAccess(): Boolean

    suspend fun delete(id: Long)

    suspend fun deleteApp(packageName: String)

    suspend fun clear()
}
