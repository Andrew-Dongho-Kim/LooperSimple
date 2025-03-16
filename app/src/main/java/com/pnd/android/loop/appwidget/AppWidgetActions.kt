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

val ACTION_PARAMS_ACTION = ActionParameters.Key<String>(PARAMS_ACTION)
val ACTION_PARAMS_LOOP_ID = ActionParameters.Key<Int>(PARAMS_LOOP_ID)
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
            loopId = parameters[ACTION_PARAMS_LOOP_ID] ?: -1
        )
    }
}

fun doneAction(loopId: Int) = action(action = Action.DONE_LOOP, loopId = loopId)
fun skipAction(loopId: Int) = action(action = Action.SKIP_LOOP, loopId = loopId)

fun startAction(loopId: Int) = action(action = Action.START_LOOP, loopId = loopId)
fun stopAction(loopId: Int) = action(action = Action.STOP_LOOP, loopId = loopId)

private fun action(
    @Action action: String,
    loopId: Int
) = actionRunCallback<AppWidgetLoopAction>(
    parameters = actionParametersOf(
        ACTION_PARAMS_ACTION to action,
        ACTION_PARAMS_LOOP_ID to loopId
    )
)
