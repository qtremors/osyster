package dev.qtremors.osyster.feature.quicktiles

import kotlin.math.roundToInt
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.os.Build
import android.view.View
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.device.domain.fullnessStatus
import dev.qtremors.osyster.core.device.domain.batteryStatus
import dev.qtremors.osyster.core.device.domain.LevelStatus
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.device.domain.DeviceSummary
import dev.qtremors.osyster.core.device.domain.DeviceSummarySource
import dev.qtremors.osyster.core.device.domain.getOrNull
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.DataUsageSource
import dev.qtremors.osyster.core.usage.domain.ScreenUsageSource
import dev.qtremors.osyster.core.usage.domain.UsageAccessState
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One widget listing battery, memory, storage, data today, and screen time. A list (not a stack
 * of cards) keeps every reading readable at any size. Android refreshes it every 30 minutes;
 * tapping a reading opens Osyster, or the matching tool for data and screen time.
 */
class GlanceStackWidget : AppWidgetProvider() {
    // RemoteViewsService adapters are deprecated on Android 15 in favor of static collections, but
    // they are the only way to feed a widget list on Android 8 to 11, which Osyster supports.
    @Suppress("DEPRECATION")
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val adapter = Intent(context, GlanceStackService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            adapter.data = android.net.Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME))
            val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val views = RemoteViews(context.packageName, R.layout.widget_glance_stack).apply {
                setRemoteAdapter(R.id.glance_stack, adapter)
                setEmptyView(R.id.glance_stack, R.id.glance_empty)
                if (open != null) {
                    setPendingIntentTemplate(R.id.glance_stack, PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                }
            }
            manager.updateAppWidget(id, views)
            manager.notifyAppWidgetViewDataChanged(id, R.id.glance_stack)
        }
    }

}

@AndroidEntryPoint
class GlanceStackService : RemoteViewsService() {
    @Inject lateinit var summary: DeviceSummarySource
    @Inject lateinit var data: DataUsageSource
    @Inject lateinit var screen: ScreenUsageSource

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = GlanceFactory(applicationContext, summary, data, screen)
}

/** [level] is 0 to 1 for a level bar; [tool] is the tool a tap opens, or null for Home. */
private data class GlanceCard(
    val title: String,
    val value: String,
    val detail: String,
    val level: Float? = null,
    val status: LevelStatus = LevelStatus.GOOD,
    val tool: Tool? = null
)

/** Runs on a binder thread, so blocking briefly for one reading is allowed here. */
private class GlanceFactory(
    private val context: Context,
    private val summary: DeviceSummarySource,
    private val data: DataUsageSource,
    private val screen: ScreenUsageSource
) : RemoteViewsService.RemoteViewsFactory {
    private var cards: List<GlanceCard> = emptyList()

    override fun onCreate() = Unit
    override fun onDestroy() = Unit

    override fun onDataSetChanged() {
        cards = runBlocking { withTimeoutOrNull(READ_TIMEOUT_MS) { readCards() } }.orEmpty()
    }

    private suspend fun readCards(): List<GlanceCard> = buildList {
        val reading: DeviceSummary? = runCatchingCancellable { summary.summary().first() }.getOrNull()
        reading?.batteryPercent?.getOrNull()?.let { percent ->
            add(GlanceCard(context.getString(R.string.glance_card_battery), "$percent%", context.getString(if (reading.isCharging) R.string.glance_card_charging else R.string.glance_card_on_battery),
                level = percent / 100f, status = batteryStatus(percent / 100f, reading.isCharging)))
        }
        usage(reading?.ramUsedBytes?.getOrNull(), reading?.ramTotalBytes?.getOrNull())?.let { (used, total) ->
            add(GlanceCard(context.getString(R.string.glance_card_memory), UsageFormat.bytes(used), context.getString(R.string.glance_card_of, UsageFormat.bytes(total)),
                level = used.toFloat() / total, status = fullnessStatus(used.toFloat() / total)))
        }
        usage(reading?.storageUsedBytes?.getOrNull(), reading?.storageTotalBytes?.getOrNull())?.let { (used, total) ->
            add(GlanceCard(context.getString(R.string.glance_card_storage), UsageFormat.bytes(used), context.getString(R.string.glance_card_of, UsageFormat.bytes(total)),
                level = used.toFloat() / total, status = fullnessStatus(used.toFloat() / total)))
        }
        if (data.accessState() == UsageAccessState.GRANTED) {
            runCatchingCancellable { data.report(DataPeriod.TODAY, includeApps = false).totals }.getOrNull()?.let { totals ->
                add(GlanceCard(context.getString(R.string.glance_card_data), UsageFormat.bytes(totals.mobile + totals.wifi), context.getString(R.string.glance_card_data_split, UsageFormat.bytes(totals.mobile), UsageFormat.bytes(totals.wifi)), tool = Tool.DATA_USAGE))
            }
            val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            runCatchingCancellable { screen.day(todayStart) }.getOrNull()?.let { day ->
                add(GlanceCard(context.getString(R.string.glance_card_screen), UsageFormat.duration(day.totalMillis), context.getString(R.string.glance_card_today), tool = Tool.SCREEN_USAGE))
            }
        }
    }

    private fun usage(used: Long?, total: Long?): Pair<Long, Long>? =
        if (used == null || total == null || total <= 0) null else used to total

    override fun getCount(): Int = cards.size

    override fun getViewAt(position: Int): RemoteViews {
        val card = cards.getOrNull(position)
        return RemoteViews(context.packageName, R.layout.widget_glance_card).apply {
            setTextViewText(R.id.glance_title, card?.title.orEmpty())
            setTextViewText(R.id.glance_value, card?.value.orEmpty())
            setTextViewText(R.id.glance_detail, card?.detail.orEmpty())
            val level = card?.level
            if (level != null) {
                setViewVisibility(R.id.glance_bar, View.VISIBLE)
                setProgressBar(R.id.glance_bar, 100, (level * 100).roundToInt().coerceIn(0, 100), false)
                // Green, amber, or red like Home; Android 12+ lets widgets tint a bar.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setColorStateList(R.id.glance_bar, "setProgressTintList", ColorStateList.valueOf(statusColor(card.status)))
                }
            } else {
                setViewVisibility(R.id.glance_bar, View.GONE)
            }
            // Data and screen time open their tool; the rest open Home.
            setOnClickFillInIntent(R.id.glance_card, Intent().apply { card?.tool?.let { putExtra(DeepLinks.EXTRA_OPEN_TOOL, it.id) } })
        }
    }

    private fun statusColor(status: LevelStatus): Int {
        val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return when (status) {
            LevelStatus.GOOD -> if (dark) 0xFF81C784.toInt() else 0xFF2E7D32.toInt()
            LevelStatus.WARNING -> if (dark) 0xFFFFCA28.toInt() else 0xFFE08600.toInt()
            LevelStatus.CRITICAL -> if (dark) 0xFFEF6C6C.toInt() else 0xFFC62828.toInt()
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true

    private companion object {
        const val READ_TIMEOUT_MS = 8_000L
    }
}
