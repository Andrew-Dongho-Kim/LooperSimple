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

/** Synthetic state only: does not read loop records or modify the user's launcher widgets. */
@RunWith(AndroidJUnit4::class)
class StatisticsWidgetRenderingTest {
    @Test fun rendersResizeMatrixWithoutClipping() = runBlocking {
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
        val states = listOf("normal" to data, "empty" to StatisticsWidgetData().apply { date = "2026-09-13" },
            "loading" to null, "large" to StatisticsWidgetData().apply {
                date = "2026-09-13"; registered = 1; todayDone = 100; todayTotal = 100
                weekDone = Int.MAX_VALUE; weekTotal = Int.MAX_VALUE
                monthTimes = listOf(123456L * 3_600_000L)
            })
        val cases = listOf(
            Triple(TodayStatisticsWidget(), R.string.stat_widget_today, 280 to 280),
            Triple(WeeklyStatisticsWidget(), R.string.stat_widget_week, 280 to 350),
            Triple(MonthlyStatisticsWidget(), R.string.stat_widget_month, 280 to 370),
        )
        for ((stateName, sample) in states) for (locale in listOf(Locale.KOREAN, Locale.ENGLISH)) {
            for (scale in listOf(1f, 2f)) for (night in listOf(false, true)) {
                for ((widget, title, regularSize) in cases) {
                    val sizes = if (stateName == "normal") listOf(56 to 56, 80 to 180, 100 to 100,
                        160 to 160, 240 to 56, 400 to 80, 280 to 259, 280 to 260, 280 to 280, regularSize)
                        else listOf(56 to 56, 160 to 160)
                    for ((width, height) in sizes) {
                        val configuration = Configuration(base.resources.configuration).apply {
                            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                                if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                            setLocale(locale)
                            fontScale = scale
                        }
                        val context = base.createConfigurationContext(configuration)
                        val state = mutablePreferencesOf().apply {
                            if (sample != null) this[KEY_STATISTICS_JSON] = statisticsMapper.writeValueAsString(sample)
                        }
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
                            val compact = width < 250 * scale || height <
                                (if (widget is MonthlyStatisticsWidget) 280 else 260) * scale
                            val name = "${widget.javaClass.simpleName}-${width}x$height-${locale.language}-${scale}-${if (night) "dark" else "light"}-$stateName"
                            fun inspect(child: View, x: Int, y: Int) {
                                if (child.visibility != View.VISIBLE) return
                                val left = x + child.left
                                val top = y + child.top
                                if (child is TextView && child.text.isNotBlank()) {
                                    labels += child.text.toString()
                                    assertTrue("$name: clipped ${child.text} at $left,$top (${child.width}x${child.height}) / $w,$h",
                                        left >= 0 && top >= 0 && child.width > 0 && child.height > 0 &&
                                            left + child.width <= w && top + child.height <= h)
                                    if (compact) {
                                        val layout = child.layout
                                        assertNotNull("$name: missing text layout", layout)
                                        assertTrue("$name: ellipsized ${child.text}",
                                            (0 until layout.lineCount).all { layout.getEllipsisCount(it) == 0 })
                                        assertTrue("$name: vertically clipped ${child.text}",
                                            layout.height <= child.height - child.totalPaddingTop - child.totalPaddingBottom)
                                    }
                                }
                                if (child is ViewGroup) for (index in 0 until child.childCount)
                                    inspect(child.getChildAt(index), left, top)
                            }
                            inspect(view, 0, 0)
                            if ((sample?.registered ?: 0) > 0) {
                                assertTrue("$name: missing primary value: $labels", labels.any { text -> text.any(Char::isDigit) })
                                assertFalse(labels.contains(context.getString(R.string.stat_widget_loading)))
                            } else {
                                assertTrue("$name: missing state: $labels", labels.contains(context.getString(
                                    if (sample == null) R.string.stat_widget_short_loading else R.string.stat_widget_short_empty)))
                            }
                            if (!compact && locale == Locale.KOREAN && stateName == "normal") {
                                assertTrue(labels.contains(context.getString(title)))
                                assertTrue("Missing footer: $labels", labels.any { it.contains("통계 보기") })
                            }
                            // A small representative gallery; the entire matrix is still asserted above.
                            if (stateName == "normal" && locale == Locale.KOREAN && scale == 1f) {
                                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                view.draw(Canvas(bitmap))
                                val output = File(context.getExternalFilesDir(null), "widget-renders").apply { mkdirs() }
                                File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        }
    }
}
