package com.pnd.android.loop.alarm.notification

import android.content.Context
import android.widget.RemoteViews
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.icons.icon

class LoopRemoteViewBuilder(
    context: Context,
    private val loop: LoopVo
) {

    val remoteViews = RemoteViews(
        context.packageName,
        R.layout.notification_loop
    )


    fun updateImage() {
        icon(loop.icon)



    }

}