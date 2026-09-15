package com.pnd.android.loop.appwidget.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.layout.*
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.StatisticsWidgetData
import com.pnd.android.loop.appwidget.StatisticsWidgetKind
import java.text.NumberFormat
import java.time.LocalDate

/** Content is budgeted in real text pixels, including the user's font scale, before composing. */
@Composable
internal fun CompactStatisticsWidgetContent(
    kind: StatisticsWidgetKind,
    data: StatisticsWidgetData?,
    title: String,
    modifier: GlanceModifier,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val scale = context.resources.configuration.fontScale.coerceAtLeast(1f)
    val padding = if (size.width.value < 100 || size.height.value < 100) 6f else 12f
    val width = (size.width.value - padding * 2).coerceAtLeast(1f)
    val height = (size.height.value - padding * 2).coerceAtLeast(1f)
    val locale = context.resources.configuration.locales[0]
    fun rate(done: Int, total: Int) = if (total <= 0) "–"
        else NumberFormat.getPercentInstance(locale).format(done.toDouble() / total)
    fun number(value: Number) = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 1
    }.format(value)
    fun time(ms: Long): String {
        val minutes = ms.coerceAtLeast(0) / 60_000
        return if (minutes < 60) context.getString(R.string.stat_widget_minutes, minutes)
        else context.getString(R.string.stat_widget_duration, minutes / 60, minutes % 60)
    }
    val shortTitle = context.getString(when (kind) {
        StatisticsWidgetKind.TODAY -> R.string.stat_widget_short_today
        StatisticsWidgetKind.WEEK -> R.string.stat_widget_short_week
        StatisticsWidgetKind.MONTH -> R.string.stat_widget_short_month
    })
    val valid = data != null && runCatching { LocalDate.parse(data.date) }.isSuccess
    val empty = valid && data?.registered == 0
    val values: List<String>
    val description: String
    val supporting: List<String>
    if (!valid || empty) {
        description = context.getString(if (empty) R.string.stat_widget_empty else R.string.stat_widget_loading)
        values = listOf(context.getString(if (empty) R.string.stat_widget_short_empty else R.string.stat_widget_short_loading))
        supporting = listOf(context.getString(R.string.stat_widget_empty_hint)).takeIf { empty }.orEmpty()
    } else {
        requireNotNull(data)
        when (kind) {
            StatisticsWidgetKind.TODAY -> {
                values = listOf(rate(data.todayDone, data.todayTotal))
                description = context.getString(R.string.stat_widget_done_of, data.todayDone, data.todayTotal)
                supporting = listOf(description,
                    context.getString(R.string.stat_widget_current_streak) + " · " + context.getString(R.string.stat_widget_days, data.currentStreak),
                    context.getString(R.string.stat_widget_invested) + " · " + time(data.todayTimeMs))
            }
            StatisticsWidgetKind.WEEK -> {
                val count = number(data.weekDone)
                values = listOf(context.getString(R.string.stat_widget_short_count, count), count,
                    compactNumber(data.weekDone.toDouble(), locale))
                description = context.getString(R.string.stat_widget_completed, data.weekDone)
                supporting = listOf(context.getString(R.string.stat_widget_rate, rate(data.weekDone, data.weekTotal)),
                    context.getString(R.string.stat_widget_best),
                    data.bestTitle.ifBlank { context.getString(R.string.stat_widget_no_completions) })
            }
            StatisticsWidgetKind.MONTH -> {
                val ms = (data.monthTimes.lastOrNull() ?: 0L).coerceAtLeast(0)
                val unitValue = if (ms < 3_600_000L) ms / 60_000.0 else ms / 3_600_000.0
                val unit = if (ms < 3_600_000L) "m" else "h"
                values = listOf(time(ms), number(unitValue) + unit, compactNumber(unitValue, locale) + unit)
                description = context.getString(R.string.stat_widget_invested) + " · " + time(ms)
                supporting = listOf(context.getString(R.string.stat_widget_invested),
                    context.getString(R.string.stat_widget_rate, rate(data.monthDone, data.monthTotal)),
                    context.getString(R.string.stat_widget_longest_streak) + " · " + context.getString(R.string.stat_widget_days, data.longestStreak))
            }
        }
    }
    val accessible = "$title, $description, ${supporting.joinToString(", ")}"
    val horizontal = size.height < (100 * scale).dp && size.width >= (180 * scale).dp
    if (horizontal) {
        val titleWidth = width * 0.4f
        val heading = fitLine(context, listOf(title, shortTitle), titleWidth, height, 14, true)
        val primary = fitLine(context, values, width - titleWidth - 8, height, 30, true)
        Row(modifier.semantics { contentDescription = accessible }.padding(padding.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.width(titleWidth.dp)) { heading?.let { CompactLabel(it, true) } }
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) { primary?.let { CompactLabel(it, true, true) } }
        }
        return
    }
    val heading = fitLine(context, listOf(title, shortTitle), width, (height * 0.34f).coerceAtMost(24 * scale), 14, true)
    val gap = if (height >= 90 * scale) 6f else 2f
    var remaining = height - (heading?.height?.plus(gap) ?: 0f)
    val primary = fitLine(context, values, width, remaining, if (width >= 120 * scale) 30 else 24, true)
    remaining -= primary?.height ?: 0f
    val extras = mutableListOf<CompactLine>()
    // Only show a supporting group if all its lines fit; do not leave an orphaned 'best loop' label.
    val groups = if (kind == StatisticsWidgetKind.WEEK && supporting.size == 3)
        listOf(supporting.take(1), supporting.drop(1)) else supporting.map { listOf(it) }
    for (group in groups) {
        val lines = group.map { fitLine(context, listOf(it), width, remaining - gap, 12, false) }
        val cost = lines.filterNotNull().sumOf { (it.height + gap).toDouble() }.toFloat()
        if (lines.all { it != null } && cost <= remaining) {
            extras += lines.filterNotNull()
            remaining -= cost
        }
    }
    Column(modifier.semantics { contentDescription = accessible }.padding(padding.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically) {
        if (heading != null) {
            CompactLabel(heading, true)
            Spacer(GlanceModifier.height(gap.dp))
        }
        primary?.let { CompactLabel(it, true, true) }
        // Keep Glance's ten-direct-child limit even as supplementary rows are added.
        Column {
            extras.forEach {
                Spacer(GlanceModifier.height(gap.dp))
                CompactLabel(it)
            }
        }
    }
}

private data class CompactLine(val text: String, val size: Int, val height: Float)

private fun fitLine(context: Context, candidates: List<String>, width: Float, height: Float,
    maximumSize: Int, bold: Boolean): CompactLine? {
    val metrics = context.resources.displayMetrics
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    // At extreme font scales keep the main number visible; never shrink below an 11dp glyph size.
    val minimumSize = if (maximumSize >= 24)
        kotlin.math.ceil(11 / context.resources.configuration.fontScale.coerceAtLeast(1f)).toInt().coerceAtLeast(8)
        else 11
    for (size in maximumSize downTo minimumSize) {
        // applyDimension also honors Android's non-linear accessibility font scaling.
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size.toFloat(), metrics)
        val lineHeight = (paint.fontMetrics.bottom - paint.fontMetrics.top) / metrics.density + 2f
        if (lineHeight > height) continue
        for (text in candidates) {
            if (paint.measureText(text) / metrics.density <= width - 2f)
                return CompactLine(text, size, lineHeight)
        }
    }
    return null
}

@Composable
private fun CompactLabel(line: CompactLine, bold: Boolean = false, primary: Boolean = false) {
    Text(line.text, modifier = GlanceModifier.fillMaxWidth().height(line.height.dp), maxLines = 1,
        style = TextStyle(fontSize = line.size.sp,
            color = if (primary) accent() else if (bold) textPrimary() else onSurfaceTint(0.68f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal))
}

/** Familiar SI abbreviations keep unusually large counts readable without silently clipping. */
private fun compactNumber(value: Double, locale: java.util.Locale): String {
    val units = listOf("", "k", "M", "G", "T", "P", "E")
    var amount = value
    var index = 0
    while (amount >= 1000 && index < units.lastIndex) { amount /= 1000; index++ }
    return NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }.format(amount) + units[index]
}
