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
import dagger.hilt.android.qualifiers.ApplicationContext
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
        PendingIntent.FLAG_UPDATE_CURRENT
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

    fun cancel(loop: LoopBase) {
        nm.cancel(loop.loopId)
    }
}