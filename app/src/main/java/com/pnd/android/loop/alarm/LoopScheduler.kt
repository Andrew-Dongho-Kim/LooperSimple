package com.pnd.android.loop.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.pnd.android.loop.alarm.notification.NotificationHelper
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.Day
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.NO_REPEAT
import com.pnd.android.loop.data.asLoop
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.description
import com.pnd.android.loop.data.putTo
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
    private val logger = log("AlarmController")

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val loopDao = appDb.loopDao()
    private val loopDoneDao = appDb.loopDoneDao()

    fun reserveAlarm(
        loopSchedule: LoopSchedule
    ) {
        val (action, after, loop) = loopSchedule
        if (after <= 0) return

        logger.d {
            " - repeat after:${dh2m2(after)} ${loop.description(context)}"
        }

        val systemElapsed = SystemClock.elapsedRealtime()
        val reservedTime = systemElapsed + after
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            loop.putTo(this)
            putExtra(EXTRA_RESERVED_TIME, reservedTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            loop.id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                reservedTime,
                pendingIntent
            )
        }
    }

    fun syncLoops() {
        logger.d { "start sync" }
        coroutineScope.launch {
            loopDao.allLoops().forEach { loop ->
                fillNoResponse(loop)
                if (loop.enabled) {
                    reserveAlarm(scheduleStart(loop))
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
                    loopId = loop.id,
                    date = date.toMs(),
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

    fun cancelAlarm(loop: LoopBase) {
        if (loop.enabled) {
            coroutineScope.launch { loopDao.addOrUpdate(loop.asLoopVo(enabled = false)) }
        }
        logger.d { " - cancel id:${loop.id}, title:${loop.title}" }

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_LOOP_START
        }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                loop.id,
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

            if (loop.id == LoopBase.MIDNIGHT_RESERVATION_ID) {
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

            notifyLoop(loop)
            AppWidgetUpdateWorker.updateWidget(context)

            val isAllowedDay = loop.isActiveDay()
            val isAllowedTime = loop.isActiveTime()
            val today = dayForLoop(LocalDate.now())
            logger.d {
                """ -->
                |Received alarm id:${loop.id} 
                | title:${loop.title},
                | today:${Day.toString(today)},
                | isAllowedDay:$isAllowedDay, 
                | isAllowedTime:$isAllowedTime""".trimMargin()
            }
        }

        private fun handleActionLoopEnd(context: Context, intent: Intent) {
            val loop = intent.asLoop()
            notifyLoop(loop)
            AppWidgetUpdateWorker.updateWidget(context)
        }

        private fun handleActionLoopRepeat(intent: Intent) {
            val loop = intent.asLoop()

            reserveRepeat(loop)
            notifyLoop(loop)
        }

        private fun reserveRepeat(loop: LoopBase) {
            val now = msNow
            if (now + loop.interval >= loop.loopEnd) {
                alarmController.reserveAlarm(scheduleEnd(loop))
            } else {
                alarmController.reserveAlarm(scheduleRepeat(loop))
            }
        }

        private fun notifyLoop(loop: LoopBase) {
            val isAllowedDay = loop.isActiveDay()
            val isAllowedTime = loop.isActiveTime()
            val isMock = loop.isMock
            if (!isMock && isAllowedDay && isAllowedTime) {
                notificationHelper.notify(loop)
            }
        }
    }

    companion object {
        private const val EXTRA_RESERVED_TIME = "loop_reserved_time"

        val msNow get() = LocalTime.now().toMs()

        fun scheduleStart(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_START,
            after = loop.loopStart - msNow,
            loop = loop
        )

        fun scheduleEnd(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_END,
            after = loop.loopEnd - msNow,
            loop = loop
        )

        fun scheduleRepeat(loop: LoopBase) = LoopSchedule(
            action = ACTION_LOOP_REPEAT,
            after = loop.interval - ((msNow - loop.loopStart) % loop.interval),
            loop = loop
        )

        fun scheduleSync() = LoopSchedule(
            action = ACTION_LOOP_SYNC,
            after = LoopBase.midnight().loopStart - msNow,
            loop = LoopBase.midnight()
        )

        data class LoopSchedule internal constructor(
            @LoopScheduleAction val action: String,
            val after: Long,
            val loop: LoopBase
        )
    }
}
