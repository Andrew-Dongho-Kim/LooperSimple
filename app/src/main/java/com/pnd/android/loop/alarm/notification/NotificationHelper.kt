package com.pnd.android.loop.alarm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pnd.android.loop.R
import com.pnd.android.loop.alarm.LoopScheduler
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.putTo
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject


const val CHANNEL_ID = "com.pnd.android.loop.LooperSimple"

class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nm: NotificationManager
) {

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.loop),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)
    }

    private fun LoopVo.pendingIntent(action: String) = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, LoopScheduler.AlarmReceiver::class.java).apply {
            this.action = action
            putTo(this)
        },
        // FLAG_IMMUTABLE is mandatory on Android 12+ (API 31); omitting a
        // mutability flag throws IllegalArgumentException.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun notify(loop: LoopBase) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(loop.title)

            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(0)
            .setVibrate(null)
            .setSound(null)
            .setAutoCancel(true)

        nm.notify(loop.loopId, builder.build())
    }

    /**
     * 진행 중인 루프의 상태 알림. 루프가 진행되는 동안(시작~종료) 알림 창에 고정되어
     * 진행률 바와 "11:20까지 · 32분 남음" 문구를 보여준다. 1분마다 다시 호출되어
     * 내용이 갱신되며, [alert]가 false면 갱신 시 소리/진동 없이 조용히 바뀐다.
     */
    fun notifyProgress(loop: LoopBase, alert: Boolean) {
        val window = LoopTimeWindow.of(loop) ?: return

        val untilText = context.getString(
            R.string.notification_until_time,
            loop.endInDay.toLocalTime().formatText(),
        )
        val timeLeftText = context.resources.getQuantityString(
            R.plurals.notification_time_left,
            window.remainMinutes,
            window.remainMinutes,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(loop.title)
            .setContentText("$untilText · $timeLeftText")
            .setProgress(window.totalMinutes, window.elapsedMinutes, false)
            // 진행 중에는 스와이프로 지워지지 않게 고정하고, 종료 시각에 코드로 내린다.
            .setOngoing(true)
            .setOnlyAlertOnce(!alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setDefaults(0)
            .setVibrate(null)
            .setSound(null)
            .setShowWhen(false)

        nm.notify(loop.loopId, builder.build())
    }

    fun cancel(loop: LoopBase) {
        nm.cancel(loop.loopId)
    }

    private fun LocalTime.formatText() = String.format("%02d:%02d", hour, minute)
}

/**
 * 알림 표시용으로 계산한 루프의 시간 창. 자정을 넘기는 루프(예: 23:00~01:00)도
 * 종료 시각에 하루를 더해 같은 축 위에서 계산한다. 분 단위는 올림이라 마지막 1분까지
 * "1분 남음"으로 보이고, 시작·종료 시각이 없는(anytime) 루프는 만들 수 없다.
 */
private class LoopTimeWindow private constructor(
    val totalMinutes: Int,
    val elapsedMinutes: Int,
    val remainMinutes: Int,
) {
    companion object {
        fun of(loop: LoopBase): LoopTimeWindow? {
            if (loop.startInDay < 0 || loop.endInDay < 0) return null

            val start = loop.startInDay
            val end = if (loop.startInDay > loop.endInDay) loop.endInDay + MS_1DAY else loop.endInDay

            val nowRaw = LocalTime.now().toMs()
            val now = if (loop.startInDay > loop.endInDay && nowRaw < start) nowRaw + MS_1DAY else nowRaw

            val totalMs = end - start
            if (totalMs <= 0) return null

            val remainMs = (end - now).coerceIn(0, totalMs)
            val remainMinutes = ceilToMinutes(remainMs)
            val totalMinutes = ceilToMinutes(totalMs)
            return LoopTimeWindow(
                totalMinutes = totalMinutes,
                elapsedMinutes = totalMinutes - remainMinutes,
                remainMinutes = remainMinutes,
            )
        }

        private fun ceilToMinutes(ms: Long): Int = ((ms + MS_1MIN - 1) / MS_1MIN).toInt()
    }
}