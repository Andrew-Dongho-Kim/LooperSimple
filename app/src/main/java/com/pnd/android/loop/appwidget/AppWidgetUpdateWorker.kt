package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.annotation.StringDef
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
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
import com.pnd.android.loop.util.occurrenceStartDate
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toTimeTextForLog
import com.pnd.android.loop.util.toMs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 루프의 상태를 바꾸고(완료·건너뛰기·시작·정지) 위젯을 다시 그리는 일을 백그라운드로 옮기는 워커.
 *
 * 위젯·알림의 버튼, 시작/종료 알람, 앱에서의 편집이 모두 이 워커를 거친다. 실제로 위젯을 그리는
 * 일은 [AppWidgetRefresher] 가 맡는다(상시 알림 서비스도 같은 것을 쓴다).
 */
@HiltWorker
class AppWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    appDb: AppDatabase,
    private val appWidgetRefresher: AppWidgetRefresher,
) : CoroutineWorker(
    appContext = context,
    params = params
) {

    private val logger = Logger("AppWidgetUpdateWorker")

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

        appWidgetRefresher.refresh()

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

    companion object {
        /** 모든 위젯 갱신 요청을 한 줄로 세우는 이름([enqueueWork] 참고). */
        private const val UNIQUE_WORK_NAME = "app_widget_update"

        /** 시간이 흐른 것만으로 낡는 문구를 되살리는 정기 갱신의 이름([schedulePeriodicUpdate]). */
        private const val PERIODIC_WORK_NAME = "app_widget_periodic_update"

        /**
         * 정기 갱신 주기. WorkManager 가 허용하는 최소 주기가 15분이라 그보다 촘촘하게는 둘 수 없다.
         * 진행 중인 루프가 있는 동안에는 상시 알림 서비스가 1분마다 직접 다시 그리므로
         * ([AppWidgetRefresher]), 이 주기가 실제로 맡는 몫은 "2시간 후 시작"처럼 진행 중이 아닐 때의
         * 문구다.
         */
        private const val PERIODIC_UPDATE_INTERVAL_MINUTES = 15L

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
         * 시간이 흐르는 것만으로 위젯이 낡는 것을 막는 정기 갱신을 걸어 둔다.
         *
         * 위젯 문구의 상당수는 지금 시각으로 계산된다("32분 남음", "2시간 후 시작", 진행 중 여부).
         * 갱신 트리거가 루프의 시작/종료 알람뿐이면 그 사이에는 처음 그린 문구가 그대로 남는다.
         *
         * 이미 걸려 있으면 그대로 둔다(KEEP). 위젯을 놓을 때마다 불려도 주기가 새로 시작되지 않는다.
         */
        fun schedulePeriodicUpdate(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppWidgetUpdateWorker>(
                PERIODIC_UPDATE_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** 마지막 위젯이 사라졌을 때 정기 갱신을 거둔다([AppWidgetReceiver]). */
        fun cancelPeriodicUpdate(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
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

            // 갱신 요청은 여러 곳(위젯·알림 버튼, 시작/종료 알람, 앱에서의 편집)에서 거의 동시에
            // 들어온다. 이름 하나로 묶어 들어온 순서대로 하나씩만 돌게 해, 두 워커가 나란히 돌며
            // 서로의 기록을 앞지르지 않게 한다.
            // (위젯을 그리는 구간 자체가 겹치는 것은 [AppWidgetRefresher] 가 따로 막는다.
            //  정기 갱신은 다른 이름으로 들어와 이 줄에 서지 않기 때문이다.)
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }
    }
}
