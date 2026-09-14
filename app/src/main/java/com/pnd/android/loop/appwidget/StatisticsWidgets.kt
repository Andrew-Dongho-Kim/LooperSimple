package com.pnd.android.loop.appwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.fasterxml.jackson.databind.ObjectMapper
import com.pnd.android.loop.appwidget.ui.StatisticsWidgetContent
import com.pnd.android.loop.ui.theme.AppWidgetColorProviders

internal enum class StatisticsWidgetKind { TODAY, WEEK, MONTH }
internal val KEY_STATISTICS_JSON = stringPreferencesKey("statistics_widget_v1")
internal val statisticsMapper = ObjectMapper()

abstract class StatisticsWidget internal constructor(private val kind: StatisticsWidgetKind) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val json = currentState(KEY_STATISTICS_JSON)
            val data = remember(json) {
                json?.let { runCatching { statisticsMapper.readValue(it, StatisticsWidgetData::class.java) }.getOrNull() }
            }
            GlanceTheme(colors = AppWidgetColorProviders) { StatisticsWidgetContent(kind, data) }
        }
    }
}
class TodayStatisticsWidget : StatisticsWidget(StatisticsWidgetKind.TODAY)
class WeeklyStatisticsWidget : StatisticsWidget(StatisticsWidgetKind.WEEK)
class MonthlyStatisticsWidget : StatisticsWidget(StatisticsWidgetKind.MONTH)

abstract class StatisticsWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        AppWidgetUpdateWorker.schedulePeriodicUpdate(context)
        AppWidgetUpdateWorker.updateWidget(context)
    }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWidgetUpdatesIfUnused(context)
    }
}
class TodayStatisticsWidgetReceiver : StatisticsWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayStatisticsWidget()
}
class WeeklyStatisticsWidgetReceiver : StatisticsWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeeklyStatisticsWidget()
}
class MonthlyStatisticsWidgetReceiver : StatisticsWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthlyStatisticsWidget()
}

/** onDisabled is per provider. Keep periodic work while any of the four providers is placed. */
internal fun cancelWidgetUpdatesIfUnused(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(AppWidgetReceiver::class.java, TodayStatisticsWidgetReceiver::class.java,
        WeeklyStatisticsWidgetReceiver::class.java, MonthlyStatisticsWidgetReceiver::class.java)
    if (receivers.none { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }) {
        AppWidgetUpdateWorker.cancelPeriodicUpdate(context)
    }
}
