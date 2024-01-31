package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pnd.android.loop.appwidget.AppWidget.Companion.KEY_LOOPS_JSON
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.putTo
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toMs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

@HiltWorker
class AppWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    appDb: AppDatabase
) : CoroutineWorker(
    appContext = context,
    params = params
) {

    private val logger = Logger("AppWidgetUpdateWorker")

    private val loopWithDoneDao = appDb.loopWithDoneDao()
    override suspend fun doWork(): Result {
        val loops = loopWithDoneDao.allLoops(date = LocalDate.now().toMs())
        updateWidget(
            context = context,
            loops = loops.filter { loop -> loop.isActiveDay() }
        )
        return Result.success()
    }

    private suspend fun updateWidget(
        context: Context,
        loops: List<LoopBase>
    ) {
        GlanceAppWidgetManager(context).getGlanceIds(AppWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                glanceId = glanceId
            ) { prefs ->

                val jsonArray = JSONArray()
                loops.forEach { loop ->
                    val map = mutableMapOf<String, Any?>()
                    loop.putTo(map)

                    jsonArray.put(JSONObject(map))
                }
                prefs[KEY_LOOPS_JSON] = "$jsonArray"
                logger.d { "updateWidget:$jsonArray" }
            }
        }
    }
}