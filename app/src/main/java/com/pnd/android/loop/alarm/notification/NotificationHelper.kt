package com.pnd.android.loop.alarm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pnd.android.loop.R
import com.pnd.android.loop.alarm.AlarmHelper
import com.pnd.android.loop.data.Loop
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


const val CHANNEL_ID = "com.pnd.android.loop.SimpleLooper"

class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nm: NotificationManager
) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.loop),
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
    }

    private fun Loop.pendingIntent(action: String) = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AlarmHelper.AlarmReceiver::class.java).apply {
            this.action = action
            putToIntent(this)
        },
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun notify(loop: Loop) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle(loop.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(0)
            .setVibrate(null)
            .setSound(null)
            .setAutoCancel(true)

        nm.notify(loop.id, builder.build())
    }

    fun cancel(loop: Loop) {
        nm.cancel(loop.id)
    }
}