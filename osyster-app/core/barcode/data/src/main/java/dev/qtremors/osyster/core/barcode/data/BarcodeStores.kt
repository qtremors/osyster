package dev.qtremors.osyster.core.barcode.data

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.barcode.domain.CodeFormat
import dev.qtremors.osyster.core.barcode.domain.ScanHistory
import dev.qtremors.osyster.core.barcode.domain.ScanRecord
import dev.qtremors.osyster.core.barcode.domain.SearchSite
import dev.qtremors.osyster.core.barcode.domain.SearchSiteStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomScanHistory @Inject constructor(private val db: BarcodeDatabase) : ScanHistory {
    override val records: Flow<List<ScanRecord>> = db.scans().all().map { rows ->
        rows.mapNotNull { row ->
            CodeFormat.named(row.format)?.let { ScanRecord(row.id, row.text, it, row.scannedAt, row.name) }
        }
    }

    override suspend fun add(text: String, format: CodeFormat, scannedAt: Long): Long {
        val existing = db.scans().byText(text)
        if (existing != null) {
            db.scans().touch(existing.id, scannedAt, format.name)
            return existing.id
        }
        return db.scans().insert(ScanEntity(text = text, format = format.name, scannedAt = scannedAt))
    }

    override suspend fun rename(id: Long, name: String?) = db.scans().rename(id, name?.trim()?.ifBlank { null })
    override suspend fun delete(id: Long) = db.scans().delete(id)
    override suspend fun clear() = db.scans().clear()
}

@Singleton
internal class RoomSearchSites @Inject constructor(private val db: BarcodeDatabase) : SearchSiteStore {
    override val sites: Flow<List<SearchSite>> = db.sites().all().map { rows -> rows.map { SearchSite(it.id, it.name, it.template) } }

    override suspend fun add(name: String, template: String) {
        db.sites().insert(SearchSiteEntity(name = name.trim(), template = template.trim()))
    }

    override suspend fun update(site: SearchSite) {
        db.sites().update(SearchSiteEntity(site.id, site.name.trim(), site.template.trim()))
    }

    override suspend fun delete(id: Long) = db.sites().delete(id)
}

@Module
@InstallIn(SingletonComponent::class)
internal object BarcodeDatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): BarcodeDatabase =
        Room.databaseBuilder(context, BarcodeDatabase::class.java, BarcodeDatabase.NAME).build()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BarcodeDataModule {
    @Binds
    abstract fun history(impl: RoomScanHistory): ScanHistory

    @Binds
    abstract fun sites(impl: RoomSearchSites): SearchSiteStore
}
