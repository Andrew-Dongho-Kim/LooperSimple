package com.pnd.android.loop.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.room.Room
import com.pnd.android.loop.alarm.LoopScheduler
import com.pnd.android.loop.alarm.notification.NotificationHelper
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.ui.home.group.LoopGroupRepository
import com.pnd.android.loop.ui.home.viewmodel.LoopRepository
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
    )
        .fallbackToDestructiveMigrationOnDowngrade()
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
        alarmController: LoopScheduler,
    ): LoopRepository {
        return LoopRepository(
            appDb = appDb,
            alarmController = alarmController
        )
    }

    @Provides
    fun provideLoopGroupRepository(
        appDb: AppDatabase
    ): LoopGroupRepository {
        return LoopGroupRepository(appDb = appDb)
    }

}