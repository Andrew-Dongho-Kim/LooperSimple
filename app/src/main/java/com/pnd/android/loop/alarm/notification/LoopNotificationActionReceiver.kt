package com.pnd.android.loop.alarm.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action
import com.pnd.android.loop.appwidget.PARAMS_ACTION
import com.pnd.android.loop.appwidget.PARAMS_DATE
import com.pnd.android.loop.appwidget.PARAMS_LOOP_ID
import com.pnd.android.loop.common.log

/**
 * 알림의 액션 버튼(완료/건너뛰기/정지)을 처리한다. 상시 알림과 "완료했나요?" 종료 확인
 * 알림이 같은 리시버를 쓴다.
 *
 * 위젯 버튼과 완전히 동일한 경로(AppWidgetUpdateWorker.actionLoop)로 위임하므로
 * done 상태 기록 → 위젯 갱신 → 상시 알림 갱신이 한 번에 이뤄진다. 덕분에 사용자는
 * 앱을 열지 않고 알림창에서 바로 루프를 완료/건너뛰기/정지할 수 있다.
 */
class LoopNotificationActionReceiver : BroadcastReceiver() {

    private val logger = log("LoopNotificationActionReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(PARAMS_ACTION) ?: Action.DO_NOTHING
        val loopId = intent.getIntExtra(PARAMS_LOOP_ID, -1)
        if (loopId == -1 || action == Action.DO_NOTHING) return

        // 종료 확인 알림은 기록할 날짜를 함께 실어 보낸다(이미 끝난 occurrence 라 지금 기준으로
        // 정할 수 없다). 상시 알림에는 없는 extra 라, 없으면 0 으로 넘겨 워커가 스스로 정한다.
        val date = intent.getLongExtra(PARAMS_DATE, 0L)
        logger.i { "notification action:$action loopId:$loopId date:$date" }

        // 응답 기록은 WorkManager 를 거치므로 몇 백 ms 뒤에 반영된다. "완료했나요?" 확인
        // 알림은 방금 사용자가 누른 그 알림이므로, 기다리지 않고 즉시 내려 눌린 티를 낸다.
        cancelLoopEndedNotification(context = context, loopId = loopId)

        AppWidgetUpdateWorker.actionLoop(
            context = context,
            action = action,
            loopId = loopId,
            date = date,
        )
    }
}
