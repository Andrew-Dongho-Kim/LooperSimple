package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import com.fasterxml.jackson.databind.ObjectMapper
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.ui.LoopWidget50By50
import com.pnd.android.loop.appwidget.ui.stringResourceGlide
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.asLoop

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

        val revision = currentState(KEY_REVISION)
        logger.d { "AppWidget updated revision:$revision" }
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
                provider = ImageProvider(R.drawable.refresh),
                contentDescription = stringResourceGlide(resId = R.string.refresh)
            )
        }
    }


    class AppWidgetData {
        lateinit var list: List<Map<String, Any>>

        companion object {
            val EMPTY = AppWidgetData().apply {
                list = emptyList()
            }
        }
    }

    companion object {
        private val SIZE_50_50 = DpSize(50.dp, 50.dp)
        private val SIZE_50_100 = DpSize(50.dp, 100.dp)
        private val SIZE_100_200 = DpSize(100.dp, 200.dp)

        val KEY_LOOPS_JSON = stringPreferencesKey("key_loops_json")
        val KEY_REVISION = longPreferencesKey("key_revision")
    }
}