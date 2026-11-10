package dev.qtremors.osyster

import android.app.Application
import dev.qtremors.osyster.core.battery.domain.BatteryHistorySettingsStore
import dev.qtremors.osyster.core.battery.domain.BatterySampling
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OsysterApplication : Application() {
    @Inject lateinit var batterySampling: BatterySampling
    @Inject lateinit var batterySettings: BatteryHistorySettingsStore

    override fun onCreate() {
        super.onCreate()
        LauncherShortcuts.publish(this)
        // Battery history's reading is scheduled again on each start, so a phone restart does
        // not stop it for long; an already waiting job is left as it is.
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { batterySampling.schedule(batterySettings.settings.first().enabled) }
        }
    }
}
