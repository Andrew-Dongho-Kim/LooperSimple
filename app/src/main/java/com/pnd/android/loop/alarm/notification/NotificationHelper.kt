package com.pnd.android.loop.alarm.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pnd.android.loop.HomeActivity
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action
import com.pnd.android.loop.appwidget.PARAMS_ACTION
import com.pnd.android.loop.appwidget.PARAMS_DATE
import com.pnd.android.loop.appwidget.PARAMS_LOOP_ID
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.elapsedMinutesSinceStart
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject


/** 진행 중인 루프를 소리/진동 없이 상시 표시하기 위한 알림 채널 */
const val CHANNEL_ID_ONGOING = "com.pnd.android.loop.LooperSimple.ongoing"

/**
 * 루프가 막 시작되었음을 화면 상단에 잠깐 띄우기(heads-up) 위한 알림 채널.
 * 상시 알림(무음)과 달리 IMPORTANCE_HIGH 라 화면이 켜져 있을 때 상단에 peek 된다.
 * 앱 톤에 맞춰 소리·진동은 끈다(중요도만 HIGH).
 */
const val CHANNEL_ID_LOOP_STARTED = "com.pnd.android.loop.LooperSimple.loop_started"

/**
 * 포그라운드 서비스가 소유하는 "진행 중 루프" 통합 알림의 고정 ID.
 * 루프 ID(양수 DB 자동 증가값)와 겹치지 않도록 별도의 상수를 사용한다.
 */
const val FOREGROUND_NOTIFICATION_ID = 0x10F0

/**
 * "루프 시작" 안내(heads-up) 알림의 고정 ID. 상시 알림 및 루프 ID와 겹치지 않도록
 * 별도의 상수를 쓴다. 매번 같은 ID 로 갱신하므로 안내 알림은 하나만 유지된다.
 */
const val LOOP_STARTED_NOTIFICATION_ID = 0x10F1

/**
 * "완료했나요?" 종료 확인 알림 ID의 기준값. 이 알림은 시작 안내와 달리 루프마다 따로 띄운다.
 * 액션 버튼(완료·건너뛰기)은 알림 전체에 적용되므로 대상이 하나로 명확해야 하고, 같은 시각에
 * 끝난 루프가 여러 개면 각각 물어야 하기 때문이다. 다른 알림 ID·루프 ID와 겹치지 않도록
 * 충분히 떨어진 기준값에 loopId 를 더해 쓴다.
 */
private const val LOOP_ENDED_NOTIFICATION_ID_BASE = 0x2000_0000

/** 시작 안내 알림이 알림창에 남는 시간(ms). 이후 스스로 사라져 상시 알림만 남는다. */
private const val LOOP_STARTED_TIMEOUT_MS = 60_000L

/**
 * 종료 확인 알림이 알림창에 남는 시간(ms). 시작 안내(1분)보다 넉넉히 두어 잠깐 자리를 비운
 * 사이에도 기록할 수 있게 하되, 무한정 남겨 두지는 않는다.
 */
private const val LOOP_ENDED_TIMEOUT_MS = 30 * 60_000L

/**
 * 해당 루프의 "완료했나요?" 종료 확인 알림을 내린다.
 *
 * 알림 액션 버튼을 눌러도 알림이 스스로 닫히지는 않고, 앱이나 위젯에서 답한 경우에는 알림이
 * 그 사실조차 모른다. 응답(완료/건너뛰기/정지)을 기록하는 모든 경로에서 이 함수를 불러,
 * 이미 답한 루프에 확인 알림이 남아 다시 묻는 일이 없게 한다.
 */
fun cancelLoopEndedNotification(context: Context, loopId: Int) {
    NotificationManagerCompat.from(context).cancel(LOOP_ENDED_NOTIFICATION_ID_BASE + loopId)
}

/** 펼친 알림에 한 번에 나열할 최대 루프 수. 초과분은 "외 N개"로 요약한다. */
private const val MAX_EXPANDED_ROWS = 4

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

        // IMPORTANCE_HIGH: 화면 상단에 잠깐 peek 되지만, 소리·진동은 끈다.
        val startedChannel = NotificationChannel(
            CHANNEL_ID_LOOP_STARTED,
            context.getString(R.string.notification_loop_started_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        nm.createNotificationChannel(startedChannel)
    }

    /**
     * 진행 중인 루프들을 하나로 묶은 상시(ongoing) 알림을 만든다. 포그라운드 서비스가
     * 이 알림을 소유하므로 사용자가 스와이프로 지울 수 없고, 앱을 실행하지 않아도
     * 루프가 진행되는 동안 알림창에 계속 남는다.
     *
     * - 루프 1개/여러 개: 동일한 커스텀 룩(루프별 색상 바 + 진행률)으로 표시
     * - 빈 목록(placeholder): 앱 이름만 표시(서비스가 곧 스스로 내림)
     */
    fun buildOngoingNotification(loops: List<LoopBase>): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ONGOING)
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setDefaults(0)
            .setVibrate(null)
            .setSound(null)
            .setShowWhen(false)
            // Android 12+ 는 포그라운드 서비스 알림 표시를 기본 10초 지연시킨다. 루프 시작을
            // 알리는 알림에 10초 지연은 치명적이라 즉시 표시로 바꾼다.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentIntent())

        when {
            loops.isEmpty() -> {
                builder.setContentTitle(context.getString(R.string.app_name))
            }

            else -> {
                // 단일/다중 모두 동일한 커스텀 룩(루프별 색상 바 + 진행률)을 쓴다. 커스텀
                // 뷰를 지원하지 않는 환경(웨어러블 등)을 위해 제목/본문 폴백도 채워 둔다.
                val single = loops.singleOrNull()
                builder
                    .setContentTitle(single?.title ?: loopsInProgressTitle(loops))
                    .setContentText(single?.let { lineText(it) } ?: loops.joinToString(", ") { it.title })
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(buildLoopsCollapsedView(loops))
                    .setCustomBigContentView(buildLoopsExpandedView(loops))

                // 단일 루프에는 앱을 열지 않고 응답할 수 있는 액션 버튼을 붙인다.
                if (single != null) addLoopActions(builder, single)
            }
        }

        return builder.build()
    }

    /** 이미 표시 중인 통합 알림 내용을 조용히 갱신한다. */
    fun updateOngoing(loops: List<LoopBase>) {
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildOngoingNotification(loops))
    }

    /**
     * 방금 시작된 루프를 화면 상단에 친절하게 알린다(heads-up). 상시 알림과 별개의
     * 1회성 안내로, 제목에 안내 문구를, 본문에 시간 정보를 담아 잠시 뒤 스스로 사라진다.
     *
     * 예) "아침 운동 시작할 시간이에요" / "11:20까지 · 32분 남음"
     */
    fun notifyLoopStarted(loop: LoopBase) {
        val builder = newAnnouncementBuilder()
            .setContentTitle(context.getString(R.string.notification_loop_started_title, loop.title))
            .setContentText(lineText(loop))
        // 방금 뜬 알림에서 바로 응답할 수 있게 액션 버튼을 붙인다. 이게 없으면 사용자가
        // 알림창을 내려 상시 알림을 찾아야 해서, 두 알림이 그냥 중복으로만 보인다.
        addLoopActions(builder, loop)
        nm.notify(LOOP_STARTED_NOTIFICATION_ID, builder.build())
    }

    /**
     * 같은 시각에 시작한 루프들을 하나로 묶어 알린다. 안내 알림은 ID 하나를 공유하므로
     * 각각 따로 띄우면 마지막 것만 남는다(=나머지 루프의 시작을 놓친다).
     *
     * 예) "루프 3개 시작할 시간이에요" + 각 루프의 시간 정보 나열
     */
    fun notifyLoopsStarted(loops: List<LoopBase>) {
        val single = loops.singleOrNull()
        if (single != null) {
            notifyLoopStarted(single)
            return
        }
        if (loops.isEmpty()) return

        val builder = newAnnouncementBuilder()
            .setContentTitle(
                context.resources.getQuantityString(
                    R.plurals.notification_loops_started_title,
                    loops.size,
                    loops.size,
                )
            )
        nm.notify(LOOP_STARTED_NOTIFICATION_ID, builder.withLoopLines(loops).build())
    }

    /**
     * 아직 진행 중인 루프들을 "진행 중" 안내 + 시간 정보로 알린다.
     * 화면이 다시 켜졌을 때, 그리고 anytime 루프가 오래 방치되지 않도록 1시간마다 호출된다.
     *
     * - 루프 1개: "○○ 진행 중이에요" / "11:20까지 · 32분 남음" (+ 응답 버튼)
     * - 루프 여러 개: "진행 중인 루프 N개" + 각 루프의 시간 정보 나열
     */
    fun notifyLoopsInProgress(loops: List<LoopBase>) {
        if (loops.isEmpty()) return

        val builder = newAnnouncementBuilder()
        val single = loops.singleOrNull()
        if (single != null) {
            builder.setContentTitle(
                context.getString(R.string.notification_loop_in_progress_title, single.title)
            ).setContentText(lineText(single))
            addLoopActions(builder, single)
        } else {
            builder.setContentTitle(loopsInProgressTitle(loops)).withLoopLines(loops)
        }
        nm.notify(LOOP_STARTED_NOTIFICATION_ID, builder.build())
    }

    /**
     * 시간창이 끝났는데도 아직 응답이 없는 루프에 "완료했나요?" 를 묻는다. 종료 시각이 지나면
     * 루프는 상시 알림에서 조용히 사라지므로(진행 중이 아니게 된다) 이 알림이 없으면 사용자는
     * 기록할 기회를 그대로 놓치고 미응답으로 남는다.
     *
     * 예) "아침 운동 완료했나요?" / "08:00에 종료됐어요" + [완료] · [건너뛰기]
     *
     * 시작 안내와 달리 루프마다 별개의 ID 로 띄운다. 같은 시각에 끝난 루프가 여러 개일 때
     * 하나로 묶으면 액션 버튼이 어느 루프를 가리키는지 알 수 없기 때문이다.
     */
    fun notifyLoopEnded(loop: LoopBase, occurrenceDate: LocalDate) {
        val builder = newAnnouncementBuilder(timeoutMs = LOOP_ENDED_TIMEOUT_MS)
            .setContentTitle(context.getString(R.string.notification_loop_ended_title, loop.title))
            .setContentText(
                context.getString(
                    R.string.notification_loop_ended_text,
                    loop.endInDay.toLocalTime().formatText(),
                )
            )
        addDoneSkipActions(builder, loop, occurrenceDate)
        nm.notify(LOOP_ENDED_NOTIFICATION_ID_BASE + loop.loopId, builder.build())
    }

    /** 여러 루프를 "제목 · 시간 정보" 한 줄씩 펼쳐 보여준다(접힘 상태에서는 제목만 나열). */
    private fun NotificationCompat.Builder.withLoopLines(
        loops: List<LoopBase>
    ): NotificationCompat.Builder {
        val inbox = NotificationCompat.InboxStyle()
        loops.forEach { loop -> inbox.addLine("${loop.title} · ${lineText(loop)}") }
        return setContentText(loops.joinToString(", ") { it.title }).setStyle(inbox)
    }

    /** 시작/진행 중/종료 확인 안내 알림에 공통으로 쓰는 heads-up 빌더(무음, 잠시 뒤 자동 소멸). */
    private fun newAnnouncementBuilder(
        timeoutMs: Long = LOOP_STARTED_TIMEOUT_MS,
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID_LOOP_STARTED)
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setTimeoutAfter(timeoutMs)
            .setDefaults(0)
            .setVibrate(null)
            .setSound(null)
            .setContentIntent(contentIntent())

    /**
     * 상시 알림에 앱을 열지 않고 응답할 수 있는 액션 버튼을 붙인다. 알림 액션은 라인별이
     * 아니라 알림 전체에 적용되므로, 대상이 하나로 명확한 단일 루프 알림에만 노출한다.
     *
     * - anytime 루프(진행 중) → [정지]
     * - 시간대 루프          → [완료] · [건너뛰기]
     */
    private fun addLoopActions(builder: NotificationCompat.Builder, loop: LoopBase) {
        if (loop.isAnyTime) {
            builder.addAction(
                R.drawable.stop,
                context.getString(R.string.notification_action_stop),
                loopActionIntent(loop.loopId, Action.STOP_LOOP),
            )
        } else {
            addDoneSkipActions(builder, loop)
        }
    }

    /**
     * [완료] · [건너뛰기] 한 쌍. 시간대 루프의 진행 중 알림과 종료 확인 알림이 함께 쓴다.
     *
     * [occurrenceDate] 를 주면 응답을 그 날짜에 기록한다. 이미 끝난 루프에 답하는 종료 확인
     * 알림은 누르는 시점이 occurrence 밖이라 날짜를 지금 기준으로 정할 수 없기 때문이다.
     */
    private fun addDoneSkipActions(
        builder: NotificationCompat.Builder,
        loop: LoopBase,
        occurrenceDate: LocalDate? = null,
    ) {
        builder.addAction(
            R.drawable.done,
            context.getString(R.string.notification_action_done),
            loopActionIntent(loop.loopId, Action.DONE_LOOP, occurrenceDate),
        )
        builder.addAction(
            R.drawable.skip,
            context.getString(R.string.notification_action_skip),
            loopActionIntent(loop.loopId, Action.SKIP_LOOP, occurrenceDate),
        )
    }

    /**
     * 접힌 알림: "N개 진행 중" + 가장 먼저 끝나는 루프의 종료 시각·진행률 요약.
     * 시간제 루프가 하나도 없으면(모두 anytime) 진행률 바를 감추고 루프 제목만 나열한다.
     */
    private fun buildLoopsCollapsedView(loops: List<LoopBase>): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.notification_loops_collapsed)
        val single = loops.singleOrNull()

        if (single != null) {
            // 단일 루프: 제목 + 그 루프의 시간 요약("11:20까지 · 32분 남음") + 진행률.
            view.setTextViewText(R.id.collapsed_title, single.title)
            view.setTextViewText(R.id.collapsed_subtitle, lineText(single))
            bindCollapsedProgress(view, single)
            return view
        }

        // 다중 루프: "N개 진행 중" + 가장 임박한 루프 기준 요약/진행률.
        view.setTextViewText(R.id.collapsed_title, loopsInProgressTitle(loops))
        val nearest = nearestEndingLoop(loops)
        if (nearest != null) {
            view.setTextViewText(
                R.id.collapsed_subtitle,
                context.getString(
                    R.string.notification_next_end,
                    nearest.endInDay.toLocalTime().formatText(),
                ),
            )
            bindCollapsedProgress(view, nearest)
        } else {
            // 시간제 루프가 하나도 없으면(모두 anytime) 진행률을 감추고 제목만 나열한다.
            view.setTextViewText(R.id.collapsed_subtitle, loops.joinToString(", ") { it.title })
            view.setViewVisibility(R.id.collapsed_progress, View.GONE)
        }
        return view
    }

    /** 접힘 뷰의 진행률 바를 해당 루프에 맞춰 채운다. anytime(시간 창 없음)이면 감춘다. */
    private fun bindCollapsedProgress(view: RemoteViews, loop: LoopBase) {
        val window = LoopTimeWindow.of(loop)
        if (window == null) {
            view.setViewVisibility(R.id.collapsed_progress, View.GONE)
            return
        }
        view.setViewVisibility(R.id.collapsed_progress, View.VISIBLE)
        view.setProgressBar(R.id.collapsed_progress, window.totalMinutes, window.elapsedMinutes, false)
        view.setProgressColor(R.id.collapsed_progress, loop.color.opaque())
    }

    /**
     * 펼친 알림: 종료가 임박한 시간제 루프부터, anytime 루프는 뒤로 정렬해 한 줄씩 쌓는다.
     * 표시 한도([MAX_EXPANDED_ROWS])를 넘으면 마지막에 "외 N개"를 덧붙인다.
     */
    private fun buildLoopsExpandedView(loops: List<LoopBase>): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.notification_loops_expanded)
        view.removeAllViews(R.id.loops_container)

        val ordered = loops.sortedWith(
            compareBy(
                { it.isAnyTime },
                { LoopTimeWindow.of(it)?.remainMinutes ?: Int.MAX_VALUE },
            )
        )

        // 루프가 하나일 때는 알림 액션 버튼(완료·건너뛰기)이 이미 붙으므로 줄 버튼을 감춘다.
        val showRowAction = loops.size > 1
        ordered.take(MAX_EXPANDED_ROWS).forEach { loop ->
            view.addView(R.id.loops_container, buildLoopRow(loop, showRowAction))
        }

        val hidden = ordered.size - MAX_EXPANDED_ROWS
        if (hidden > 0) {
            val more = RemoteViews(context.packageName, R.layout.notification_loops_more)
            more.setTextViewText(
                R.id.loops_more,
                context.resources.getQuantityString(
                    R.plurals.notification_more_loops,
                    hidden,
                    hidden,
                ),
            )
            view.addView(R.id.loops_container, more)
        }
        return view
    }

    /** 펼친 알림의 루프 한 줄. anytime 루프는 종료 시각·진행률이 없어 경과 시간만 보여준다. */
    private fun buildLoopRow(loop: LoopBase, showAction: Boolean): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.notification_loop_row)
        row.setTextViewText(R.id.loop_row_title, loop.title)
        row.setInt(R.id.loop_row_accent, "setBackgroundColor", loop.color.opaque())
        bindRowAction(row, loop, showAction)

        if (loop.isAnyTime) {
            row.setTextViewText(R.id.loop_row_remaining, timePassed(loop))
            row.setViewVisibility(R.id.loop_row_progress, View.GONE)
            row.setViewVisibility(R.id.loop_row_end, View.GONE)
            return row
        }

        row.setTextViewText(R.id.loop_row_remaining, timeLeftText(loop))
        row.setTextViewText(
            R.id.loop_row_end,
            context.getString(
                R.string.notification_until_time,
                loop.endInDay.toLocalTime().formatText(),
            ),
        )
        row.setViewVisibility(R.id.loop_row_end, View.VISIBLE)

        val window = LoopTimeWindow.of(loop)
        if (window != null) {
            row.setViewVisibility(R.id.loop_row_progress, View.VISIBLE)
            row.setProgressBar(R.id.loop_row_progress, window.totalMinutes, window.elapsedMinutes, false)
            row.setProgressColor(R.id.loop_row_progress, loop.color.opaque())
        } else {
            row.setViewVisibility(R.id.loop_row_progress, View.GONE)
        }
        return row
    }

    /**
     * 줄마다 붙는 응답 버튼. anytime 루프는 [정지], 시간대 루프는 [완료]로 대상을 하나로
     * 좁힌다(건너뛰기까지 넣으면 한 줄에 아이콘이 몰려 좁은 알림에서 눌리기 어렵다).
     */
    private fun bindRowAction(row: RemoteViews, loop: LoopBase, showAction: Boolean) {
        if (!showAction) {
            row.setViewVisibility(R.id.loop_row_action, View.GONE)
            return
        }

        val isStop = loop.isAnyTime
        val labelRes =
            if (isStop) R.string.notification_action_stop else R.string.notification_action_done
        row.setViewVisibility(R.id.loop_row_action, View.VISIBLE)
        row.setImageViewResource(R.id.loop_row_action, if (isStop) R.drawable.stop else R.drawable.done)
        // 알림 배경(라이트/다크)에 상관없이 보이도록 루프 색으로 칠한다.
        row.setInt(R.id.loop_row_action, "setColorFilter", loop.color.opaque())
        row.setContentDescription(R.id.loop_row_action, context.getString(labelRes))
        row.setOnClickPendingIntent(
            R.id.loop_row_action,
            loopActionIntent(loop.loopId, if (isStop) Action.STOP_LOOP else Action.DONE_LOOP),
        )
    }

    private fun loopsInProgressTitle(loops: List<LoopBase>): String =
        context.resources.getQuantityString(
            R.plurals.notification_loops_in_progress,
            loops.size,
            loops.size,
        )

    /** 종료가 가장 임박한 시간제 루프. 모두 anytime 이면 null. */
    private fun nearestEndingLoop(loops: List<LoopBase>): LoopBase? =
        loops.filterNot { it.isAnyTime }
            .minByOrNull { LoopTimeWindow.of(it)?.remainMinutes ?: Int.MAX_VALUE }

    /** "32분 남음" 한 조각. 시간 창을 계산할 수 없으면 빈 문자열. */
    private fun timeLeftText(loop: LoopBase): String {
        val window = LoopTimeWindow.of(loop) ?: return ""
        return context.resources.getQuantityString(
            R.plurals.notification_time_left,
            window.remainMinutes,
            window.remainMinutes,
        )
    }

    /** loop.color 는 반투명일 수 있어, 알림 배경 위에서 흐릿하지 않도록 완전 불투명으로 만든다. */
    private fun Int.opaque(): Int = this or 0xFF000000.toInt()

    /**
     * ProgressBar 를 루프 색으로 칠한다. 틴트 API 가 없는 구버전(API < 31)에서는 기본색으로
     * 두어도 색 구분은 왼쪽 액센트 바가 담당하므로 문제 없다.
     */
    private fun RemoteViews.setProgressColor(viewId: Int, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColorStateList(viewId, "setProgressTintList", ColorStateList.valueOf(color))
        }
    }

    /**
     * 액션 버튼 하나에 대응하는 PendingIntent. Intent 동등성은 extra 를 보지 않으므로,
     * 루프·액션마다 고유한 requestCode 를 부여해 서로 덮어쓰지 않게 한다.
     */
    private fun loopActionIntent(
        loopId: Int,
        @Action action: String,
        occurrenceDate: LocalDate? = null,
    ): PendingIntent {
        val intent = Intent(context, LoopNotificationActionReceiver::class.java).apply {
            putExtra(PARAMS_ACTION, action)
            putExtra(PARAMS_LOOP_ID, loopId)
            occurrenceDate?.let { date -> putExtra(PARAMS_DATE, date.toMs()) }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(loopId, action, dated = occurrenceDate != null),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * [dated] 는 날짜를 실은 종료 확인용 액션을 진행 중 알림용과 구분한다. Intent 동등성은
     * extra 를 보지 않으므로 requestCode 를 공유하면 FLAG_UPDATE_CURRENT 로 서로의 extra 를
     * 덮어써, 한쪽 버튼이 엉뚱한 날짜에 기록하게 된다.
     */
    private fun requestCode(loopId: Int, @Action action: String, dated: Boolean): Int {
        val actionCode = when (action) {
            Action.DONE_LOOP -> 1
            Action.SKIP_LOOP -> 2
            Action.START_LOOP -> 3
            Action.STOP_LOOP -> 4
            else -> 0
        }
        return loopId * 10 + if (dated) actionCode + 5 else actionCode
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
        return if (loop.isAnyTime) {
            timePassed(loop)
        } else {
            timeRemained(loop)
        }
    }

    /**
     * "32분 경과" / "3시간 경과". 자정을 넘겨 이어지는 anytime 루프(예: 어제 23:00 시작)도
     * 하루 둘레로 계산하므로 음수가 나오지 않는다. 통계 화면용 time_stat_* 문자열은 "#"
     * 접두사가 붙어 있어 알림에 그대로 쓸 수 없으므로 알림 전용 문자열을 쓴다.
     */
    private fun timePassed(loop: LoopBase): String {
        val elapsed = loop.elapsedMinutesSinceStart() ?: return ""
        val hours = elapsed / 60
        return if (hours > 0) {
            context.resources.getQuantityString(
                R.plurals.notification_time_passed_hours,
                hours,
                hours,
            )
        } else {
            context.resources.getQuantityString(
                R.plurals.notification_time_passed_mins,
                elapsed,
                elapsed,
            )
        }
    }

    private fun timeRemained(loop: LoopBase): String {
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

    private fun LocalTime.formatText() =
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
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
            val end =
                if (loop.startInDay > loop.endInDay) loop.endInDay + MS_1DAY else loop.endInDay

            val nowRaw = LocalTime.now().toMs()
            val now =
                if (loop.startInDay > loop.endInDay && nowRaw < start) nowRaw + MS_1DAY else nowRaw

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
