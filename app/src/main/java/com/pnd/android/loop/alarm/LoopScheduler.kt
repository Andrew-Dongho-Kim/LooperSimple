package com.pnd.android.loop.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.pnd.android.loop.alarm.notification.AnyTimeStartPrompter
import com.pnd.android.loop.alarm.notification.LoopEndPrompter
import com.pnd.android.loop.alarm.notification.LoopForegroundService
import com.pnd.android.loop.alarm.notification.LoopStartAnnouncer
import com.pnd.android.loop.alarm.notification.NotificationSettings
import com.pnd.android.loop.alarm.notification.cancelLoopPrompts
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
import com.pnd.android.loop.util.MS_1HOUR
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.dh2m2
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.isActiveTime
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import com.pnd.android.loop.util.toTimeTextForLog
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
    private val habitualStartEstimator: HabitualStartEstimator,
    private val notificationSettings: NotificationSettings,
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
            // 정확 알람이 막혔다고 아무것도 예약하지 않으면 시작 알림·종료 확인·습관 알림이 전부
            // 오지 않아 앱이 사실상 멈춘다. 몇 분 늦더라도 도착하는 편이 훨씬 낫다.
            // setAndAllowWhileIdle 은 정확 알람 권한 없이도 Doze 를 넘어 전달된다(오차 최대 수 분).
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                reservedTime,
                pendingIntent
            )
            logger.i {
                " - inexact(after:${dh2m2(after)}) ${loop.description(context)}"
            }
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
                    // anytime 루프는 시작 시각이 없어 위 예약이 그냥 건너뛰어진다(after <= 0).
                    // 대신 과거 기록에서 추정한 습관 시각에 "시작할까요?" 알람을 건다.
                    if (loop.isAnyTime) reserveAnyTimeDueAlarm(loop)
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

    /**
     * anytime 루프에 "보통 이 시각에 시작하셨어요" 알람을 오늘 한 번 예약한다.
     *
     * 이 함수는 [syncLoops] 를 타므로 자정, 부팅, 그리고 앱을 열 때마다 다시 불린다. 같은
     * PendingIntent 를 다시 setExact 하는 것이므로 중복 예약은 되지 않고 시각만 갱신된다.
     *
     * 추정 시각이 이미 지났으면 [reserveAlarm] 이 스스로 건너뛴다(after <= 0). 즉 하루에 한 번,
     * 그 시각을 놓쳤으면 그날은 조용히 넘어간다.
     */
    private suspend fun reserveAnyTimeDueAlarm(loop: LoopBase) {
        val preferences = notificationSettings.current
        if (!preferences.anyTimeDueEnabled) {
            // 설정에서 껐다면 이미 걸려 있는 알람까지 걷어낸다. 그냥 두면 오늘 몫이 그대로 도착한다.
            cancelAlarm(loop = loop, alarmAction = ACTION_LOOP_ANYTIME_DUE)
            return
        }

        // 오늘이 그 루프의 요일이 아니면 애초에 할 일이 없다.
        if (!loop.isActiveDay()) return

        val habitualStart = habitualStartEstimator.estimate(loop.loopId)
        if (habitualStart == null) {
            // 실제로 시작한 기록이 아직 없다. 근거 없이 시각을 발명하지 않는다.
            logger.i { " - no habitual start yet: ${loop.title}" }
            return
        }

        val dueInDay = habitualStart.startInDay
        // 사용자가 직접 정하지 않고 앱이 추정한 시각이라, 방해 금지 구간이면 물러난다.
        if (preferences.isInQuietHours(dueInDay)) {
            logger.i { " - habitual start is in quiet hours: ${loop.title}" }
            return
        }

        logger.i {
            " - anytime due at ${dueInDay.toTimeTextForLog()} " +
                    "(${habitualStart.basis}, ${habitualStart.sampleCount} samples) ${loop.title}"
        }
        reserveAlarm(
            LoopSchedule(
                action = ACTION_LOOP_ANYTIME_DUE,
                after = dueInDay - msNow,
                loop = loop,
            )
        )
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

    /**
     * 앱에서 루프 상태를 바꿨다면, 그 루프에 대해 답을 기다리던 알림들도 함께 내린다.
     * 그 알림들은 사용자가 어디서 답했는지 알 수 없으므로 기록하는 쪽에서 내려 줘야 한다.
     */
    fun cancelLoopPrompts(loopId: Int) {
        cancelLoopPrompts(context = context, loopId = loopId)
    }

    fun cancelAlarm(loop: LoopBase) {
        if (loop.enabled) {
            coroutineScope.launch { loopDao.addOrUpdate(loop.asLoopVo(enabled = false)) }
        }
        logger.i { " - cancel id:${loop.loopId}, title:${loop.title}" }

        // PendingIntent 동등성은 action 을 포함하므로 시작/종료/반복/습관시각이 각각 별개로
        // 예약돼 있다. START 만 취소하면 남은 알람이 나중에 유령처럼 도착해 서비스를 깨우고
        // 반복 체인을 계속 재예약한다. 예약하는 모든 액션을 빠짐없이 취소한다.
        listOf(
            ACTION_LOOP_START,
            ACTION_LOOP_END,
            ACTION_LOOP_REPEAT,
            ACTION_LOOP_ANYTIME_DUE,
        ).forEach { alarmAction ->
            cancelAlarm(loop = loop, alarmAction = alarmAction)
        }
    }

    /** 특정 루프의 특정 액션 알람 하나만 취소한다. */
    private fun cancelAlarm(loop: LoopBase, @LoopScheduleAction alarmAction: String) {
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = alarmAction
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            loop.loopId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    @AndroidEntryPoint
    class AlarmReceiver : BroadcastReceiver() {

        private val logger = log("AlarmReceiver")

        @Inject
        lateinit var alarmController: LoopScheduler

        @Inject
        lateinit var loopStartAnnouncer: LoopStartAnnouncer

        @Inject
        lateinit var loopEndPrompter: LoopEndPrompter

        @Inject
        lateinit var anyTimeStartPrompter: AnyTimeStartPrompter

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_LOOP_START -> handleActionLoopStart(context, intent)
                ACTION_LOOP_END -> handleActionLoopEnd(context, intent)
                ACTION_LOOP_REPEAT -> handleActionLoopRepeat(intent)
                ACTION_LOOP_SYNC -> handleActionLoopSync(context, intent)
                ACTION_LOOP_ANYTIME_DUE -> handleActionAnyTimeDue(intent)

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


        /**
         * 습관 시각 도달: 오늘 아직 시작하지 않은 anytime 루프라면 "시작할까요?" 를 묻는다.
         * 알림만 띄우면 되므로 상시 알림·위젯은 건드리지 않는다(아직 상태가 바뀐 게 없다).
         */
        private fun handleActionAnyTimeDue(intent: Intent) {
            anyTimeStartPrompter.prompt(intent.asLoop().loopId)
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
            // 종료 시각: 통합 알림을 다시 계산해 끝난 루프를 목록에서 뺀다. 서비스의 1분 틱도
            // 같은 일을 하지만, Doze 등으로 틱을 놓친 경우를 위한 확실한 트리거로 남겨 둔다.
            LoopForegroundService.refresh(context)
            AppWidgetUpdateWorker.updateWidget(context)

            // 목록에서 빼기만 하면 미응답 루프가 조용히 사라진다. 아직 답하지 않았다면
            // "완료했나요?" 로 한 번 물어, 그 자리에서 기록할 수 있게 한다.
            loopEndPrompter.prompt(intent.asLoop().loopId)
        }

        private fun handleActionLoopRepeat(intent: Intent) {
            val loop = intent.asLoop()

            // 알림은 건드리지 않는다. 포그라운드 서비스가 이미 1분마다 DB를 다시 읽어
            // 갱신하므로, 반복 틱마다 refresh 를 부르면 서비스만 불필요하게 재기동된다.
            // 이 알람의 역할은 종료 알람까지 이어지는 반복 체인을 유지하는 것뿐이다.
            reserveRepeat(loop)
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
