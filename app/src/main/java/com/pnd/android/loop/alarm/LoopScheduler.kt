package com.pnd.android.loop.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.pnd.android.loop.alarm.notification.NotificationHelper
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopVo.Factory.MIDNIGHT_RESERVATION_ID
import com.pnd.android.loop.data.asLoop
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.common.NO_REPEAT
import com.pnd.android.loop.data.description
import com.pnd.android.loop.data.putTo
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.MS_1SEC
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.dh2m2
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.isActiveTime
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject


class LoopScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    appDb: AppDatabase
) {
    private val logger = log("LoopScheduler")

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val loopDao = appDb.loopDao()
    private val loopDoneDao = appDb.loopDoneDao()

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    fun reserveAlarm(
        loopSchedule: LoopSchedule
    ) {
        val (action, after, loop) = loopSchedule
        if (after <= 0) return


        val systemElapsed = SystemClock.elapsedRealtime()
        val reservedTime = systemElapsed + after
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            loop.putTo(this)
            putExtra(EXTRA_RESERVED_TIME, reservedTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            loop.loopId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (canScheduleExactAlarms()) {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                reservedTime,
                pendingIntent
            )
            logger.d {
                " - repeat after:${dh2m2(after)} ${loop.description(context)}"
            }

        } else {

            logger.e { "can't schedule exact alarm" }
        }
    }

    fun syncLoops() {
        logger.d { "start sync" }
        coroutineScope.launch {
            loopDao.getAllLoops().forEach { loop ->
                fillNoResponse(loop)
                if (loop.enabled) {
                    reserveAlarm(scheduleStart(loop))

                    // 동기화 시점(앱 시작 등)에 이미 진행 중인 루프는 시작 알람이 지나갔으므로,
                    // 곧바로 진행 틱을 걸어 남은 시간 알림이 다시 표시되도록 한다.
                    if (loop.hasTimeWindow && loop.isActiveDay() && loop.isActiveTime()) {
                        reserveAlarm(scheduleProgressTick(loop, after = MS_1SEC))
                    }
                } else {
                    cancelAlarm(loop)
                }
            }
            reserveAlarm(scheduleSync())
        }
    }

    private suspend fun fillNoResponse(loop: LoopBase) {
        val created = loop.created.toLocalDate()

        val now = LocalDate.now()
        var date = if (created == now) now else now.minusDays(1L)

        while (date.isBefore(now) || date.isEqual(now)) {
            if (!loop.isActiveDay(date)) {
                date = date.plusDays(1)
                continue
            }

            loopDoneDao.addIfAbsent(
                LoopDoneVo(
                    loopId = loop.loopId,
                    date = date.toMs(),
                    startInDay = loop.startInDay,
                    endInDay = loop.endInDay,
                    done = if (loop.enabled) {
                        DoneState.NO_RESPONSE
                    } else {
                        DoneState.DISABLED
                    }
                )
            )

            date = date.plusDays(1)
        }
    }

    /**
     * 오늘 이미 완료/스킵으로 응답한 루프인지 비동기로 확인한다. 진행 알림 틱이
     * 응답을 마친 루프의 알림을 계속 띄우지 않도록 판단하는 데 쓰인다.
     */
    fun checkRespondedToday(loop: LoopBase, onResult: (Boolean) -> Unit) {
        coroutineScope.launch {
            val state = loopDoneDao.getDoneState(loop.loopId, LocalDate.now().toMs())?.done
            onResult(state == DoneState.DONE || state == DoneState.SKIP)
        }
    }

    fun cancelAlarm(loop: LoopBase) {
        if (loop.enabled) {
            coroutineScope.launch { loopDao.addOrUpdate(loop.asLoopVo(enabled = false)) }
        }
        logger.d { " - cancel id:${loop.loopId}, title:${loop.title}" }

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_LOOP_START
        }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                loop.loopId,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        alarmManager.cancel(pendingIntent)
    }

    @AndroidEntryPoint
    class AlarmReceiver : BroadcastReceiver() {

        private val logger = log("AlarmReceiver")

        @Inject
        lateinit var alarmController: LoopScheduler

        @Inject
        lateinit var notificationHelper: NotificationHelper

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_LOOP_START -> handleActionLoopStart(context, intent)
                ACTION_LOOP_END -> handleActionLoopEnd(context, intent)
                ACTION_LOOP_REPEAT -> handleActionLoopRepeat(intent)
                ACTION_LOOP_PROGRESS -> handleActionLoopProgress(intent)
                ACTION_LOOP_SYNC -> handleActionLoopSync(context, intent)

                ACTION_LOOP_DONE -> handleActionLoopDone(intent)
                ACTION_LOOP_CANCEL -> handleActionLoopCancel(intent)
            }
        }

        private fun handleActionLoopDone(intent: Intent) {
            notificationHelper.cancel(intent.asLoop())
        }

        private fun handleActionLoopCancel(intent: Intent) {
            notificationHelper.cancel(intent.asLoop())
        }


        private fun handleActionLoopSync(context: Context, intent: Intent) {
            val loop = intent.asLoop()

            if (loop.loopId == MIDNIGHT_RESERVATION_ID) {
                alarmController.syncLoops()

                // TEMP CODE
                notificationHelper.notify(loop)
            }
            AppWidgetUpdateWorker.updateWidget(context)
        }

        private fun handleActionLoopStart(context: Context, intent: Intent) {
            val loop = intent.asLoop()

            if (loop.interval == NO_REPEAT) {
                alarmController.reserveAlarm(scheduleEnd(loop))
            } else {
                reserveRepeat(loop = loop)
            }

            // 시작 시점에는 소리로 알리고, 이후 진행 알림은 1분 틱으로 조용히 갱신된다.
            notifyLoop(loop, alert = true)
            AppWidgetUpdateWorker.updateWidget(context)

            val isAllowedDay = loop.isActiveDay()
            val isAllowedTime = loop.isActiveTime()
            val today = dayForLoop(LocalDate.now())
            logger.d {
                """ -->
                |Received alarm id:${loop.loopId} 
                | title:${loop.title},
                | today:${LoopDay.toString(today)},
                | isAllowedDay:$isAllowedDay, 
                | isAllowedTime:$isAllowedTime""".trimMargin()
            }
        }

        private fun handleActionLoopEnd(context: Context, intent: Intent) {
            val loop = intent.asLoop()
            // 종료 시각: 진행 알림을 내려 루프 창이 끝났음을 알린다.
            notificationHelper.cancel(loop)
            AppWidgetUpdateWorker.updateWidget(context)
        }

        private fun handleActionLoopRepeat(intent: Intent) {
            val loop = intent.asLoop()

            reserveRepeat(loop)
            // 반복 간격마다 다시 소리로 알린다 (진행 알림 내용도 함께 갱신된다).
            notifyLoop(loop, alert = true)
        }

        /**
         * 1분 진행 틱: 아직 진행 중이면 알림을 조용히 갱신하고 다음 틱을 예약한다.
         * 종료 시각을 지났거나 이미 완료/스킵으로 응답한 루프면 알림을 내리고 틱을 멈춘다.
         */
        private fun handleActionLoopProgress(intent: Intent) {
            val loop = intent.asLoop()
            if (!loop.isActiveTime()) {
                notificationHelper.cancel(loop)
                return
            }

            alarmController.checkRespondedToday(loop) { responded ->
                if (responded) {
                    notificationHelper.cancel(loop)
                } else {
                    notifyLoop(loop, alert = false)
                }
            }
        }

        private fun reserveRepeat(loop: LoopBase) {
            val now = msNow
            if (now + loop.interval >= loop.endInDay) {
                alarmController.reserveAlarm(scheduleEnd(loop))
            } else {
                alarmController.reserveAlarm(scheduleRepeat(loop))
            }
        }

        /**
         * 지금 진행 중인 루프의 알림을 띄운다. 시작·종료 시각이 모두 있는 루프는
         * 남은 시간을 보여주는 진행 알림(+1분 갱신 틱 예약)으로, 시간 창이 없는
         * 루프는 기존의 단순 알림으로 표시한다. [alert]가 true면 소리로 알린다.
         */
        private fun notifyLoop(loop: LoopBase, alert: Boolean) {
            val isAllowedDay = loop.isActiveDay()
            val isAllowedTime = loop.isActiveTime()
            val isMock = loop.isMock
            if (isMock || !isAllowedDay || !isAllowedTime) return

            if (loop.hasTimeWindow) {
                notificationHelper.notifyProgress(loop, alert = alert)
                alarmController.reserveAlarm(scheduleProgressTick(loop))
            } else {
                notificationHelper.notify(loop)
            }
        }
    }

    companion object {
        private const val EXTRA_RESERVED_TIME = "loop_reserved_time"

        val msNow get() = LocalTime.now().toMs()

        fun scheduleStart(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_START,
            after = loop.startInDay - msNow,
            loop = loop
        )

        fun scheduleEnd(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_END,
            after = loop.endInDay - msNow,
            loop = loop
        )

        fun scheduleRepeat(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_REPEAT,
            after = loop.interval - ((msNow - loop.startInDay) % loop.interval),
            loop = loop
        )

        /**
         * 진행 알림을 갱신하는 틱. 기본은 1분 뒤이며, 동기화 시점에 이미 진행 중인
         * 루프를 즉시 표시해야 할 때는 [after]를 짧게 줄 수 있다.
         */
        fun scheduleProgressTick(loop: LoopBase, after: Long = MS_1MIN) = LoopSchedule(
            action = ACTION_LOOP_PROGRESS,
            after = after,
            loop = loop
        )

        /** 시작·종료 시각이 모두 있어 남은 시간을 계산할 수 있는 루프인지 */
        val LoopBase.hasTimeWindow: Boolean
            get() = startInDay >= 0 && endInDay >= 0

        fun scheduleSync() = LoopSchedule(
            action = ACTION_LOOP_SYNC,
            after = LoopVo.midnight().startInDay - msNow,
            loop = LoopVo.midnight()
        )

        data class LoopSchedule internal constructor(
            @LoopScheduleAction val action: String,
            val after: Long,
            val loop: LoopBase
        )
    }
}
