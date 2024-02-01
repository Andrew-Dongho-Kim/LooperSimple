package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

fun enqueueUpdateWidget(context: Context) {
    val request = OneTimeWorkRequestBuilder<AppWidgetUpdateWorker>()
        .build()

    WorkManager.getInstance(context).enqueue(request)
}

class AppWidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        enqueueUpdateWidget(context)
    }
}