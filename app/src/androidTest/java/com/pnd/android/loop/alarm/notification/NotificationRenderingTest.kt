package com.pnd.android.loop.alarm.notification

import android.app.NotificationManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.toMs
import java.io.File
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Inflates real RemoteViews, with synthetic loops; never posts alerts or accesses loop records. */
@RunWith(AndroidJUnit4::class)
class NotificationRenderingTest {
    @Test fun customViewsFitNotificationBudgets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val now = LocalTime.now()
        val timed = LoopVo.create(
            id = 91001, title = "집중해서 읽고 정리하는 시간 · A long loop title",
            color = Color.rgb(65, 104, 200),
            startInDay = now.minusMinutes(20).toMs(), endInDay = now.plusMinutes(25).toMs(),
        )
        val anytime = LoopVo.anytime(id = 91002, title = "가볍게 스트레칭", color = Color.rgb(58, 146, 123))
        val scenarios = mapOf(
            "single" to listOf(timed),
            "anytime" to listOf(anytime),
            "mixed" to listOf(timed, anytime, timed.copy(loopId = 91003, title = "산책", color = Color.rgb(186, 135, 58))),
            "overflow" to (0..4).map { timed.copy(loopId = 91100 + it, title = "Loop $it") },
        )
        for (locale in listOf(Locale.KOREAN, Locale.ENGLISH)) {
            for (night in listOf(false, true)) for (scale in listOf(1f, 1.3f, 2f)) {
                val config = Configuration(base.resources.configuration).apply {
                    setLocale(locale)
                    fontScale = scale
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config),
                    if (night) android.R.style.Theme_DeviceDefault else android.R.style.Theme_DeviceDefault_Light)
                val helper = NotificationHelper(context, context.getSystemService(NotificationManager::class.java))
                for ((scenario, loops) in scenarios) {
                    val notification = helper.buildOngoingNotification(loops)
                    assertEquals(if (loops.size == 1) if (loops[0].isAnyTime) 1 else 2 else 0,
                        notification.actions?.size ?: 0)
                    for ((kind, remote) in listOf("collapsed" to notification.contentView, "expanded" to notification.bigContentView)) {
                        for (widthDp in listOf(260, 340)) instrumentation.runOnMainSync {
                            val density = context.resources.displayMetrics.density
                            val width = (widthDp * density).toInt()
                            val parent = FrameLayout(context)
                            val view = remote.apply(context, parent)
                            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                            view.layout(0, 0, width, view.measuredHeight)
                            view.viewTreeObserver.dispatchOnPreDraw()
                            view.jumpDrawablesToCurrentState()
                            val name = "$scenario-$kind-${locale.language}-$night-$scale-$widthDp"
                            val limit = if (kind == "collapsed") 64 else 252
                            assertTrue("$name height ${view.height / density} exceeds $limit dp", view.height <= limit * density)
                            fun inspect(child: View) {
                                if (child.visibility != View.VISIBLE) return
                                if (child is TextView && child.text.isNotBlank()) {
                                    val owner = child.parent as View
                                    assertTrue("$name out of bounds: ${child.text} (${child.left},${child.top},${child.right},${child.bottom}) in ${owner.width}x${owner.height}",
                                        child.left >= 0 && child.top >= 0 && child.right <= owner.width && child.bottom <= owner.height)
                                    assertTrue("$name zero width: ${child.text}", child.width > 0)
                                    assertTrue("$name clipped text height: ${child.text}",
                                        child.layout.height <= child.height - child.totalPaddingTop - child.totalPaddingBottom)
                                }
                                if (child is ViewGroup) for (i in 0 until child.childCount) inspect(child.getChildAt(i))
                            }
                            inspect(view)
                            if (kind == "expanded" && loops.size > 1) {
                                val action = view.findViewById<TextView>(R.id.loop_row_action)
                                assertEquals(View.VISIBLE, action.visibility)
                                assertTrue(action.text.isNotBlank())
                                assertTrue(action.isClickable)
                                assertTrue(action.width >= 48 * density && action.height >= 48 * density)
                                assertTrue(action.contentDescription.contains(loops[0].title))
                            }
                            if (locale == Locale.KOREAN && scale == 1f && widthDp == 340) {
                                val bitmap = Bitmap.createBitmap(width, view.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(if (night) Color.rgb(37, 39, 45) else Color.rgb(248, 249, 252))
                                view.draw(canvas)
                                val dir = File(context.getExternalFilesDir(null), "notification-renders").apply { mkdirs() }
                                File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        }
    }
}