package com.pnd.android.loop.appwidget

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.appwidget.compose
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pnd.android.loop.R
import java.io.File
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses synthetic Glance state; never reads or modifies the loop database or launcher widgets. */
@RunWith(AndroidJUnit4::class)
class StatisticsWidgetRenderingTest {
    @Test fun rendersAllThreeWidgetsAtSmallAndDefaultSizesInBothThemes() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val data = StatisticsWidgetData().apply {
            date = "2026-09-13"; registered = 7
            todayDone = 5; todayTotal = 7; todayTimeMs = 6_000_000L
            currentStreak = 12; longestStreak = 21
            weekDone = 25; weekTotal = 32; weekCounts = listOf(4, 5, 3, 4, 3, 1, 5)
            bestTitle = "아침 운동"; bestDone = 5; bestTotal = 7
            monthDone = 38; monthTotal = 50
            monthTimes = listOf(7L, 11L, 9L, 15L, 12L, 18L).map { it * 3_600_000L }
        }
        val state = mutablePreferencesOf(KEY_STATISTICS_JSON to statisticsMapper.writeValueAsString(data))
        val cases = listOf(
            Triple(TodayStatisticsWidget(), R.string.stat_widget_today, 280 to 280),
            Triple(WeeklyStatisticsWidget(), R.string.stat_widget_week, 280 to 350),
            Triple(MonthlyStatisticsWidget(), R.string.stat_widget_month, 280 to 370),
        )
        for (night in listOf(false, true)) for ((widget, title, regularSize) in cases) {
            for ((width, height) in listOf(160 to 160, regularSize)) {
                val configuration = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    setLocale(Locale.KOREAN)
                    fontScale = 1f
                }
                val context = base.createConfigurationContext(configuration)
                val remote = widget.compose(context, size = DpSize(width.dp, height.dp), state = state)
                instrumentation.runOnMainSync {
                    val density = context.resources.displayMetrics.density
                    val w = (width * density).toInt()
                    val h = (height * density).toInt()
                    val view = remote.apply(context, FrameLayout(context))
                    view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                    view.layout(0, 0, w, h)
                    val labels = mutableListOf<String>()
                    fun inspect(child: View, x: Int, y: Int) {
                        if (child.visibility != View.VISIBLE) return
                        val left = x + child.left
                        val top = y + child.top
                        if (child is TextView && child.text.isNotBlank()) {
                            labels += child.text.toString()
                            assertTrue("Clipped ${child.text} at $width x $height: $top..${top + child.height} / $h",
                                left >= 0 && top >= 0 && left + child.width <= w && top + child.height <= h)
                        }
                        if (child is ViewGroup) for (index in 0 until child.childCount) inspect(child.getChildAt(index), left, top)
                    }
                    inspect(view, 0, 0)
                    assertTrue(labels.contains(context.getString(title)))
                    assertFalse(labels.contains(context.getString(R.string.stat_widget_loading)))
                    if (width > 160) {
                        assertTrue("Missing footer: $labels", labels.any { it.contains("통계 보기") })
                        when (widget) {
                            is TodayStatisticsWidget -> assertTrue(labels.contains("투자 시간"))
                            is WeeklyStatisticsWidget -> assertTrue(labels.contains("아침 운동"))
                            is MonthlyStatisticsWidget -> assertTrue(labels.contains("이번 달 완료율"))
                        }
                    }
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    val output = File(context.getExternalFilesDir(null), "widget-renders").apply { mkdirs() }
                    File(output, "${widget.javaClass.simpleName}-${width}-${if (night) "dark" else "light"}.png")
                        .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
        }
    }
}
