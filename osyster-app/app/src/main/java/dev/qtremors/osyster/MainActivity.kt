package dev.qtremors.osyster

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.ui.theme.OsysterTheme
import dev.qtremors.osyster.core.ui.theme.ThemeOptions
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import dev.qtremors.osyster.shell.OsysterAppShell
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeSettings: ThemeSettings

    private val themeOptions = MutableStateFlow<ThemeOptions?>(null)
    private val loadedOptions: StateFlow<ThemeOptions?> = themeOptions.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            themeSettings.options.collect { themeOptions.value = it }
        }
        // Keep the splash until the saved theme loads, so the first frame never flashes the wrong
        // colors. A 2 second ceiling keeps a slow disk from holding the app hostage.
        val shownAt = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            themeOptions.value == null && SystemClock.uptimeMillis() - shownAt < SPLASH_CEILING_MS
        }

        setContent {
            val options by loadedOptions.collectAsStateWithLifecycle()
            OsysterTheme(options = options ?: ThemeOptions()) {
                OsysterAppShell(
                    appVersion = BuildConfig.VERSION_NAME,
                    onOpenUrl = ::openUrl,
                    onOpenBatteryUsage = ::openBatteryUsage
                )
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            // No browser installed: nothing to open, and nothing the user can fix from here.
        }
    }

    /** The first battery screen this phone can open: per-app usage, then general battery settings. */
    private fun openBatteryUsage() {
        val candidates = listOf(
            Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
            Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
        )
        candidates.firstOrNull { it.resolveActivity(packageManager) != null }?.let { intent ->
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // Resolved a moment ago but gone now; nothing else to offer.
            }
        }
    }

    private companion object {
        const val SPLASH_CEILING_MS = 2_000L
    }
}
