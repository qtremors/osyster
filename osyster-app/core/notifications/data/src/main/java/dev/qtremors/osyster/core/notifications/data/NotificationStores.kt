package dev.qtremors.osyster.core.notifications.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.withTransaction
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.notifications.domain.CapturedNotification
import dev.qtremors.osyster.core.notifications.domain.HistoryApp
import dev.qtremors.osyster.core.notifications.domain.HistoryEntry
import dev.qtremors.osyster.core.notifications.domain.HistoryQuery
import dev.qtremors.osyster.core.notifications.domain.HistorySettings
import dev.qtremors.osyster.core.notifications.domain.HistorySettingsStore
import dev.qtremors.osyster.core.notifications.domain.MAX_ENTRIES
import dev.qtremors.osyster.core.notifications.domain.NewEntry
import dev.qtremors.osyster.core.notifications.domain.NotificationHistory
import dev.qtremors.osyster.core.notifications.domain.NotificationRecorder
import dev.qtremors.osyster.core.notifications.domain.RemovalReason
import dev.qtremors.osyster.core.notifications.domain.Retention
import dev.qtremors.osyster.core.notifications.domain.entriesFor
import dev.qtremors.osyster.core.notifications.domain.retentionCutoff
import dev.qtremors.osyster.core.notifications.domain.shouldRecord
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.historyStore: DataStore<Preferences> by preferencesDataStore("notification_history")

/**
 * Saves notifications in batches. The listener's calls only check a cached setting and drop the
 * notification into a channel; one coroutine wakes for each burst, waits a moment for the rest
 * of it, and writes them all in one transaction. With history off, nothing runs at all.
 */
@Singleton
internal class RoomNotificationRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: NotificationDatabase,
    private val pictures: NotificationPictures,
    settingsStore: HistorySettingsStore,
    dispatchers: AppDispatchers
) : NotificationRecorder {
    private sealed interface Op {
        data class Post(val notification: CapturedNotification) : Op
        data class Remove(val key: String, val packageName: String, val at: Long, val reason: RemovalReason) : Op
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val settings = settingsStore.settings.stateIn(scope, SharingStarted.Eagerly, HistorySettings())
    private val ops = Channel<Op>(Channel.UNLIMITED)

    /** The last content saved for each live notification, so a repost that changed nothing is skipped. */
    private val lastSaved = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > RECENT_KEYS
    }
    private var lastPrune = 0L

    override val recording: Boolean get() = settings.value.enabled

    init {
        scope.launch {
            for (first in ops) {
                delay(BATCH_WINDOW_MS)
                val batch = mutableListOf(first)
                while (true) batch += ops.tryReceive().getOrNull() ?: break
                runCatching { write(batch) }
            }
        }
    }

    override fun posted(notification: CapturedNotification) {
        if (shouldRecord(notification, settings.value, context.packageName)) ops.trySend(Op.Post(notification))
    }

    override fun removed(key: String, packageName: String, at: Long, reason: RemovalReason) {
        if (recording) ops.trySend(Op.Remove(key, packageName, at, reason))
    }

    private suspend fun write(batch: List<Op>) {
        val dao = db.entries()
        db.withTransaction {
            for (op in batch) when (op) {
                is Op.Post -> {
                    val entries = entriesFor(op.notification)
                    val plain = entries.singleOrNull()?.takeIf { it.conversation == null }
                    if (plain != null) {
                        val mapKey = op.notification.packageName + "|" + op.notification.key
                        if (lastSaved[mapKey] == plain.fingerprint) continue
                        lastSaved[mapKey] = plain.fingerprint
                    }
                    insert(newMessagesOnly(entries))
                }
                is Op.Remove -> dao.markRemoved(op.packageName, op.key, op.at, op.reason.name)
            }
        }
        val now = System.currentTimeMillis()
        if (now - lastPrune > PRUNE_EVERY_MS) {
            lastPrune = now
            dao.deleteBefore(retentionCutoff(now, settings.value.retention))
            dao.trimTo(MAX_ENTRIES)
            pictures.deleteUnused(dao.pictures())
        }
    }

    /**
     * Saves [entries]. A picture is written first so the row can point to it, and removed again
     * if the row turned out to be there already.
     */
    private suspend fun insert(entries: List<NewEntry>) {
        val rows = entries.map { entry ->
            val name = entry.picture?.let { pictures.save("${entry.fingerprint}-${entry.postedAt}.jpg", it) }
            entry.toRow(name)
        }
        db.entries().insert(rows).forEachIndexed { index, id ->
            if (id == -1L) rows[index].picture?.let { pictures.file(it).delete() }
        }
    }

    /**
     * A chat notification repeats its latest messages every time. Most apps keep each message's
     * time, so the unique index already skips them; some stamp them anew, so a message whose
     * content matches one of the chat's recent rows is that row. Each row matches once, so the
     * same words sent twice still count twice.
     */
    private suspend fun newMessagesOnly(entries: List<NewEntry>): List<NewEntry> {
        val first = entries.firstOrNull() ?: return entries
        val conversation = first.conversation ?: return entries
        val recent = db.entries().recentFingerprints(first.packageName, conversation, entries.size + RECENT_MESSAGES)
            .groupingBy { it }.eachCount().toMutableMap()
        return entries.filter { entry ->
            val left = recent[entry.fingerprint] ?: 0
            if (left > 0) recent[entry.fingerprint] = left - 1
            left == 0
        }
    }

    private companion object {
        const val RECENT_MESSAGES = 25
        const val BATCH_WINDOW_MS = 1_000L
        const val RECENT_KEYS = 256
        const val PRUNE_EVERY_MS = 6 * 60 * 60 * 1000L
    }
}

@Singleton
internal class RoomNotificationHistory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: NotificationDatabase,
    private val pictures: NotificationPictures
) : NotificationHistory {
    override fun entries(query: HistoryQuery): Flow<List<HistoryEntry>> =
        db.entries().newest(query.packageName, query.search?.trim()?.takeIf(String::isNotEmpty)?.let(::likePattern), query.limit)
            .map { rows -> rows.map { it.toEntry(pictures) } }

    override fun conversation(packageName: String, conversation: String): Flow<List<HistoryEntry>> =
        db.entries().conversation(packageName, conversation).map { rows -> rows.map { it.toEntry(pictures) } }

    override val apps: Flow<List<HistoryApp>> = db.entries().apps().map { rows ->
        rows.map { HistoryApp(it.packageName, it.appLabel, it.count, it.lastAt) }
    }

    override fun hasAccess(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    override suspend fun delete(id: Long) = afterDeleting { db.entries().delete(id) }
    override suspend fun deleteApp(packageName: String) = afterDeleting { db.entries().deleteApp(packageName) }
    override suspend fun clear() = afterDeleting { db.entries().clear() }

    private suspend fun afterDeleting(delete: suspend () -> Unit) {
        delete()
        pictures.deleteUnused(db.entries().pictures())
    }

    /** Matches [text] anywhere, with LIKE's own wildcards in it taken literally. */
    private fun likePattern(text: String): String =
        "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
}

@Singleton
internal class HistorySettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : HistorySettingsStore {
    private val enabled = booleanPreferencesKey("enabled")
    private val keepOngoing = booleanPreferencesKey("keep_ongoing")
    private val retention = stringPreferencesKey("retention")
    private val excluded = stringSetPreferencesKey("excluded")

    override val settings: Flow<HistorySettings> = context.historyStore.data.map(::read)

    override suspend fun update(transform: (HistorySettings) -> HistorySettings) {
        context.historyStore.edit { prefs ->
            val next = transform(read(prefs))
            prefs[enabled] = next.enabled
            prefs[keepOngoing] = next.keepOngoing
            prefs[retention] = next.retention.name
            prefs[excluded] = next.excluded
        }
    }

    private fun read(prefs: Preferences) = HistorySettings(
        enabled = prefs[enabled] ?: false,
        keepOngoing = prefs[keepOngoing] ?: false,
        retention = prefs[retention]?.let { name -> Retention.entries.firstOrNull { it.name == name } } ?: Retention.MONTH,
        excluded = prefs[excluded].orEmpty()
    )
}

private fun NewEntry.toRow(pictureName: String?) = EntryRow(
    key = key,
    packageName = packageName,
    appLabel = appLabel,
    postedAt = postedAt,
    title = title,
    text = text,
    subText = subText,
    sender = sender,
    conversation = conversation,
    details = details,
    replies = replies.joinToString("\n").ifEmpty { null },
    actions = actions.joinToString("\n").ifEmpty { null },
    progress = progress,
    picture = pictureName,
    channelId = channelId,
    category = category,
    fingerprint = fingerprint
)

private fun EntryRow.toEntry(pictures: NotificationPictures) = HistoryEntry(
    id = id,
    key = key,
    packageName = packageName,
    appLabel = appLabel,
    postedAt = postedAt,
    title = title,
    text = text,
    subText = subText,
    sender = sender,
    conversation = conversation,
    details = details,
    replies = replies?.split('\n').orEmpty(),
    actions = actions?.split('\n').orEmpty(),
    progress = progress,
    picturePath = picture?.let { pictures.file(it).path },
    channelId = channelId,
    category = category,
    removedAt = removedAt,
    removal = removal?.let { name -> RemovalReason.entries.firstOrNull { it.name == name } }
)

@Module
@InstallIn(SingletonComponent::class)
internal object NotificationDatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): NotificationDatabase =
        Room.databaseBuilder(context, NotificationDatabase::class.java, NotificationDatabase.NAME)
            // Version 1 never shipped; any test copy of it is rebuilt empty.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
            .build()

    @Provides
    @Singleton
    fun pictures(@ApplicationContext context: Context): NotificationPictures = NotificationPictures(context)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationDataModule {
    @Binds
    abstract fun recorder(impl: RoomNotificationRecorder): NotificationRecorder

    @Binds
    abstract fun history(impl: RoomNotificationHistory): NotificationHistory

    @Binds
    abstract fun settings(impl: HistorySettingsDataStore): HistorySettingsStore
}
