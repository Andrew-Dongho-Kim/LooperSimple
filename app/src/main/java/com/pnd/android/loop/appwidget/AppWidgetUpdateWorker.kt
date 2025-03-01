package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pnd.android.loop.appwidget.AppWidget.Companion.KEY_LOOPS_JSON
import com.pnd.android.loop.appwidget.AppWidget.Companion.KEY_REVISION
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.TodayLoopOrder
import com.pnd.android.loop.data.isNotRespond
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

    private val fullLoopDao = appDb.fullLoopDao()
    private val loopDao = appDb.loopDao()
    private val loopDoneDao = appDb.loopDoneDao()

    override suspend fun doWork(): Result {
        val doneLoopId = params.inputData.getInt(DONE_LOOP, -1)
        if (doneLoopId != -1) {
            val loop = loopDao.getLoop(doneLoopId)
            loopDoneDao.addOrUpdate(
                LoopDoneVo(
                    loopId = doneLoopId,
                    date = LocalDate.now().toMs(),
                    startInDay = loop.startInDay,
                    endInDay = loop.endInDay,
                    done = DoneState.DONE
                )
            )
        }

        val skipLoopId = params.inputData.getInt(SKIP_LOOP, -1)
        if (skipLoopId != -1) {
            val loop = loopDao.getLoop(skipLoopId)
            loopDoneDao.addOrUpdate(
                LoopDoneVo(
                    loopId = skipLoopId,
                    date = LocalDate.now().toMs(),
                    startInDay = loop.startInDay,
                    endInDay = loop.endInDay,
                    done = DoneState.SKIP
                )
            )
        }

        val loops = fullLoopDao.getAllEnabledLoops(date = LocalDate.now().toMs())
        updateWidget(
            context = context,
            loops = loops.filter { loop ->
                loop.isActiveDay()
            }.sortedWith(TodayLoopOrder())
        )

        return Result.success()
    }

    private suspend fun updateWidget(
        context: Context,
        loops: List<LoopBase>
    ) {
        val jsonArray = JSONArray()
        loops.filter { loop -> loop.isNotRespond }.forEach { loop ->
            val map = mutableMapOf<String, Any?>()
            loop.putTo(map)

            jsonArray.put(JSONObject(map))
        }
        logger.d { "updateWidget:$jsonArray" }

        GlanceAppWidgetManager(context).getGlanceIds(AppWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                glanceId = glanceId
            ) { prefs ->
                // This is hack to force update widget
                val revision = prefs[KEY_REVISION]?.let { it + 1 } ?: 0
                prefs[KEY_REVISION] = revision
                prefs[KEY_LOOPS_JSON] = "{\"loops\": $jsonArray, \"total\":${loops.size}}"

                logger.d { "updateWidget[$glanceId] revision:$revision" }
            }
        }
        AppWidget().updateAll(context)
    }

    companion object {
        private const val UPDATE_LOOPS = "params_update_loops"
        private const val DONE_LOOP = "done_loop"
        private const val SKIP_LOOP = "skip_loop"

        fun updateWidget(context: Context) {
            enqueueWork(
                context = context,
                inputData = Data.Builder()
                    .putBoolean(UPDATE_LOOPS, true)
                    .build()
            )
        }

        fun doneLoop(context: Context, loopId: Int) {
            enqueueWork(
                context = context,
                inputData = Data.Builder()
                    .putBoolean(UPDATE_LOOPS, true)
                    .putInt(DONE_LOOP, loopId)
                    .build()
            )
        }

        fun skipLoop(context: Context, loopId: Int) {
            enqueueWork(
                context = context,
                inputData = Data.Builder()
                    .putBoolean(UPDATE_LOOPS, true)
                    .putInt(SKIP_LOOP, loopId)
                    .build()
            )
        }

        private fun enqueueWork(context: Context, inputData: Data) {
            val request = OneTimeWorkRequestBuilder<AppWidgetUpdateWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
