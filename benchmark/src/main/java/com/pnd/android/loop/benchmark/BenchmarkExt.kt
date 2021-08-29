package com.pnd.android.loop.benchmark

import androidx.annotation.IntRange
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule

const val TARGET_PACKAGE = "com.pnd.android.loop"
const val WAIT_UI = 100L

fun MacrobenchmarkRule.measureRepeated(
    metrics: List<Metric>,
    compilationMode: CompilationMode = CompilationMode.SpeedProfile(),
    startupMode: StartupMode? = null,
    @IntRange(from = 1)
    iterations: Int = 5,
    setupBlock: MacrobenchmarkScope.() -> Unit = {},
    measureBlock: MacrobenchmarkScope.() -> Unit
) = measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = metrics,
    compilationMode = compilationMode,
    startupMode = startupMode,
    iterations = iterations,
    setupBlock = setupBlock,
    measureBlock = measureBlock
)