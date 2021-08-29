package com.pnd.android.loop.benchmark

import android.content.Intent
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun measureStartup() = benchmarkRule.measureRepeated(
        metrics = listOf(StartupTimingMetric()),
        startupMode = StartupMode.COLD
    ) {
        pressHome()

        startActivityAndWait(Intent(Intent.ACTION_MAIN).apply {
            setPackage(TARGET_PACKAGE)
            addCategory(Intent.CATEGORY_LAUNCHER)
        })
    }

}