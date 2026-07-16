package com.pnd.android.loop.alarm.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action
import com.pnd.android.loop.appwidget.PARAMS_ACTION
import com.pnd.android.loop.appwidget.PARAMS_LOOP_ID
import com.pnd.android.loop.common.log

/**
 * 상시 알림(포그라운드 서비스)의 액션 버튼(완료/건너뛰기/정지)을 처리한다.
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

        logger.i { "notification action:$action loopId:$loopId" }
        AppWidgetUpdateWorker.actionLoop(
            context = context,
            action = action,
            loopId = loopId,
        )
    }
}
