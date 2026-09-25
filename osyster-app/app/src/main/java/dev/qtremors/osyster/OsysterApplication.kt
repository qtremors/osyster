package dev.qtremors.osyster

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OsysterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LauncherShortcuts.publish(this)
    }
}
