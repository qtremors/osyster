package dev.qtremors.osyster.core.battery.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.qtremors.osyster.core.battery.domain.ACTION_BATTERY_HISTORY_UPDATED
import dev.qtremors.osyster.core.battery.domain.BatteryFacts
import dev.qtremors.osyster.core.battery.domain.BatteryHealth
import dev.qtremors.osyster.core.battery.domain.BatteryHistory
import dev.qtremors.osyster.core.battery.domain.BatteryHistorySettings
import dev.qtremors.osyster.core.battery.domain.BatteryHistorySettingsStore
import dev.qtremors.osyster.core.battery.domain.BatterySample
import dev.qtremors.osyster.core.battery.domain.BatterySampling
import dev.qtremors.osyster.core.battery.domain.KEEP_MS
import dev.qtremors.osyster.core.battery.domain.PlugType
import dev.qtremors.osyster.core.battery.domain.SAMPLE_EVERY_MS
import dev.qtremors.osyster.core.battery.domain.normalizeCurrent
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Entity(tableName = "samples")
internal data class SampleRow(
    @PrimaryKey val at: Long,
    val level: Int,
    val charging: Boolean,
    val plugged: String,
    val temperatureC: Float?,
    val voltageMv: Int?,
    val currentMa: Int?
)

@Dao
internal interface SampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: SampleRow)

    @Query("SELECT * FROM samples WHERE at >= :since ORDER BY at ASC")
    fun since(since: Long): Flow<List<SampleRow>>

    @Query("DELETE FROM samples WHERE at < :before")
    suspend fun deleteBefore(before: Long)

    @Query("DELETE FROM samples")
    suspend fun clear()
}

@Database(entities = [SampleRow::class], version = 1, exportSchema = false)
internal abstract class BatteryDatabase : RoomDatabase() {
    abstract fun samples(): SampleDao

    companion object {
        const val NAME = "battery_history.db"
    }
}

/**
 * Battery readings. Each one comes from Android's last battery broadcast, which it keeps for
 * anyone to read, plus the current from BatteryManager: no sensors are woken and nothing runs in
 * between readings.
 */
@Singleton
internal class RoomBatteryHistory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: BatteryDatabase,
    private val dispatchers: AppDispatchers
) : BatteryHistory {
    override fun samples(since: Long): Flow<List<BatterySample>> = db.samples().since(since).map { rows ->
        rows.map { row ->
            BatterySample(
                at = row.at,
                level = row.level,
                charging = row.charging,
                plugged = PlugType.entries.firstOrNull { it.name == row.plugged } ?: PlugType.NONE,
                temperatureC = row.temperatureC,
                voltageMv = row.voltageMv,
                currentMa = row.currentMa
            )
        }
    }

    override suspend fun recordNow(): BatterySample? = withContext(dispatchers.io) {
        val sample = read() ?: return@withContext null
        db.samples().insert(
            SampleRow(sample.at, sample.level, sample.charging, sample.plugged.name, sample.temperatureC, sample.voltageMv, sample.currentMa)
        )
        db.samples().deleteBefore(sample.at - KEEP_MS)
        runCatching { context.sendBroadcast(Intent(ACTION_BATTERY_HISTORY_UPDATED).setPackage(context.packageName)) }
        sample
    }

    override fun facts(): BatteryFacts {
        val intent = sticky()
        val cycles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it >= 0 }
        } else {
            null
        }
        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.FAILURE
            else -> BatteryHealth.UNKNOWN
        }
        return BatteryFacts(cycles, health)
    }

    override suspend fun clear() = withContext(dispatchers.io) { db.samples().clear() }

    private fun sticky(): Intent? = runCatching { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()

    private fun read(): BatterySample? {
        val intent = sticky() ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        if (level < 0) return null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val pluggedCode = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val charging = pluggedCode != 0 && (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
        val plugged = when (pluggedCode) {
            BatteryManager.BATTERY_PLUGGED_AC -> PlugType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PlugType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
            0 -> PlugType.NONE
            else -> PlugType.DOCK
        }
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val current = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        return BatterySample(
            at = System.currentTimeMillis(),
            level = level * 100 / scale,
            charging = charging,
            plugged = plugged,
            temperatureC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }?.div(10f),
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it > 0 },
            // A full battery only trickles, in units too small to tell microamps from milliamps.
            currentMa = normalizeCurrent(current, charging).takeIf { status != BatteryManager.BATTERY_STATUS_FULL }
        )
    }
}

/**
 * The periodic reading, with Android's own job scheduler: every half hour, batched by Android
 * with other work and paused in Doze, so it never wakes the phone on its own. It is scheduled
 * again when the app starts, so a restart of the phone does not stop it for long.
 */
@Singleton
internal class JobBatterySampling @Inject constructor(@param:ApplicationContext private val context: Context) : BatterySampling {
    override fun schedule(enabled: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!enabled) {
            scheduler.cancel(JOB_ID)
            return
        }
        // Scheduling again would restart the period, so a job already waiting is left alone.
        if (scheduler.getPendingJob(JOB_ID) != null) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, BatterySampleJob::class.java))
            .setPeriodic(SAMPLE_EVERY_MS, FLEX_MS)
            .build()
        runCatching { scheduler.schedule(job) }
    }

    companion object {
        const val JOB_ID = 4101
        private const val FLEX_MS = 10 * 60_000L
    }
}

@AndroidEntryPoint
class BatterySampleJob : JobService() {
    @Inject lateinit var history: BatteryHistory

    private val scope = CoroutineScope(SupervisorJob())

    override fun onStartJob(params: JobParameters?): Boolean {
        scope.launch {
            runCatching { history.recordNow() }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = false

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private val Context.batteryStore: DataStore<Preferences> by preferencesDataStore("battery_history")

@Singleton
internal class BatterySettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BatteryHistorySettingsStore {
    private val enabled = booleanPreferencesKey("enabled")

    override val settings: Flow<BatteryHistorySettings> = context.batteryStore.data.map { BatteryHistorySettings(it[enabled] ?: true) }

    override suspend fun update(transform: (BatteryHistorySettings) -> BatteryHistorySettings) {
        context.batteryStore.edit { prefs ->
            prefs[enabled] = transform(BatteryHistorySettings(prefs[enabled] ?: true)).enabled
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object BatteryDatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): BatteryDatabase =
        Room.databaseBuilder(context, BatteryDatabase::class.java, BatteryDatabase.NAME).build()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BatteryDataModule {
    @Binds
    abstract fun history(impl: RoomBatteryHistory): BatteryHistory

    @Binds
    abstract fun sampling(impl: JobBatterySampling): BatterySampling

    @Binds
    abstract fun settings(impl: BatterySettingsDataStore): BatteryHistorySettingsStore
}
