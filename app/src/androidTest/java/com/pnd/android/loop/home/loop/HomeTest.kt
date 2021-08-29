package com.pnd.android.loop.home.loop

import android.app.AlarmManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pnd.android.loop.LooperSimpleApplication
import com.pnd.android.loop.MainActivity
import com.pnd.android.loop.TestTag
import com.pnd.android.loop.alarm.AlarmHelper
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.di.AppModule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@UninstallModules(AppModule::class)
@HiltAndroidTest
class HomeTest {
    @get:Rule
    val composerRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    lateinit var appDb: AppDatabase

    @BindValue
    lateinit var alarmHelper: AlarmHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<LooperSimpleApplication>()
        appDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        alarmHelper = AlarmHelper(
            context = context,
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
            appDb = appDb
        )
        hiltRule.inject()
    }



    @Test
    fun loopCard_ShouldBe_Created_By_DefaultValues() {
        composerRule
            .onNodeWithTag(TestTag.USER_INPUT_TEXT_FIELD)
            .performTextInput("Early in the morning!")

        composerRule
            .onNodeWithTag(TestTag.USER_INPUT_SUBMIT_BUTTON)
            .performClick()

        composerRule
            .onNodeWithText("Early in the morning!")
            .assertExists()
    }


}