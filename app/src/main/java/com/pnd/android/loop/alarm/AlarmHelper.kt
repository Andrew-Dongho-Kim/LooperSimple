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
import com.pnd.android.loop.common.test
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.Loop
import com.pnd.android.loop.data.description
import com.pnd.android.loop.util.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AlarmHelper"


class AlarmHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    appDb: AppDatabase
) {
    private val logger = log(TAG)

    private val loopDao = appDb.loopDao()

    @VisibleForTesting
    fun notifyAfter(loop: Loop): Long {
        val curr = localTime()
        if (loop.tickStart == 0L) {
            val timeInDay = curr % MS_1DAY
            loop.tickStart = curr - timeInDay + loop.loopStart
            loop.enabled = false
            test { "tick start : ${loop.tickStart}" }
        }

        return if (loop.tickStart > curr) {
            loop.tickStart - curr
        } else {
            loop.interval - ((curr - loop.tickStart) % loop.interval)
        }
    }

    fun reserveRepeat(
        loop: Loop,
        showToast: Boolean = true
    ) {
        val after = notifyAfter(loop)
        val systemElapsed = SystemClock.elapsedRealtime()
        if (!loop.enabled) {
            loop.enabled = true
            GlobalScope.launch { loopDao.add(loop) }
        }

        logger.d {
            " - repeat after:${dh2m2(after)} ${loop.description(context)}"
        }

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_LOOP_ALARM
            loop.putToIntent(this)
            putExtra(EXTRA_RESERVED_TIME, systemElapsed + after)
        }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                loop.id,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        alarmManager.setExact(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            systemElapsed + after,
            pendingIntent
        )

        if (showToast) {
            GlobalScope.launch(Dispatchers.Main) {
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
        GlobalScope.launch {
            loopDao.syncGetAll().forEach { loop ->
                if (loop.enabled) {
                    reserveRepeat(loop = loop, showToast = false)
                } else {
                    cancel(loop)
                }
            }
        }
    }

    fun cancel(loop: Loop) {
        if (loop.enabled) {
            loop.enabled = false
            loop.tickStart = 0
            GlobalScope.launch { loopDao.add(loop) }
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
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        alarmManager.cancel(pendingIntent)
    }

    @AndroidEntryPoint
    class AlarmReceiver : BroadcastReceiver() {

        private val logger = log(TAG)

        @Inject
        lateinit var alarmHelper: AlarmHelper

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
            notificationHelper.cancel(Loop.popFromIntent(intent))
        }

        private fun handleActionLoopCancel(intent: Intent) {
            notificationHelper.cancel(Loop.popFromIntent(intent))
        }

        private fun handleActionLoopAlarm(context: Context, intent: Intent) {
            val loop = Loop.popFromIntent(intent)
            alarmHelper.reserveRepeat(loop = loop, showToast = false)

            val currTime = localTime()
            val today = day(currTime)
            val isAllowedDay = loop.isAllowedDay()
            val isAllowedTime = loop.isAllowedTime()
            if (isAllowedDay && isAllowedTime) {
                alarmPlayer.play(AlarmCategory.alarm(loop.alarms))
                notificationHelper.notify(loop)
            }

            logger.d {
                """ -->
                |Received alarm id:${loop.id} 
                | title:${loop.title},
                | today:${Loop.Day.toString(today)},
                | isAllowedDay:$isAllowedDay, 
                | isAllowedTime:$isAllowedTime""".trimMargin()
            }
        }
    }

    companion object {
        private const val EXTRA_RESERVED_TIME = "loop_reserved_time"
        private val ALLOW_DIFF = MS_1MIN
    }
}