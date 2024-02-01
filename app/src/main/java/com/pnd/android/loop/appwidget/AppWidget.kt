package com.pnd.android.loop.appwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.pnd.android.loop.R
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoop
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.isPast

private val logger = Logger("AppWidget")

class AppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ResponsiveLoopWidget()
        }
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SIZE_50_50,
            SIZE_50_100,
            SIZE_100_200
        )
    )

    @Composable
    private fun ResponsiveLoopWidget() {
        val jsonLoops = currentState(KEY_LOOPS_JSON)
        val appWidgetData = try {
            ObjectMapper().readValue(
                jsonLoops,
                AppWidgetData::class.java
            )
        } catch (e: Exception) {
            logger.e { "Parse failed:$jsonLoops, exception:$e" }
            AppWidgetData.EMPTY
        }
        if (jsonLoops.isNullOrEmpty()) {
            RefreshLayout()
        } else {
            val loops = appWidgetData.list.map { it.asLoop() }
            LoopWidget50By50(
                loops = loops
            )
        }
    }

    @Composable
    private fun RefreshLayout(
        modifier: GlanceModifier = GlanceModifier,
    ) {
        Box(modifier = modifier.appWidgetBackground()) {
            Image(
                modifier = GlanceModifier
                    .clickable(onClick = actionRunCallback<AppWidgetRefreshAction>())
                    .padding(12.dp),
                provider = ImageProvider(vectorToBitmap(R.drawable.refresh)),
                contentDescription = stringResourceGlide(resId = R.string.refresh)
            )
        }
    }

    @Composable
    private fun LoopWidget50By50(
        modifier: GlanceModifier = GlanceModifier,
        loops: List<LoopBase>
    ) {
        if (loops.isEmpty()) {
            Text(
                modifier = modifier,
                text = stringResourceGlide(resId = R.string.desc_no_loops)
            )
        } else {
            val loop = pickOneLoop(loops)
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(AppColor.surface),

                ) {
                Text(
                    modifier = GlanceModifier.padding(
                        top = 12.dp,
                        start = 4.dp,
                        end = 4.dp
                    ),
                    text = loop.title
                )
                Text(
                    modifier = GlanceModifier.padding(
                        top = 4.dp,
                        start = 4.dp,
                        end = 4.dp
                    ),
                    text = " ~ ${loop.loopEnd.formatHourMinute(context = LocalContext.current)}",
                    style = TextStyle(color = ColorProvider(AppColor.primary))
                )

                if (loop.isPast()) {
                    Row(
                        modifier = GlanceModifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Image(
                            modifier = GlanceModifier.padding(start = 4.dp),
                            provider = ImageProvider(vectorToBitmap(resId = R.drawable.done)),
                            contentDescription = ""
                        )
                        Image(
                            modifier = GlanceModifier.padding(start = 12.dp),
                            provider = ImageProvider(vectorToBitmap(resId = R.drawable.skip)),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }

    private fun pickOneLoop(loops: List<LoopBase>): LoopBase {
        return loops.minBy { it.loopEnd }
    }

    @Composable
    private fun stringResourceGlide(resId: Int) =
        LocalContext.current.getString(resId)


    @Composable
    fun vectorToBitmap(@DrawableRes resId: Int): Bitmap {
        val drawable = AppCompatResources.getDrawable(LocalContext.current, resId)
        val b = Bitmap.createBitmap(
            drawable!!.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(b)
        drawable.setBounds(0, 0, c.width, c.height)
        drawable.draw(c)
        return b
    }

    class AppWidgetData {
        lateinit var list: List<Map<String, Any>>

        companion object {
            val EMPTY = AppWidgetData().apply { list = emptyList() }
        }
    }

    companion object {
        private val SIZE_50_50 = DpSize(50.dp, 50.dp)
        private val SIZE_50_100 = DpSize(50.dp, 100.dp)
        private val SIZE_100_200 = DpSize(100.dp, 200.dp)

        val KEY_LOOPS_JSON = stringPreferencesKey("key_loops_json")
    }
}