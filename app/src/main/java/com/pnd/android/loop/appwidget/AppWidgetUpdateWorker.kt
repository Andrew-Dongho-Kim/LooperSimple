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
import com.pnd.android.loop.alarm.notification.LoopForegroundService
import com.pnd.android.loop.alarm.notification.cancelLoopPrompts
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.data.TodayLoopOrder
import com.pnd.android.loop.data.actualEndInDay
import com.pnd.android.loop.data.actualStartInDay
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.data.putTo
import com.pnd.android.loop.util.currentOccurrence
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.occurrenceStartDate
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toTimeTextForLog
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
        var loopStateChanged = false
        when {
            has(DONE_LOOP) -> { done(); loopStateChanged = true }
            has(SKIP_LOOP) -> { skip(); loopStateChanged = true }
            has(START_LOOP) -> { start(); loopStateChanged = true }
            has(STOP_LOOP) -> { stop(); loopStateChanged = true }
            else -> {
                // do nothing
            }
        }

        refresh()

        // 위젯에서 루프를 시작/정지(완료/스킵)했을 때도 상시 알림을 즉시 동기화한다.
        //  - 시작(IN_PROGRESS)하면 곧바로 알림에 등록되고,
        //  - 정지/완료/스킵하면 진행 중인 루프가 없어질 경우 서비스가 스스로 알림을 내린다.
        if (loopStateChanged) {
            LoopForegroundService.refresh(context)
            // 이미 답한 루프에 질문("완료했나요?", "시작할까요?")이 남지 않게 한다. 위젯에서
            // 답한 경우까지 여기서 함께 처리된다(알림 액션도 이 워커를 거친다).
            cancelLoopPrompts(context = context, loopId = loopId())
        }
        return Result.success()
    }

    private fun has(@Action action: String) = params.inputData.getString(PARAMS_ACTION) == action
    private fun loopId() = params.inputData.getInt(PARAMS_LOOP_ID, -1)

    /** 호출자가 지정한 기록 날짜([PARAMS_DATE]). 지정하지 않았으면 null(=지금 기준으로 정한다). */
    private fun requestedDate(): LocalDate? =
        params.inputData.getLong(PARAMS_DATE, 0L).takeIf { it > 0L }?.toLocalDate()

    private suspend fun done() {
        val loopId = loopId()
        val loop = loopDao.getLoop(loopId) ?: return
        val date = requestedDate() ?: responseDate(loop)
        loopDoneDao.addOrUpdate(
            LoopDoneVo(
                loopId = loopId,
                date = date.toMs(),
                startInDay = loop.startInDay,
                endInDay = loop.endInDay,
                done = DoneState.DONE
            )
        )
        logger.i { "done: $loopId on $date" }
    }

    private suspend fun skip() {
        val loopId = loopId()
        val loop = loopDao.getLoop(loopId) ?: return
        val date = requestedDate() ?: responseDate(loop)
        loopDoneDao.addOrUpdate(
            LoopDoneVo(
                loopId = loopId,
                date = date.toMs(),
                startInDay = loop.startInDay,
                endInDay = loop.endInDay,
                done = DoneState.SKIP
            )
        )
        logger.i { "skip: $loopId on $date" }
    }

    /**
     * 응답(완료/건너뛰기/정지)을 기록할 occurrence 의 날짜.
     *
     * done 기록은 루프가 "시작한 날"에 저장된다. 자정을 넘겨 이어지는 루프를 그냥 오늘로
     * 기록하면 어제 시작한 occurrence 는 영원히 미응답으로 남고(알림·위젯에서 사라지지 않고
     * 통계도 어긋난다) 오늘 몫이 잘못 완료 처리된다.
     *  - 시간제: 자정을 넘긴 구간이면 어제([occurrenceStartDate])
     *  - anytime: 진행 중(IN_PROGRESS) 기록이 있는 날. 오늘 → 어제 순으로 찾는다.
     *
     * 어느 쪽이든 "지금이 그 occurrence 안"이라는 전제가 깔려 있다. 이미 끝난 루프에 답하는
     * 경우(종료 확인 알림)에는 이 판정이 맞지 않으므로, 호출자가 [PARAMS_DATE] 로 날짜를 지정한다.
     */
    private suspend fun responseDate(loop: LoopBase): LocalDate {
        val today = LocalDate.now()
        if (!loop.isAnyTime) return loop.occurrenceStartDate()

        if (isInProgressAt(loop.loopId, today)) return today
        val yesterday = today.minusDays(1)
        return if (isInProgressAt(loop.loopId, yesterday)) yesterday else today
    }

    private suspend fun isInProgressAt(loopId: Int, date: LocalDate) =
        loopDoneDao.getDoneState(loopId = loopId, date = date.toMs())?.done == DoneState.IN_PROGRESS

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
        logger.i { "start: $loopId" }
    }

    private suspend fun stop() {
        val loopId = loopId()
        val loop = loopDao.getLoop(loopId) ?: return
        val endAt = LocalTime.now().toMs()
        // 어제 시작해 자정을 넘긴 anytime 루프는 어제 행에 IN_PROGRESS 로 남아 있다.
        // 오늘 행에 기록하면 어제 행이 계속 진행 중으로 남아 알림이 사라지지 않는다.
        val date = responseDate(loop).toMs()

        val doneVo = loopDoneDao.getDoneState(
            loopId = loopId,
            date = date
        )

        val startAt = doneVo?.startInDay ?: 0
        loopDoneDao.addOrUpdate(
            LoopDoneVo(
                loopId = loopId,
                date = date,
                startInDay = startAt,
                endInDay = endAt,
                done = DoneState.DONE
            )
        )
        // done 기록이 없으면 startAt 이 음수일 수 있다. 로그에는 시각 변환이 안전한 쪽을 쓴다.
        logger.i {
            "stop: $loopId, startAt: ${startAt.toTimeTextForLog()}, " +
                    "endAt: ${endAt.toTimeTextForLog()}"
        }
    }

    private suspend fun refresh() {
        val today = LocalDate.now()
        // 자정을 넘겨 이어지는 루프의 done 기록은 어제 행에 있다. 오늘 행만 보면 이미 완료한
        // 루프가 계속 미응답으로 남아 위젯에서 사라지지 않는다.
        val yesterdayLoops = fullLoopDao.getAllEnabledLoops(date = today.minusDays(1).toMs())
            .associateBy { it.loopId }
        val loops = fullLoopDao.getAllEnabledLoops(date = today.toMs())
            .map { loop -> currentOccurrence(today = loop, yesterday = yesterdayLoops[loop.loopId]) }
        updateWidget(
            context = context,
            loops = loops.filter { loop ->
                // 오늘 활성이거나, 자정을 넘겨 지금도 진행 중인(어제 시작한) 루프. 그중 미응답·진행 중만.
                (loop.isActiveDay() || loop.isActive()) && !loop.isRespond && !loop.isDisabled
            }.sortedWith(TodayLoopOrder())
                .map { loop -> loop.toWidgetLoop() }
        )
    }

    /**
     * 위젯으로 전달되는 루프는 putTo/asLoop 를 거치며 done 상태·실제 시작/종료 시각을 잃고
     * 순수 LoopVo 로 재구성된다. anytime 루프는 base start/end 가 항상 ANY_TIME(-1)이라,
     * 이대로면 위젯이 진행 여부를 알 수 없어 늘 "시작" 버튼만 노출된다.
     * 그래서 anytime 루프에 한해 실제 시작/종료 시각(done 기록)을 start/end 로 옮겨 실어,
     * 위젯이 시작/정지 버튼과 "started at" 라벨을 올바로 표시하게 한다.
     */
    private fun LoopBase.toWidgetLoop(): LoopBase =
        if (isAnyTime) asLoopVo(startInDay = actualStartInDay, endInDay = actualEndInDay) else this

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
        logger.i { "updateWidget:$jsonArray" }

        GlanceAppWidgetManager(context).getGlanceIds(AppWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                glanceId = glanceId
            ) { prefs ->
                // This is hack to force update widget
                val revision = prefs[KEY_REVISION]?.let { it + 1 } ?: 0
                prefs[KEY_REVISION] = revision
                prefs[KEY_LOOPS_JSON] = "{\"loops\": $jsonArray, \"total\":${loops.size}}"

                logger.i { "updateWidget[$glanceId] revision:$revision" }
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

        /**
         * @param date 응답을 기록할 occurrence 의 날짜(에폭 ms). 0 이면 지금 기준으로 정한다([PARAMS_DATE]).
         */
        fun actionLoop(
            context: Context,
            @Action action: String,
            loopId: Int,
            date: Long = 0L,
        ) = enqueueWork(
            context = context,
            inputData = Data.Builder()
                .putBoolean(PARAMS_UPDATE_LOOPS, true)
                .putString(PARAMS_ACTION, action)
                .putInt(PARAMS_LOOP_ID, loopId)
                .putLong(PARAMS_DATE, date)
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
