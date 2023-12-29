package com.pnd.android.loop.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import com.pnd.android.loop.alarm.notification.NotificationHelper
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopVo.Companion.NO_REPEAT
import com.pnd.android.loop.data.LoopVo.Companion.asLoop
import com.pnd.android.loop.data.description
import com.pnd.android.loop.util.day
import com.pnd.android.loop.util.dh2m2
import com.pnd.android.loop.util.hourIn24
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.isActiveTime
import com.pnd.android.loop.util.localTimeInDay
import com.pnd.android.loop.util.min
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AlarmController"


class AlarmController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    appDb: AppDatabase
) {
    private val logger = log(TAG)

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val loopDao = appDb.loopDao()

    @VisibleForTesting
    fun notifyAfter(loop: LoopVo): Long {
        val curr = localTimeInDay()

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
        loop: LoopVo,
        showToast: Boolean = true
    ) {
        val after = notifyAfter(loop)
        val systemElapsed = SystemClock.elapsedRealtime()
        if (!loop.enabled) {
            coroutineScope.launch { loopDao.addOrUpdate(loop.copy(enabled = true)) }
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

        alarmManager.setExact(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            systemElapsed + after,
            pendingIntent
        )

        if (showToast) {
            coroutineScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    context, "${hourIn24(after)}시간 ${min(after)}분 후에 알람이 울립니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun vibe() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        val msTime = 10L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator!!.vibrate(
                VibrationEffect.createOneShot(
                    msTime,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator!!.vibrate(msTime)
        }
    }


    fun syncAlarms() {
        logger.d { "start sync" }
        coroutineScope.launch {
            loopDao.allLoops().forEach { loop ->
                if (loop.enabled) {
                    reserveAlarm(loop = loop, showToast = false)
                } else {
                    cancelAlarm(loop)
                }
            }
        }
    }

    fun cancelAlarm(loop: LoopVo) {
        if (loop.enabled) {
            coroutineScope.launch { loopDao.addOrUpdate(loop.copy(enabled = false)) }
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

        private val logger = log(TAG)

        @Inject
        lateinit var alarmController: AlarmController

        @Inject
        lateinit var alarmPlayer: AlarmPlayer

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
                alarmController.reserveAlarm(loop = loop, showToast = false)
            }

            val today = day()
            val isAllowedDay = loop.isActiveDay()
            val isAllowedTime = loop.isActiveTime()
            if (isAllowedDay && isAllowedTime) {
                alarmPlayer.play(AlarmCategory.alarm(loop.alarms))
                notificationHelper.notify(loop)
            }

            logger.d {
                """ -->
                |Received alarm id:${loop.id} 
                | title:${loop.title},
                | today:${LoopVo.Day.toString(today)},
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