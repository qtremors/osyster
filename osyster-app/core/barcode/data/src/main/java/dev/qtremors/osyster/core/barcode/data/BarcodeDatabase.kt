package dev.qtremors.osyster.core.barcode.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One saved scan. The text is unique: scanning it again only moves it to the top. */
@Entity(tableName = "scans", indices = [Index(value = ["text"], unique = true)])
internal data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val format: String,
    val scannedAt: Long,
    val name: String? = null
)

@Entity(tableName = "search_sites")
internal data class SearchSiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val template: String
)

@Dao
internal interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY scannedAt DESC")
    fun all(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE text = :text LIMIT 1")
    suspend fun byText(text: String): ScanEntity?

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Query("UPDATE scans SET scannedAt = :at, format = :format WHERE id = :id")
    suspend fun touch(id: Long, at: Long, format: String)

    @Query("UPDATE scans SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String?)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scans")
    suspend fun clear()
}

@Dao
internal interface SearchSiteDao {
    @Query("SELECT * FROM search_sites ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<SearchSiteEntity>>

    @Insert
    suspend fun insert(site: SearchSiteEntity): Long

    @Update
    suspend fun update(site: SearchSiteEntity)

    @Query("DELETE FROM search_sites WHERE id = :id")
    suspend fun delete(id: Long)
}

/**
 * Scan history and search sites. Android backup leaves databases out (backup_rules.xml), so past
 * scans never leave the phone that way.
 */
@Database(entities = [ScanEntity::class, SearchSiteEntity::class], version = 1, exportSchema = false)
internal abstract class BarcodeDatabase : RoomDatabase() {
    abstract fun scans(): ScanDao
    abstract fun sites(): SearchSiteDao

    companion object {
        const val NAME = "barcode.db"
    }
}
