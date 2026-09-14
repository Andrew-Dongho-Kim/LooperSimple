package com.pnd.android.loop.appwidget.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pnd.android.loop.HomeActivity
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.StatisticsWidgetData
import com.pnd.android.loop.appwidget.StatisticsWidgetKind
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.ui.ARGS_NAVIGATE_ACTION
import com.pnd.android.loop.ui.theme.AppWidgetPalette
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DateTextStyle

@Composable
internal fun StatisticsWidgetContent(kind: StatisticsWidgetKind, data: StatisticsWidgetData?) {
    val context = LocalContext.current
    val size = LocalSize.current
    val scale = context.resources.configuration.fontScale.coerceAtLeast(1f)
    val roomy = size.width >= (250 * scale).dp
    val chart = roomy && size.height >= (260 * scale).dp
    val details = size.height >= ((if (kind == StatisticsWidgetKind.TODAY) 260 else 190) * scale).dp
    val intent = Intent(context, HomeActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        this.data = NavigatePage.StatisticsPage.deepLink().toUri()
        putExtra(ARGS_NAVIGATE_ACTION, NavigatePage.StatisticsPage.route)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    val title = when (kind) {
        StatisticsWidgetKind.TODAY -> R.string.stat_widget_today
        StatisticsWidgetKind.WEEK -> R.string.stat_widget_week
        StatisticsWidgetKind.MONTH -> R.string.stat_widget_month
    }
    Column(
        GlanceModifier.fillMaxSize().appWidgetBackground().background(widgetSurface())
            .cornerRadius(WIDGET_CARD_RADIUS).clickable(actionStartActivity(intent))
            .padding(if (roomy) 18.dp else 12.dp),
    ) {
        Column {
            Label(context.getString(title), 15, bold = true)
            Spacer(GlanceModifier.height(5.dp))
            Box(GlanceModifier.width(22.dp).height(3.dp).cornerRadius(2.dp).background(accent())) {}
        }
        Spacer(GlanceModifier.height(10.dp))
        val date = data?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (data == null || date == null) {
            Label(context.getString(R.string.stat_widget_loading), 13, color = secondary())
        } else if (data.registered == 0) {
            Label(context.getString(R.string.stat_widget_empty), 16, bold = true)
            Spacer(GlanceModifier.height(8.dp))
            Label(context.getString(R.string.stat_widget_empty_hint), 12, color = secondary(), lines = 2)
        } else Column(GlanceModifier.fillMaxWidth()) {
            when (kind) {
                StatisticsWidgetKind.TODAY -> TodayStats(data, roomy, details)
                StatisticsWidgetKind.WEEK -> WeekStats(data, date, chart, details)
                StatisticsWidgetKind.MONTH -> MonthStats(data, date, chart, details)
            }
            if (size.height >= (420 * scale).dp) Column {
                Spacer(GlanceModifier.height(8.dp))
                val rule = if (kind == StatisticsWidgetKind.TODAY) R.string.stat_widget_today_rule
                    else R.string.stat_widget_rate_rule
                Label(context.getString(rule), 11, color = secondary(), lines = 2)
                if (data.estimated) Label(context.getString(R.string.stat_widget_estimated), 11, color = secondary())
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        if (details) Column {
            Spacer(GlanceModifier.height(8.dp))
            val stamp = date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: ""
            Label(context.getString(R.string.stat_widget_open, stamp), 11, color = secondary())
        }
    }
}

@Composable
private fun TodayStats(data: StatisticsWidgetData, roomy: Boolean, details: Boolean) {
    val context = LocalContext.current
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        if (roomy) {
            ProgressRing(data.todayDone, data.todayTotal)
            Spacer(GlanceModifier.width(14.dp))
        }
        Column(GlanceModifier.defaultWeight()) {
            Label(percent(data.todayDone, data.todayTotal), 30, bold = true)
            Label(context.getString(R.string.stat_widget_done_of, data.todayDone, data.todayTotal), 12, color = secondary())
        }
    }
    if (!details && LocalSize.current.height >= (175 * context.resources.configuration.fontScale.coerceAtLeast(1f)).dp) {
        Spacer(GlanceModifier.height(8.dp))
        Label(context.getString(R.string.stat_widget_current_streak) + " · " +
            context.getString(R.string.stat_widget_days, data.currentStreak), 12, color = secondary())
    }
    if (details) {
        Spacer(GlanceModifier.height(6.dp))
        val status = when {
            data.todayTotal == 0 -> context.getString(R.string.stat_widget_no_plan)
            data.todayDone == data.todayTotal -> context.getString(R.string.stat_widget_all_done)
            else -> context.getString(R.string.stat_widget_remaining,
                (data.todayTotal - data.todayDone - data.todaySkipped).coerceAtLeast(0), data.todaySkipped)
        }
        Label(status, 12, color = secondary())
        Spacer(GlanceModifier.height(10.dp))
        if (!roomy) {
            Label(context.getString(R.string.stat_widget_current_streak) + " · " +
                context.getString(R.string.stat_widget_days, data.currentStreak), 12)
            Label(context.getString(R.string.stat_widget_invested) + " · " + duration(data.todayTimeMs), 12)
        } else Row(GlanceModifier.fillMaxWidth()) {
            Metric(context.getString(R.string.stat_widget_days, data.currentStreak),
                context.getString(R.string.stat_widget_current_streak), GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            Metric(duration(data.todayTimeMs), context.getString(R.string.stat_widget_invested), GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun WeekStats(data: StatisticsWidgetData, date: LocalDate, chart: Boolean, details: Boolean) {
    val context = LocalContext.current
    Label(context.getString(R.string.stat_widget_completed, data.weekDone),
        if (LocalSize.current.width < 250.dp) 18 else 26, bold = true)
    Label(context.getString(R.string.stat_widget_rate, percent(data.weekDone, data.weekTotal)), 12, color = secondary())
    if (chart) {
        val start = date.minusDays(date.dayOfWeek.value - 1L)
        val locale = context.resources.configuration.locales[0]
        Spacer(GlanceModifier.height(10.dp))
        BarChart((0..6).map { index ->
            val count = data.weekCounts.getOrElse(index) { -1 }
            ChartBar(start.plusDays(index.toLong()).dayOfWeek.getDisplayName(DateTextStyle.NARROW, locale),
                count.coerceAtLeast(0).toFloat(), if (count < 0) "–" else NumberFormat.getIntegerInstance(locale).format(count),
                index == date.dayOfWeek.value - 1)
        })
    }
    if (details && LocalSize.current.height >= ((if (chart) 330 else 225) * context.resources.configuration.fontScale.coerceAtLeast(1f)).dp) {
        Spacer(GlanceModifier.height(10.dp))
        Column(GlanceModifier.fillMaxWidth().cornerRadius(14.dp).background(widgetWell(active = true)).padding(10.dp)) {
            Label(context.getString(R.string.stat_widget_best), 11, color = secondary())
            Label(data.bestTitle.ifBlank { context.getString(R.string.stat_widget_no_completions) }, 14, bold = true)
            if (data.bestTotal > 0) Label(context.getString(R.string.stat_widget_best_result,
                percent(data.bestDone, data.bestTotal), data.bestDone, data.bestTotal), 11, color = secondary())
        }
    }
}

@Composable
private fun MonthStats(data: StatisticsWidgetData, date: LocalDate, chart: Boolean, details: Boolean) {
    val context = LocalContext.current
    Label(duration(data.monthTimes.lastOrNull() ?: 0L), if (LocalSize.current.width < 250.dp) 20 else 26, bold = true)
    Label(context.getString(R.string.stat_widget_month_time, date.monthValue), 12, color = secondary())
    if (chart) {
        val month = YearMonth.from(date)
        val locale = context.resources.configuration.locales[0]
        Spacer(GlanceModifier.height(10.dp))
        BarChart((0..5).map { index ->
            val time = data.monthTimes.getOrElse(index) { 0L }
            ChartBar(month.minusMonths(5L - index).month.getDisplayName(DateTextStyle.SHORT, locale),
                time.toFloat(), NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }
                    .format(time / 3_600_000.0), index == 5)
        })
        Label(context.getString(R.string.stat_widget_hours_chart), 11, color = secondary())
    }
    if (details && LocalSize.current.height >= ((if (chart) 340 else 225) * context.resources.configuration.fontScale.coerceAtLeast(1f)).dp) {
        Spacer(GlanceModifier.height(10.dp))
        if (LocalSize.current.width < (250 * context.resources.configuration.fontScale.coerceAtLeast(1f)).dp) {
            Label(context.getString(R.string.stat_widget_rate, percent(data.monthDone, data.monthTotal)), 12)
            Label(context.getString(R.string.stat_widget_longest_streak) + " · " +
                context.getString(R.string.stat_widget_days, data.longestStreak), 12)
        } else Row(GlanceModifier.fillMaxWidth()) {
            Metric(percent(data.monthDone, data.monthTotal), context.getString(R.string.stat_widget_completion_rate), GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            Metric(context.getString(R.string.stat_widget_days, data.longestStreak),
                context.getString(R.string.stat_widget_longest_streak), GlanceModifier.defaultWeight())
        }
    }
}

private data class ChartBar(val label: String, val value: Float, val display: String, val current: Boolean)

@Composable
private fun BarChart(bars: List<ChartBar>) {
    val maximum = bars.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    Row(GlanceModifier.fillMaxWidth()) {
        bars.forEach { bar ->
            Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Label(bar.display, 11, align = TextAlign.Center)
                Spacer(GlanceModifier.height(4.dp))
                Box(GlanceModifier.height(44.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (bar.value > 0) {
                        Box(GlanceModifier.width(16.dp).height((44 * bar.value / maximum).coerceAtLeast(2f).dp)
                            .cornerRadius(5.dp).background(accent(if (bar.current) 1f else 0.45f))) {}
                    } else {
                        Box(GlanceModifier.width(16.dp).height(2.dp).background(onSurfaceTint(0.15f))) {}
                    }
                }
                Spacer(GlanceModifier.height(5.dp))
                Label(bar.label, 11, bold = bar.current, color = secondary(), align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProgressRing(done: Int, total: Int) {
    val ratio = if (total == 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val track = remember { ringBitmap(1f) }
    val progress = remember(ratio) { ringBitmap(ratio) }
    // Monochrome masks with day/night ColorProviders change theme without cached bitmap colors.
    Box(GlanceModifier.size(64.dp), contentAlignment = Alignment.Center) {
        Image(ImageProvider(track), null, GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(ringTrackColor()))
        Image(ImageProvider(progress), null, GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(accent()))
    }
}

private fun ringBitmap(ratio: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
    if (ratio > 0) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 16f
            strokeCap = Paint.Cap.ROUND
        }
        Canvas(bitmap).drawArc(RectF(12f, 12f, 148f, 148f), -90f, 360f * ratio, false, paint)
    }
    return bitmap
}

@Composable
private fun Metric(value: String, title: String, modifier: GlanceModifier) {
    Column(modifier.cornerRadius(14.dp).background(widgetWell()).padding(10.dp)) {
        Label(value, 14, bold = true)
        Spacer(GlanceModifier.height(2.dp))
        Label(title, 11, color = secondary())
    }
}

@Composable
private fun Label(text: String, size: Int, bold: Boolean = false, color: ColorProvider = textPrimary(),
    lines: Int = 1, align: TextAlign = TextAlign.Start) {
    Text(text, maxLines = lines, style = TextStyle(fontSize = size.sp, color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = align))
}

private fun secondary(): ColorProvider = onSurfaceTint(0.68f)

// Some ImageView hosts drop tint alpha. Composite the track into an opaque day/night color.
private fun ringTrackColor(): ColorProvider = androidx.glance.color.ColorProvider(
    day = AppWidgetPalette.onSurface.day.copy(alpha = 0.10f).compositeOver(AppWidgetPalette.surface.day),
    night = AppWidgetPalette.onSurface.night.copy(alpha = 0.10f).compositeOver(AppWidgetPalette.surface.night),
)

private fun percent(done: Int, total: Int): String = if (total <= 0) "–"
    else NumberFormat.getPercentInstance().format(done.toDouble() / total)

@Composable
private fun duration(ms: Long): String {
    val context = LocalContext.current
    val minutes = ms.coerceAtLeast(0) / 60_000L
    return if (minutes < 60) context.getString(R.string.stat_widget_minutes, minutes)
    else context.getString(R.string.stat_widget_duration, minutes / 60, minutes % 60)
}
