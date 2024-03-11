package com.pnd.android.loop.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.room.Room
import com.pnd.android.loop.alarm.AlarmController
import com.pnd.android.loop.alarm.notification.NotificationHelper
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.MIGRATION_1_2
import com.pnd.android.loop.data.MIGRATION_2_3
import com.pnd.android.loop.data.MIGRATION_3_4
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideAppDb(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "app_db"
    ).addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .enableMultiInstanceInvalidation()
        .build()

    @Provides
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Provides
    fun provideNotificationHelper(
        @ApplicationContext context: Context,
        nm: NotificationManager
    ): NotificationHelper {
        return NotificationHelper(context, nm)
    }

    @Provides
    fun provideLoopRepository(
        appDb: AppDatabase,
        alarmController: AlarmController,
    ): LoopRepository {
        return LoopRepository(
            appDb = appDb,
            alarmController = alarmController
        )
    }

}