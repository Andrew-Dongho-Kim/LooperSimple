package com.pnd.android.loop.alarm.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pnd.android.loop.HomeActivity
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject


/** 진행 중인 루프를 소리/진동 없이 상시 표시하기 위한 알림 채널 */
const val CHANNEL_ID_ONGOING = "com.pnd.android.loop.LooperSimple.ongoing"

/**
 * 포그라운드 서비스가 소유하는 "진행 중 루프" 통합 알림의 고정 ID.
 * 루프 ID(양수 DB 자동 증가값)와 겹치지 않도록 별도의 상수를 사용한다.
 */
const val FOREGROUND_NOTIFICATION_ID = 0x10F0

class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nm: NotificationManager
) {

    init {
        // IMPORTANCE_LOW: 상태바/알림창에는 뜨지만 소리·진동은 울리지 않는다(완전 무음).
        val channel = NotificationChannel(
            CHANNEL_ID_ONGOING,
            context.getString(R.string.notification_ongoing_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    /**
     * 진행 중인 루프들을 하나로 묶은 상시(ongoing) 알림을 만든다. 포그라운드 서비스가
     * 이 알림을 소유하므로 사용자가 스와이프로 지울 수 없고, 앱을 실행하지 않아도
     * 루프가 진행되는 동안 알림창에 계속 남는다.
     *
     * - 루프 1개: 제목 + "11:20까지 · 32분 남음" + 진행률 바
     * - 루프 여러 개: "진행 중인 루프 N개" + 각 루프를 나열(InboxStyle)
     * - 빈 목록(placeholder): 앱 이름만 표시(서비스가 곧 스스로 내림)
     */
    fun buildOngoingNotification(loops: List<LoopBase>): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ONGOING)
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setDefaults(0)
            .setVibrate(null)
            .setSound(null)
            .setShowWhen(false)
            .setContentIntent(contentIntent())

        when {
            loops.isEmpty() -> {
                builder.setContentTitle(context.getString(R.string.app_name))
            }

            loops.size == 1 -> {
                val loop = loops.first()
                builder.setContentTitle(loop.title)
                    .setContentText(lineText(loop))
                LoopTimeWindow.of(loop)?.let { window ->
                    builder.setProgress(window.totalMinutes, window.elapsedMinutes, false)
                }
            }

            else -> {
                val inbox = NotificationCompat.InboxStyle()
                loops.forEach { loop -> inbox.addLine("${loop.title} · ${lineText(loop)}") }
                builder.setContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.notification_loops_in_progress,
                        loops.size,
                        loops.size,
                    )
                )
                    .setContentText(loops.joinToString(", ") { it.title })
                    .setStyle(inbox)
            }
        }

        return builder.build()
    }

    /** 이미 표시 중인 통합 알림 내용을 조용히 갱신한다. */
    fun updateOngoing(loops: List<LoopBase>) {
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildOngoingNotification(loops))
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            // FLAG_IMMUTABLE is mandatory on Android 12+ (API 31).
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** "11:20까지 · 32분 남음" 한 줄. 남은 시간 계산이 불가하면 종료 시각만 보여준다. */
    private fun lineText(loop: LoopBase): String {
        val untilText = context.getString(
            R.string.notification_until_time,
            loop.endInDay.toLocalTime().formatText(),
        )
        val window = LoopTimeWindow.of(loop) ?: return untilText
        val timeLeftText = context.resources.getQuantityString(
            R.plurals.notification_time_left,
            window.remainMinutes,
            window.remainMinutes,
        )
        return "$untilText · $timeLeftText"
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
