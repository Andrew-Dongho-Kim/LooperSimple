package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.fasterxml.jackson.databind.ObjectMapper
import com.pnd.android.loop.appwidget.ui.LoopWidgetMedium
import com.pnd.android.loop.appwidget.ui.LoopWidgetSmall
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
            SIZE_SMALL,
            SIZE_MEDIUM,
            SIZE_LARGE
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
        val size = LocalSize.current
        logger.d { "AppWidget updated revision:$revision, widgetSize:$size" }

        if (!jsonLoops.isNullOrEmpty()) {
            val loops = appWidgetData.loops.map { it.asLoop() }

            when (size) {
                SIZE_SMALL -> LoopWidgetSmall(
                    loops = loops,
                    todayTotal = appWidgetData.total,
                )

                else -> LoopWidgetMedium(
                    loops = loops,
                    todayTotal = appWidgetData.total
                )
            }
        }
    }

    class AppWidgetData {
        lateinit var loops: List<Map<String, Any>>
        var total: Int = 0

        companion object {
            val EMPTY = AppWidgetData().apply {
                loops = emptyList()
                total = 0
            }
        }
    }

    companion object {
        private val SIZE_SMALL = DpSize(54.dp, 73.dp)
        private val SIZE_MEDIUM = DpSize(110.dp, 148.dp)
        private val SIZE_LARGE = DpSize(500.dp, 600.dp)

        val KEY_LOOPS_JSON = stringPreferencesKey("key_loops_json")
        val KEY_REVISION = longPreferencesKey("key_revision")
    }
}