package com.pnd.android.loop.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pnd.android.loop.data.history.CompletionCounts
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import com.pnd.android.loop.ui.theme.AppTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic gallery, drawn directly without Espresso's platform input API reflection. */
@RunWith(AndroidJUnit4::class)
class LoopTreeCardRenderingTest {
    @Test fun rendersGrowthWaterAndRewardGallery() {
        data class Sample(val name: String, val percent: Int?, val done: Int, val total: Int,
            val vitality: TreeVitality = TreeVitality(), val dark: Boolean = false, val fontScale: Float = 1f, val water: Boolean = false)
        val samples = (0..9).map { Sample("growth-${it + 1}", it * 10, 3, 5) } +
            (1..5).map { level ->
                val days = treeRewards[level - 1].days
                Sample("reward-$level", 84, 4, 5, TreeVitality(days, if (level >= 4) days else 0))
            } + listOf(Sample("no-history", null, 0, 0),
                Sample("dark-tree", 84, 4, 5, TreeVitality(100, 100), true),
                Sample("large-font-tree", 64, 3, 5, fontScale = 2f),
                Sample("empty-water", 64, 0, 5, water = true),
                Sample("partial-water", 64, 3, 5, water = true),
                Sample("full-water", 64, 5, 5, water = true),
                Sample("no-tasks-water", 64, 0, 0, water = true),
                Sample("dark-water", 64, 3, 5, dark = true, water = true),
                Sample("large-font-water", 64, 3, 5, fontScale = 2f, water = true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, ComponentActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComponentActivity
        val sample = mutableStateOf(samples.first())
        var bounds = Rect.Zero
        instrumentation.runOnMainSync {
            // The attached device may be dozing or locked during a long build.
            if (Build.VERSION.SDK_INT >= 27) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.setContent {
                val current = sample.value
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, current.fontScale)) {
                    AppTheme(darkTheme = current.dark) {
                        Box(Modifier.width(360.dp).padding(12.dp)) {
                            AppCard(Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
                                color = if (current.water) waterCardColor() else treeCardColor()) {
                                if (current.water) LoopWaterCard(LoopRates(CompletionCounts(
                                    done = current.done, unanswered = current.total - current.done)))
                                else LoopTreeCard(
                                    overallRates = if (current.percent == null) LoopRates.Empty else
                                        LoopRates(CompletionCounts(done = current.percent,
                                            unanswered = 100 - current.percent)),
                                    vitality = current.vitality, hasEstimatedHistory = false,
                                )
                            }
                        }
                    }
                }
            }
        }
        val directory = File(context.getExternalFilesDir(null), "tree-renders").apply { mkdirs() }
        try {
            samples.forEach { current ->
                instrumentation.runOnMainSync { sample.value = current }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(1300)
                instrumentation.runOnMainSync {
                    val decor = activity.window.decorView
                    assertTrue("${current.name}: preview lost window focus", activity.hasWindowFocus())
                    assertTrue("${current.name}: missing card", bounds.width > 0 && bounds.height > 0)
                    assertTrue("${current.name}: clipped card $bounds in ${decor.width}x${decor.height}",
                        bounds.left >= 0 && bounds.top >= 0 && bounds.right <= decor.width && bounds.bottom <= decor.height)
                    val bitmap = Bitmap.createBitmap(bounds.width.toInt(), bounds.height.toInt(), Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.translate(-bounds.left, -bounds.top)
                    decor.draw(canvas)
                    val colors = mutableSetOf<Int>()
                    for (y in 0 until bitmap.height step 24) {
                        for (x in 0 until bitmap.width step 24) colors.add(bitmap.getPixel(x, y))
                    }
                    assertTrue("${current.name}: empty capture", colors.size > 3)
                    File(directory, "${current.name}.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
