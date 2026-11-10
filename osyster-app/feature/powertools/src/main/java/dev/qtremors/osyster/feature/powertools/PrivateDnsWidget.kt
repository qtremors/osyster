package dev.qtremors.osyster.feature.powertools

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.privilege.domain.Keys
import dev.qtremors.osyster.core.privilege.domain.PrivateDnsMode
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.privilege.domain.isValidDnsHost
import dev.qtremors.osyster.core.privilege.domain.togglePrivateDnsMode
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A 2x1 Private DNS switch. Off turns on to the provider saved in System tweaks, or to automatic
 * without one. Without settings access the card says so, and a tap opens Power setup instead.
 * Changes from the tile or System tweaks refresh it through [PrivateDnsWidget.updateAll].
 */
@AndroidEntryPoint
class PrivateDnsWidget : AppWidgetProvider() {
    @Inject lateinit var settings: SystemSettings
    @Inject lateinit var shell: PrivilegedShell

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        launch { render(context, manager, ids) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        launch {
            if (shell.status.value.canWriteSettings) {
                val current = PrivateDnsMode.parse(settings.read(Keys.PrivateDnsMode))
                val hasHost = settings.read(Keys.PrivateDnsHost)?.let(::isValidDnsHost) == true
                settings.write(Keys.PrivateDnsMode, togglePrivateDnsMode(current, hasHost).value)
            }
            val manager = AppWidgetManager.getInstance(context)
            render(context, manager, manager.getAppWidgetIds(ComponentName(context, PrivateDnsWidget::class.java)))
        }
    }

    /** Runs [block] off the main thread while keeping the receiver alive until it finishes. */
    private fun launch(block: suspend () -> Unit) {
        val pending = goAsync()
        scope.launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        shell.refresh()
        val access = shell.status.value.canWriteSettings
        val mode = PrivateDnsMode.parse(settings.read(Keys.PrivateDnsMode))
        val on = mode != PrivateDnsMode.OFF
        val state = when {
            !access -> context.getString(R.string.tile_set_up)
            mode == PrivateDnsMode.OFF -> context.getString(R.string.tweaks_dns_off)
            mode == PrivateDnsMode.AUTOMATIC -> context.getString(R.string.tweaks_dns_automatic)
            else -> settings.read(Keys.PrivateDnsHost) ?: context.getString(R.string.tweaks_dns_provider)
        }
        val views = RemoteViews(context.packageName, R.layout.widget_private_dns).apply {
            setTextViewText(R.id.dns_widget_state, state)
            setImageViewResource(R.id.dns_widget_switch, if (on) R.drawable.power_widget_switch_on else R.drawable.power_widget_switch_off)
            setContentDescription(
                R.id.dns_widget_root,
                context.getString(if (on) R.string.widget_dns_turn_off else R.string.widget_dns_turn_on)
            )
            setOnClickPendingIntent(R.id.dns_widget_root, if (access) toggleIntent(context) else setupIntent(context))
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun toggleIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, PrivateDnsWidget::class.java).setAction(ACTION_TOGGLE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun setupIntent(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(DeepLinks.EXTRA_OPEN_TOOL, Tool.POWER_SETUP.id) ?: return null
        return PendingIntent.getActivity(context, 1, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val ACTION_TOGGLE = "dev.qtremors.osyster.action.TOGGLE_PRIVATE_DNS_WIDGET"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Redraws every Private DNS widget, after the tile or System tweaks changed the mode. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrivateDnsWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(ComponentName(context, PrivateDnsWidget::class.java))
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }
}
