package dev.qtremors.osyster.core.notifications.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One saved notification, or one message of a chat. The unique index on app, time, and content
 * fingerprint makes reposts of the same content (apps repost on every update, and Android
 * replays the shade when the listener reconnects) land on the row already there.
 */
@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["packageName", "postedAt", "fingerprint"], unique = true),
        Index(value = ["postedAt"]),
        Index(value = ["packageName", "conversation"]),
        Index(value = ["packageName", "key"])
    ]
)
internal data class EntryRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val packageName: String,
    val appLabel: String,
    val postedAt: Long,
    val title: String?,
    val text: String?,
    val subText: String?,
    val sender: String?,
    val conversation: String?,
    val details: String? = null,
    /** Replies and button labels, one per line. */
    val replies: String? = null,
    val actions: String? = null,
    val progress: Int? = null,
    /** The picture's file name in the history's picture folder. */
    val picture: String? = null,
    val channelId: String?,
    val category: String?,
    val fingerprint: String,
    val removedAt: Long? = null,
    val removal: String? = null
)

/** One app in the history, with how many entries it has and when it last posted. */
internal data class AppRow(val packageName: String, val appLabel: String, val count: Int, val lastAt: Long)

@Dao
internal interface EntryDao {
    /** New rows' ids; -1 for each row that was already there. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rows: List<EntryRow>): List<Long>

    @Query("SELECT picture FROM entries WHERE picture IS NOT NULL")
    suspend fun pictures(): List<String>

    /**
     * The newest entries, optionally for one app and matching [pattern] (a LIKE pattern with
     * backslash escapes) in any text.
     */
    @Query(
        """SELECT * FROM entries
        WHERE (:packageName IS NULL OR packageName = :packageName)
          AND (:pattern IS NULL OR title LIKE :pattern ESCAPE '\' OR text LIKE :pattern ESCAPE '\'
               OR sender LIKE :pattern ESCAPE '\' OR appLabel LIKE :pattern ESCAPE '\' OR subText LIKE :pattern ESCAPE '\')
        ORDER BY postedAt DESC, id DESC
        LIMIT :limit"""
    )
    fun newest(packageName: String?, pattern: String?, limit: Int): Flow<List<EntryRow>>

    /** The content of a chat's latest messages, to recognize ones an app posts again with a new time. */
    @Query("SELECT fingerprint FROM entries WHERE packageName = :packageName AND conversation = :conversation ORDER BY postedAt DESC LIMIT :limit")
    suspend fun recentFingerprints(packageName: String, conversation: String, limit: Int): List<String>

    @Query("SELECT * FROM entries WHERE packageName = :packageName AND conversation = :conversation ORDER BY postedAt ASC, id ASC")
    fun conversation(packageName: String, conversation: String): Flow<List<EntryRow>>

    @Query(
        """SELECT packageName, appLabel, COUNT(*) AS count, MAX(postedAt) AS lastAt FROM entries
        GROUP BY packageName ORDER BY lastAt DESC"""
    )
    fun apps(): Flow<List<AppRow>>

    /** Marks a plain notification as gone from the shade. Chat messages are left as they were. */
    @Query(
        """UPDATE entries SET removedAt = :at, removal = :reason
        WHERE packageName = :packageName AND key = :key AND removedAt IS NULL AND conversation IS NULL"""
    )
    suspend fun markRemoved(packageName: String, key: String, at: Long, reason: String)

    @Query("DELETE FROM entries WHERE postedAt < :before")
    suspend fun deleteBefore(before: Long)

    /** Keeps only the newest [keep] entries. */
    @Query("DELETE FROM entries WHERE id NOT IN (SELECT id FROM entries ORDER BY postedAt DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM entries WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("DELETE FROM entries")
    suspend fun clear()
}

@Database(entities = [EntryRow::class], version = 2, exportSchema = false)
internal abstract class NotificationDatabase : RoomDatabase() {
    abstract fun entries(): EntryDao

    companion object {
        const val NAME = "notification_history.db"
    }
}

/**
 * Pictures saved with the history, one JPEG per notification that had one, in the app's own
 * files. Files no entry points to any more are removed when the history is pruned or cleared.
 */
internal class NotificationPictures(context: android.content.Context) {
    private val folder = java.io.File(context.filesDir, FOLDER)

    fun file(name: String) = java.io.File(folder, name)

    /** Writes [source] as [name]; null when the picture could not be written. */
    fun save(name: String, source: dev.qtremors.osyster.core.notifications.domain.PictureSource): String? {
        folder.mkdirs()
        val target = file(name)
        val written = runCatching { target.outputStream().use(source::write) }.getOrDefault(false)
        if (!written) target.delete()
        return name.takeIf { written }
    }

    fun deleteUnused(used: Collection<String>) {
        val keep = used.toHashSet()
        folder.listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    private companion object {
        const val FOLDER = "notification_pictures"
    }
}
