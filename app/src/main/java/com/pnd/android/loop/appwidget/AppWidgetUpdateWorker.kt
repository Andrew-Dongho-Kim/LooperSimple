package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.annotation.StringDef
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
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action.Companion.DONE_LOOP
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action.Companion.DO_NOTHING
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action.Companion.SKIP_LOOP
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action.Companion.START_LOOP
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action.Companion.STOP_LOOP
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.data.TodayLoopOrder
import com.pnd.android.loop.data.isInProgressState
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.data.putTo
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

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
        when {
            has(DONE_LOOP) -> done()
            has(SKIP_LOOP) -> skip()
            has(START_LOOP) -> start()
            has(STOP_LOOP) -> stop()
            else -> {
                // do nothing
            }
        }

        refresh()
        return Result.success()
    }

    private fun has(@Action action: String) = params.inputData.getString(PARAMS_ACTION) == action
    private fun loopId() = params.inputData.getInt(PARAMS_LOOP_ID, -1)

    private suspend fun done() {
        val loopId = loopId()
        val loop = loopDao.getLoop(loopId)
        loopDoneDao.addOrUpdate(
            LoopDoneVo(
                loopId = loopId,
                date = LocalDate.now().toMs(),
                startInDay = loop.startInDay,
                endInDay = loop.endInDay,
                done = DoneState.DONE
            )
        )
        logger.d { "done: $loopId" }
    }

    private suspend fun skip() {
        val loopId = loopId()
        val loop = loopDao.getLoop(loopId)
        loopDoneDao.addOrUpdate(
            LoopDoneVo(
                loopId = loopId,
                date = LocalDate.now().toMs(),
                startInDay = loop.startInDay,
                endInDay = loop.endInDay,
                done = DoneState.SKIP
            )
        )
        logger.d { "skip: $loopId" }
    }

    private suspend fun start() {
        val loopId = loopId()
        loopDoneDao.addOrUpdate(
            LoopDoneVo(
                loopId = loopId,
                date = LocalDate.now().toMs(),
                startInDay = LocalTime.now().toMs(),
                endInDay = ANY_TIME,
                done = DoneState.IN_PROGRESS
            )
        )
        logger.d { "start: $loopId" }
    }

    private suspend fun stop() {
        val loopId = loopId()
        val endAt = LocalTime.now().toMs()
        val today = LocalDate.now().toMs()

        val doneVo = loopDoneDao.getDoneState(
            loopId = loopId,
            date = today
        )

        val startAt = doneVo?.startInDay ?: 0
        loopDoneDao.addOrUpdate(
            LoopDoneVo(
                loopId = loopId,
                date = today,
                startInDay = startAt,
                endInDay = endAt,
                done = DoneState.DONE
            )
        )
        logger.d { "stop: $loopId, statAt: ${startAt.toLocalTime()}, endAt:${endAt.toLocalTime()}" }
    }

    private suspend fun refresh() {
        val loops = fullLoopDao.getAllEnabledLoops(date = LocalDate.now().toMs())
        updateWidget(
            context = context,
            loops = loops.filter { loop ->
                loop.isActiveDay() && (loop.isNotRespond || loop.isInProgressState)
            }.sortedWith(TodayLoopOrder())
        )
    }

    private suspend fun updateWidget(
        context: Context,
        loops: List<LoopBase>
    ) {
        val jsonArray = JSONArray()
        loops.forEach { loop ->
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
        @StringDef(DO_NOTHING, DONE_LOOP, SKIP_LOOP, START_LOOP, STOP_LOOP)
        annotation class Action {
            companion object {
                const val DO_NOTHING = "do_nothing"
                const val DONE_LOOP = "done_loop"
                const val SKIP_LOOP = "skip_loop"
                const val START_LOOP = "start_loop"
                const val STOP_LOOP = "stop_loop"
            }
        }

        fun updateWidget(context: Context) {
            enqueueWork(
                context = context,
                inputData = Data.Builder()
                    .putBoolean(PARAMS_UPDATE_LOOPS, true)
                    .build()
            )
        }

        fun actionLoop(
            context: Context,
            @Action action: String,
            loopId: Int
        ) = enqueueWork(
            context = context,
            inputData = Data.Builder()
                .putBoolean(PARAMS_UPDATE_LOOPS, true)
                .putString(PARAMS_ACTION, action)
                .putInt(PARAMS_LOOP_ID, loopId)
                .build()
        )

        private fun enqueueWork(context: Context, inputData: Data) {
            val request = OneTimeWorkRequestBuilder<AppWidgetUpdateWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
