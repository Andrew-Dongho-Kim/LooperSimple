package com.pnd.android.loop.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.pnd.android.loop.alarm.notification.NotificationHelper
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.Day
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject


class AlarmController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    appDb: AppDatabase
) {
    private val logger = log("AlarmController")

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val loopDao = appDb.loopDao()
    private val loopDoneDao = appDb.loopDoneDao()

    @VisibleForTesting
    fun notifyAfter(loop: LoopBase): Long {
        val curr = LocalTime.now().toMs()

        val loopStart = loop.loopStart
        return if (loopStart > curr) {
            loopStart - curr
        } else if (loop.interval == 0L) {
            NO_NOTIFY
        } else {
            loop.interval - ((curr - loopStart) % loop.interval)
        }
    }

    fun reserveAlarm(
        loop: LoopBase
    ) {
        val after = notifyAfter(loop)
        val systemElapsed = SystemClock.elapsedRealtime()
        if (!loop.enabled) {
            coroutineScope.launch { loopDao.addOrUpdate(loop.asLoopVo(enabled = true)) }
        }
        if (after == NO_NOTIFY) {
            return
        }

        logger.d {
            " - repeat after:${dh2m2(after)} ${loop.description(context)}"
        }

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_LOOP_ALARM
            loop.putTo(this)
            putExtra(EXTRA_RESERVED_TIME, systemElapsed + after)
        }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                loop.id,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                systemElapsed + after,
                pendingIntent
            )
        }
    }

    fun syncAlarms() {
        logger.d { "start sync" }
        coroutineScope.launch {
            loopDao.allLoops().forEach { loop ->
                fillNoResponse(loop)
                if (loop.enabled) {
                    reserveAlarm(loop = loop)
                } else {
                    cancelAlarm(loop)
                }
            }
            reserveAlarm(loop = LoopBase.midnight())
        }
    }

    private suspend fun fillNoResponse(loop: LoopBase) {
        val now = LocalDate.now()
        var date = now.minusDays(1L)
        
        while (date.isBefore(now) || date.isEqual(now)) {
            if (!loop.isActiveDay(date)) {
                date = date.plusDays(1)
                continue
            }

            loopDoneDao.addIfAbsent(
                LoopDoneVo(
                    loopId = loop.id,
                    date = date.toMs(),
                    done = LoopDoneVo.DoneState.NO_RESPONSE
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
            action = ACTION_LOOP_ALARM
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
        lateinit var alarmController: AlarmController

        @Inject
        lateinit var notificationHelper: NotificationHelper

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_LOOP_ALARM -> handleActionLoopAlarm(context, intent)
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

        private fun handleActionLoopAlarm(context: Context, intent: Intent) {
            val loop = intent.asLoop()

            if (loop.interval != NO_REPEAT) {
                alarmController.reserveAlarm(loop = loop)
            }

            val today = dayForLoop(LocalDate.now())

            val isAllowedDay = loop.isActiveDay()
            val isAllowedTime = loop.isActiveTime()
            val isMock = loop.isMock
            if (isMock) {
                if (loop.id == LoopBase.MIDNIGHT_RESERVATION_ID) {
                    alarmController.syncAlarms()

                    // TEMP CODE
                    notificationHelper.notify(loop)
                }
            } else if (isAllowedDay && isAllowedTime) {
                notificationHelper.notify(loop)
            }

            AppWidgetUpdateWorker.updateWidget(context)
            logger.d {
                """ -->
                |Received alarm id:${loop.id} 
                | title:${loop.title},
                | today:${Day.toString(today)},
                | isAllowedDay:$isAllowedDay, 
                | isAllowedTime:$isAllowedTime""".trimMargin()
            }
        }
    }


    companion object {
        private const val EXTRA_RESERVED_TIME = "loop_reserved_time"

        private const val NO_NOTIFY = -1L
    }
}