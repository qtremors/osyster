package dev.qtremors.osyster.feature.quicktiles

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
 * One widget, several cards: swipe between battery, memory, storage, data today, and screen time.
 * Android refreshes it every 30 minutes; tapping a card opens Osyster.
 */
class GlanceStackWidget : AppWidgetProvider() {
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

private data class GlanceCard(val title: String, val value: String, val detail: String)

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
            add(GlanceCard(context.getString(R.string.glance_card_battery), "$percent%", context.getString(if (reading.isCharging) R.string.glance_card_charging else R.string.glance_card_on_battery)))
        }
        usage(reading?.ramUsedBytes?.getOrNull(), reading?.ramTotalBytes?.getOrNull())?.let { (used, total) ->
            add(GlanceCard(context.getString(R.string.glance_card_memory), UsageFormat.bytes(used), context.getString(R.string.glance_card_of, UsageFormat.bytes(total))))
        }
        usage(reading?.storageUsedBytes?.getOrNull(), reading?.storageTotalBytes?.getOrNull())?.let { (used, total) ->
            add(GlanceCard(context.getString(R.string.glance_card_storage), UsageFormat.bytes(used), context.getString(R.string.glance_card_of, UsageFormat.bytes(total))))
        }
        if (data.accessState() == UsageAccessState.GRANTED) {
            runCatchingCancellable { data.report(DataPeriod.TODAY, includeApps = false).totals }.getOrNull()?.let { totals ->
                add(GlanceCard(context.getString(R.string.glance_card_data), UsageFormat.bytes(totals.mobile + totals.wifi), context.getString(R.string.glance_card_data_split, UsageFormat.bytes(totals.mobile), UsageFormat.bytes(totals.wifi))))
            }
            val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            runCatchingCancellable { screen.day(todayStart) }.getOrNull()?.let { day ->
                add(GlanceCard(context.getString(R.string.glance_card_screen), UsageFormat.duration(day.totalMillis), context.getString(R.string.glance_card_today)))
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
            setOnClickFillInIntent(R.id.glance_card, Intent())
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
