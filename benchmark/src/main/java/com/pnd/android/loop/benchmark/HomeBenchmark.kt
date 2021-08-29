package com.pnd.android.loop.benchmark

import android.content.Context
import android.content.Intent
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.pnd.android.loop.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class HomeBenchmark {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()


    @Test
    fun measureCreateLoop() = benchmarkRule.measureRepeated(
        metrics = listOf(FrameTimingMetric()),
        setupBlock = {
            startActivityAndWait(Intent(Intent.ACTION_MAIN).apply {
                setPackage(TARGET_PACKAGE)
                addCategory(Intent.CATEGORY_LAUNCHER)
            })
        }
    ) {
//        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//        val userInput = device.findObject(By.text(context.getString(R.string.add)))
//
//
//        userInput.click(WAIT_UI)


    }

}