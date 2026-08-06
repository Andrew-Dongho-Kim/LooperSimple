package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker.Companion.Action
import com.pnd.android.loop.ui.ARGS_NAVIGATE_ACTION

const val PARAMS_UPDATE_LOOPS = "params_update_loops"
const val PARAMS_ACTION = "params_action"
const val PARAMS_LOOP_ID = "params_loop_id"

/**
 * 응답(완료/건너뛰기)을 기록할 occurrence 의 날짜(에폭 ms). 0 이면 "지금" 기준으로 정한다.
 *
 * 누르는 시점에 다시 계산하면 늦는 경우가 있어, 화면을 만들 때 정한 날짜를 함께 넘긴다.
 *  - "완료했나요?" 알림: 누르는 시점이 이미 occurrence 밖이다.
 *  - 위젯의 어젯밤 몫 줄: 자정을 넘긴 루프(예: 22:00~06:30)가 오늘 아침에 끝난 몫이라,
 *    지금 기준으로 정하면 오늘 행에 잘못 기록된다([WidgetLoop] 참고).
 */
const val PARAMS_DATE = "params_date"

val ACTION_PARAMS_ACTION = ActionParameters.Key<String>(PARAMS_ACTION)
val ACTION_PARAMS_LOOP_ID = ActionParameters.Key<Int>(PARAMS_LOOP_ID)
val ACTION_PARAMS_DATE = ActionParameters.Key<Long>(PARAMS_DATE)
val ACTION_PARAM_NAVIGATE = ActionParameters.Key<String>(ARGS_NAVIGATE_ACTION)

class AppWidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        AppWidgetUpdateWorker.updateWidget(context)
    }
}

class AppWidgetLoopAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        AppWidgetUpdateWorker.actionLoop(
            context = context,
            action = parameters[ACTION_PARAMS_ACTION] ?: Action.DO_NOTHING,
            loopId = parameters[ACTION_PARAMS_LOOP_ID] ?: -1,
            date = parameters[ACTION_PARAMS_DATE] ?: 0L,
        )
    }
}

// dateMs = 0 이면 워커가 지금 기준으로 기록할 날짜를 정한다([PARAMS_DATE]).
fun doneAction(loopId: Int, dateMs: Long = 0L) =
    action(action = Action.DONE_LOOP, loopId = loopId, dateMs = dateMs)

fun skipAction(loopId: Int, dateMs: Long = 0L) =
    action(action = Action.SKIP_LOOP, loopId = loopId, dateMs = dateMs)

fun startAction(loopId: Int) = action(action = Action.START_LOOP, loopId = loopId)
fun stopAction(loopId: Int) = action(action = Action.STOP_LOOP, loopId = loopId)

private fun action(
    @Action action: String,
    loopId: Int,
    dateMs: Long = 0L,
) = actionRunCallback<AppWidgetLoopAction>(
    parameters = actionParametersOf(
        ACTION_PARAMS_ACTION to action,
        ACTION_PARAMS_LOOP_ID to loopId,
        ACTION_PARAMS_DATE to dateMs,
    )
)
