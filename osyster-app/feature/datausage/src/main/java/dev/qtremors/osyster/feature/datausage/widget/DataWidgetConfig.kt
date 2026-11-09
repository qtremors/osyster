package dev.qtremors.osyster.feature.datausage.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.StringRes
import dev.qtremors.osyster.core.ui.format.UsageFormat
import dev.qtremors.osyster.core.usage.domain.DataPeriod
import dev.qtremors.osyster.core.usage.domain.TrafficTotals
import dev.qtremors.osyster.feature.datausage.R

/** What the two lines of a data widget count. */
enum class WidgetTraffic {
    /** Mobile on top, Wi-Fi below. */
    NETWORK,

    /** Downloaded on top, uploaded below, mobile and Wi-Fi together. */
    DIRECTION
}

/** The choices made for one placed widget in its setup screen. */
data class DataWidgetConfig(val period: DataPeriod, val traffic: WidgetTraffic = WidgetTraffic.NETWORK)

/**
 * Each placed widget's choices, kept by widget ID. A widget with nothing saved (placed before
 * setup existed, or added from Osyster's Widgets page) uses its own default period.
 */
internal object DataWidgetSettings {
    private const val FILE = "data_widgets"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun has(context: Context, widgetId: Int): Boolean = prefs(context).contains(periodKey(widgetId))

    fun read(context: Context, widgetId: Int, defaultPeriod: DataPeriod): DataWidgetConfig {
        val prefs = prefs(context)
        val period = prefs.getString(periodKey(widgetId), null)
            ?.let { saved -> DataPeriod.entries.firstOrNull { it.name == saved } } ?: defaultPeriod
        val traffic = prefs.getString(trafficKey(widgetId), null)
            ?.let { saved -> WidgetTraffic.entries.firstOrNull { it.name == saved } } ?: WidgetTraffic.NETWORK
        return DataWidgetConfig(period, traffic)
    }

    fun write(context: Context, widgetId: Int, config: DataWidgetConfig) {
        prefs(context).edit()
            .putString(periodKey(widgetId), config.period.name)
            .putString(trafficKey(widgetId), config.traffic.name)
            .apply()
    }

    fun delete(context: Context, widgetIds: IntArray) {
        val editor = prefs(context).edit()
        widgetIds.forEach { editor.remove(periodKey(it)).remove(trafficKey(it)) }
        editor.apply()
    }

    private fun periodKey(widgetId: Int) = "period_$widgetId"
    private fun trafficKey(widgetId: Int) = "traffic_$widgetId"
}

/** The period's name on the widget, kept short so a 2x1 widget has room for the number. */
@StringRes
internal fun DataPeriod.widgetLabel(): Int = when (this) {
    DataPeriod.TODAY -> R.string.widget_period_today
    DataPeriod.WEEK -> R.string.widget_period_week
    DataPeriod.MONTH -> R.string.widget_period_month
}

/** The period's name in the setup screen, the same words as the Data usage page. */
@StringRes
internal fun DataPeriod.choiceLabel(): Int = when (this) {
    DataPeriod.TODAY -> R.string.data_period_today
    DataPeriod.WEEK -> R.string.data_period_week
    DataPeriod.MONTH -> R.string.data_period_month
}

@StringRes
internal fun WidgetTraffic.choiceLabel(): Int = when (this) {
    WidgetTraffic.NETWORK -> R.string.widget_traffic_network
    WidgetTraffic.DIRECTION -> R.string.widget_traffic_direction
}

/**
 * Fills a single-period widget, compact or large (they share view IDs): the period, then two
 * values with matching icons. With [totals] null the values show a placeholder, and [placeholder]
 * replaces the top one (for example "Tap to set up").
 */
internal fun fillDataWidget(
    context: Context,
    views: RemoteViews,
    config: DataWidgetConfig,
    totals: TrafficTotals?,
    placeholder: String? = null
) {
    views.setTextViewText(R.id.widget_interval_label, context.getString(config.period.widgetLabel()))
    val unknown = context.getString(R.string.widget_unknown)
    val (top, bottom) = when (config.traffic) {
        WidgetTraffic.NETWORK -> totals?.mobile to totals?.wifi
        WidgetTraffic.DIRECTION -> totals?.let { it.mobileRx + it.wifiRx } to totals?.let { it.mobileTx + it.wifiTx }
    }
    views.setTextViewText(R.id.widget_mobile_text, top?.let(UsageFormat::bytes) ?: placeholder ?: unknown)
    views.setTextViewText(R.id.widget_wifi_text, bottom?.let(UsageFormat::bytes) ?: unknown)

    val icons = when (config.traffic) {
        WidgetTraffic.NETWORK -> listOf(R.drawable.ic_mobile_data to R.string.network_mobile_label, R.drawable.ic_wifi to R.string.network_wifi_label)
        WidgetTraffic.DIRECTION -> listOf(R.drawable.ic_download to R.string.data_downloaded, R.drawable.ic_upload to R.string.data_uploaded)
    }
    listOf(R.id.widget_mobile_icon, R.id.widget_wifi_icon).zip(icons).forEach { (view, icon) ->
        views.setImageViewResource(view, icon.first)
        views.setContentDescription(view, context.getString(icon.second))
    }
}
