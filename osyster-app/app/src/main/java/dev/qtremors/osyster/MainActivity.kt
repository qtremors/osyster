package dev.qtremors.osyster

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.ui.theme.OsysterTheme
import dev.qtremors.osyster.core.ui.theme.ThemeOptions
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import dev.qtremors.osyster.feature.onboarding.OnboardingDestination
import dev.qtremors.osyster.feature.onboarding.StartGate
import dev.qtremors.osyster.shell.OsysterAppShell
import dev.qtremors.osyster.shell.SystemActions
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

    /** A tool a widget or tile asked to open; cleared once the shell has navigated to it. */
    private val pendingTool = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingTool.value = intent?.let(::requestedTool)

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
            val secure = options?.blockScreenshots == true
            LaunchedEffect(secure) {
                if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val openTool by pendingTool.collectAsStateWithLifecycle()
            OsysterTheme(options = options ?: ThemeOptions()) {
                StartGate(
                    onNavigate = { target ->
                        when (target) {
                            OnboardingDestination.UsageAccess -> systemActions.openUsageAccess()
                            OnboardingDestination.NotificationAccess -> systemActions.openNotificationAccess()
                        }
                    }
                ) {
                    OsysterAppShell(
                        appVersion = BuildConfig.VERSION_NAME,
                        system = systemActions,
                        openTool = openTool,
                        onToolOpened = { pendingTool.value = null }
                    )
                }
            }
        }
    }

    private val systemActions = SystemActions(
        openUrl = { url -> launch(Intent(Intent.ACTION_VIEW, url.toUri())) },
        openBatteryUsage = ::openBatteryUsage,
        openAppSettings = {
            launch(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
        },
        shareText = { text ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            launch(Intent.createChooser(send, getString(R.string.share_report_title)))
        },
        openUsageAccess = {
            // Some phones support jumping straight to Osyster's entry; fall back to the full list.
            val direct = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:$packageName".toUri())
            if (direct.resolveActivity(packageManager) != null) launch(direct)
            else launch(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
        },
        openAppInfo = { pkg ->
            launch(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$pkg".toUri()))
        },
        openDeveloperOptions = { launch(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) },
        openDefaultApps = { launch(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) },
        openNotificationAccess = { launch(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
        launchApp = { pkg -> packageManager.getLaunchIntentForPackage(pkg)?.let(::launch) },
        // A store app if one is installed, otherwise the browser. Osyster itself never goes online.
        openStoreListing = { pkg ->
            val market = Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri())
            if (market.resolveActivity(packageManager) != null) launch(market)
            else launch(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pkg".toUri()))
        }
    )

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedTool(intent)?.let { pendingTool.value = it }
    }

    /** The tool a widget, tile, or shortcut asked for, as "tool" or "tool/page". */
    private fun requestedTool(intent: Intent): String? {
        val tool = intent.getStringExtra(DeepLinks.EXTRA_OPEN_TOOL) ?: return null
        return intent.getStringExtra(DeepLinks.EXTRA_OPEN_PAGE)?.let { "$tool/$it" } ?: tool
    }

    /** Starts another app's screen; if it vanished, there is nothing the user can fix from here. */
    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
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
