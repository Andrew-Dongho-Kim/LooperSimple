package com.pnd.android.loop.ui.detail

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.ui.statisctics.StreakStat
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTheme
import com.pnd.android.loop.ui.theme.background
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic screens in a separate application ID; no database or real loop is opened. */
@RunWith(AndroidJUnit4::class)
class DetailTimelineRenderingTest {
    @Test fun rendersWeekMonthDarkLargeFontAndOvernightStates() {
        data class Sample(val name: String, val dark: Boolean = false, val scale: Float = 1f,
            val monthly: Boolean = false, val overnight: Boolean = false, val empty: Boolean = false)
        val samples = listOf(Sample("week"), Sample("month", monthly = true),
            Sample("dark", dark = true), Sample("large-font", scale = 2f),
            Sample("overnight", overnight = true), Sample("empty", empty = true))
        val today = LocalDate.of(2026, 10, 8)
        val responses = (1..8).associate { day ->
            val date = today.withDayOfMonth(day)
            date to LoopDoneVo(1, 0, 27_000_000, 29_700_000, DoneState.DONE,
                timeSource = LoopDoneVo.TimeSource.USER_ENTERED, localEpochDay = date.toEpochDay())
        }
        val stats = DetailStats.empty(today).copy(
            createdDate = today.minusMonths(2),
            doneStateByDate = responses.mapValues { it.value.done },
            memoDates = setOf(today), streak = StreakStat(3, 8),
            weekly = WeeklyProgress(3, 5, true, 1),
        )
        val loop = LoopVo.anytime(title = "매일 책 읽기", color = 0xff6576b8.toInt())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, ComponentActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComponentActivity
        val sample = mutableStateOf(samples.first())
        val scroll = androidx.compose.foundation.ScrollState(0)
        instrumentation.runOnMainSync {
            if (Build.VERSION.SDK_INT >= 27) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.setContent {
                val current = sample.value
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, current.scale)) {
                    AppTheme(darkTheme = current.dark) {
                        key(current.name) {
                            Column(Modifier.fillMaxSize().background(AppColor.background).systemBarsPadding()
                                .verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                TimelineSummaryHeader(loop, stats, responses)
                                JournalCalendarCard(stats, Color(0xff6576b8), today, today,
                                    current.monthly, {}, {}, {}, {})
                                val response = when {
                                    current.empty -> null
                                    current.overnight -> responses[today]!!.copy(startInDay = 84_600_000, endInDay = 2_700_000)
                                    else -> responses[today]
                                }
                                RecordedDayCard(today, response, response?.done, Color(0xff6576b8))
                                JournalMemoCard(today,
                                    if (current.empty) "" else "오늘은 집중이 잘 됐다.\n아침에는 3장을 읽고, 마음에 남은 문장을 다시 정리했다.", {})
                            }
                        }
                    }
                }
            }
        }
        val directory = File(context.getExternalFilesDir(null), "detail-renders").apply { mkdirs() }
        fun capture(name: String) {
            instrumentation.waitForIdleSync()
            SystemClock.sleep(400)
            instrumentation.runOnMainSync {
                val view = activity.window.decorView
                assertTrue("Missing preview", view.width > 0 && view.height > 0)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val colors = mutableSetOf<Int>()
                for (y in 0 until bitmap.height step 20) for (x in 0 until bitmap.width step 20) colors.add(bitmap.getPixel(x, y))
                assertTrue("Blank screen: $name", colors.size > 5)
                File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
        try {
            samples.forEach { current ->
                instrumentation.runOnMainSync { sample.value = current }
                instrumentation.waitForIdleSync()
                runBlocking { scroll.scrollTo(0) }
                capture("${current.name}-top")
                runBlocking { scroll.scrollTo(scroll.maxValue) }
                capture("${current.name}-bottom")
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
