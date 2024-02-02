package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

val ACTION_PARAMS_LOOP_ID = ActionParameters.Key<Int>("params_loop_id")

class AppWidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        AppWidgetUpdateWorker.updateWidget(context)
    }
}

class AppWidgetDoneAction: ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        AppWidgetUpdateWorker.doneLoop(
            context = context,
            loopId = parameters[ACTION_PARAMS_LOOP_ID] ?: -1
        )
    }
}

class AppWidgetSkipAction:ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        AppWidgetUpdateWorker.skipLoop(
            context = context,
            loopId = parameters[ACTION_PARAMS_LOOP_ID] ?: -1
        )
    }
}