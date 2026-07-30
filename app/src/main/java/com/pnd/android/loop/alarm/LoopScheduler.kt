package com.pnd.android.loop.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.pnd.android.loop.alarm.notification.LoopForegroundService
import com.pnd.android.loop.alarm.notification.LoopStartAnnouncer
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
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.dh2m2
import com.pnd.android.loop.util.isActive
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
    @param:ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    appDb: AppDatabase
) {
    private val logger = log("LoopScheduler")

    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val loopDao = appDb.loopDao()
    private val fullLoopDao = appDb.fullLoopDao()
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
            logger.i {
                " - repeat after:${dh2m2(after)} ${loop.description(context)}"
            }

        } else {

            logger.e { "can't schedule exact alarm" }
        }
    }

    fun syncLoops() {
        coroutineScope.launch {
            var hasActiveLoop = false
            fullLoopDao.getAllLoops().forEach { loop ->
                logger.e { "Loop:${loop.title}, isActive:${loop.isActive()}, isActiveDay:${loop.isActiveDay()}, isActiveTime:${loop.isActiveTime()}, isAnyTime:${loop.isAnyTime}, done:${loop.done}" }
                fillNoResponse(loop)
                if (loop.enabled) {
                    reserveAlarm(scheduleStart(loop))
                    hasActiveLoop = hasActiveLoop or loop.isActive()
                } else {
                    cancelAlarm(loop)
                }
            }
            reserveAlarm(scheduleSync())

            // 진행 중인 루프가 있으면 상시 알림 서비스를 (재)시작한다. 실제로 보여줄
            // 루프가 없다면 서비스가 스스로 종료하므로 안전하다.
            if (hasActiveLoop) LoopForegroundService.refresh(context)
            logger.i { "Start syncLoops hasActiveLoop:$hasActiveLoop" }
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
     * 진행 중 루프 통합 알림을 최신 상태로 만든다. 앱 안에서 완료/스킵 등으로 루프
     * 상태가 바뀌었을 때 호출하면, 포그라운드 서비스가 DB를 다시 읽어 알림을 갱신하고
     * 더 이상 보여줄 루프가 없으면 스스로 알림을 내린다.
     */
    fun refreshOngoingNotification() {
        LoopForegroundService.refresh(context)
    }

    fun cancelAlarm(loop: LoopBase) {
        if (loop.enabled) {
            coroutineScope.launch { loopDao.addOrUpdate(loop.asLoopVo(enabled = false)) }
        }
        logger.i { " - cancel id:${loop.loopId}, title:${loop.title}" }

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
        lateinit var loopStartAnnouncer: LoopStartAnnouncer

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_LOOP_START -> handleActionLoopStart(context, intent)
                ACTION_LOOP_END -> handleActionLoopEnd(context, intent)
                ACTION_LOOP_REPEAT -> handleActionLoopRepeat(context, intent)
                ACTION_LOOP_SYNC -> handleActionLoopSync(context, intent)

                ACTION_LOOP_DONE -> handleActionLoopDone(context)
                ACTION_LOOP_CANCEL -> handleActionLoopCancel(context)
            }
        }

        // 완료/스킵/취소 시 통합 알림을 즉시 다시 계산한다. 응답한 루프는 목록에서
        // 빠지고, 남은 진행 중 루프가 없으면 서비스가 스스로 알림을 내린다.
        private fun handleActionLoopDone(context: Context) {
            LoopForegroundService.refresh(context)
        }

        private fun handleActionLoopCancel(context: Context) {
            LoopForegroundService.refresh(context)
        }


        private fun handleActionLoopSync(context: Context, intent: Intent) {
            val loop = intent.asLoop()

            if (loop.loopId == MIDNIGHT_RESERVATION_ID) {
                alarmController.syncLoops()
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

            // 진행 중인 루프를 상시 알림 서비스로 넘긴다. 서비스가 알림을 소유하므로
            // 앱이 실행 중이 아니어도 유지되고, 사용자가 스와이프로 지울 수 없다.
            notifyActiveLoop(context, loop)
            // 상시 알림 등록에 더해, 방금 시작된 루프를 화면 상단에 한 번 알린다.
            // (반복 틱이 아니라 실제 시작 시점에만 호출된다)
            if (loop.isActive()) loopStartAnnouncer.announce(loop)
            AppWidgetUpdateWorker.updateWidget(context)

            val isAllowedDay = loop.isActiveDay()
            val isAllowedTime = loop.isActiveTime()
            val today = dayForLoop(LocalDate.now())
            logger.i {
                """ -->
                |Received alarm id:${loop.loopId} 
                | title:${loop.title},
                | today:${LoopDay.toString(today)},
                | isAllowedDay:$isAllowedDay, 
                | isAllowedTime:$isAllowedTime""".trimMargin()
            }
        }

        private fun handleActionLoopEnd(context: Context, intent: Intent) {
            // 종료 시각: 통합 알림을 다시 계산해 끝난 루프를 목록에서 뺀다.
            LoopForegroundService.refresh(context)
            AppWidgetUpdateWorker.updateWidget(context)
        }

        private fun handleActionLoopRepeat(context: Context, intent: Intent) {
            val loop = intent.asLoop()

            reserveRepeat(loop)
            // 반복 간격마다 알림 내용을 조용히 갱신한다.
            notifyActiveLoop(context, loop)
        }

        private fun reserveRepeat(loop: LoopBase) {
            // 다음 반복 틱이 종료 시각을 넘기면(=종료가 먼저 오면) 종료 알람을, 아니면 다음 반복 알람을 건다.
            // 두 값 모두 자정 넘김을 고려한 "지금부터 남은 ms" 로 계산해, 23:00~02:00 같은 루프도 정확히 처리한다.
            val untilEnd = msUntilInDay(loop.endInDay)
            val untilNextTick = loop.interval - (msSinceStart(loop) % loop.interval)
            if (untilNextTick >= untilEnd) {
                alarmController.reserveAlarm(scheduleEnd(loop))
            } else {
                alarmController.reserveAlarm(scheduleRepeat(loop))
            }
        }

        /**
         * 지금 진행 중인 루프를 상시 알림 서비스로 넘긴다. 서비스가 DB를 다시 읽어
         * 현재 진행 중인 모든 루프를 하나의 알림으로 묶어 보여주고 1분마다 갱신하므로,
         * 여기서는 유효한 루프일 때 서비스를 (재)시작하기만 하면 된다.
         */
        private fun notifyActiveLoop(context: Context, loop: LoopBase) {
            if (!loop.isActive()) return

            LoopForegroundService.refresh(context)
        }
    }

    companion object {
        private const val EXTRA_RESERVED_TIME = "loop_reserved_time"

        val msNow get() = LocalTime.now().toMs()

        /** msNow 기준으로 하루 안의 [targetInDay] 시각까지 남은 ms(자정 넘김 고려, 0 ~ 24h). */
        fun msUntilInDay(targetInDay: Long): Long =
            ((targetInDay - msNow) % MS_1DAY + MS_1DAY) % MS_1DAY

        /** msNow 기준으로 루프 시작 이후 하루 안에서의 경과 ms(자정 넘김 고려, 0 ~ 24h). */
        fun msSinceStart(loop: LoopBase): Long =
            ((msNow - loop.startInDay) % MS_1DAY + MS_1DAY) % MS_1DAY

        fun scheduleStart(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_START,
            // 시작은 오늘 안의 고정 시각. 이미 지났으면 after<=0 이 되어 예약을 건너뛴다(자정 동기화가 내일 것을 잡는다).
            after = loop.startInDay - msNow,
            loop = loop
        )

        fun scheduleEnd(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_END,
            // 자정을 넘기는 루프는 종료가 다음 날이므로, 남은 시간을 하루 둘레로 계산한다.
            after = msUntilInDay(loop.endInDay),
            loop = loop
        )

        fun scheduleRepeat(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_REPEAT,
            after = loop.interval - (msSinceStart(loop) % loop.interval),
            loop = loop
        )

        fun scheduleSync() = LoopSchedule(
            action = ACTION_LOOP_SYNC,
            after = LoopVo.midnight().startInDay - msNow,
            loop = LoopVo.midnight()
        )

        data class LoopSchedule(
            @LoopScheduleAction val action: String,
            val after: Long,
            val loop: LoopBase
        )
    }
}
