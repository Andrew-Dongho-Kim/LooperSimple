package com.pnd.android.loop.data

import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.toLocalTime
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val logger = Logger(tag = "TimeStat")

sealed class TimeStat {
    class Before(after: LocalTime) : TimeStat()
    class InProgress(remain: LocalTime) : TimeStat()
    data object After : TimeStat()

    fun isPast():Boolean {
        return this == After
    }
}

fun LoopBase.timeStatAsFlow() = flow {
    val now = LocalTime.now()
    val startTime = loopStart.toLocalTime()
    val endTime = loopEnd.toLocalTime()

    while (currentCoroutineContext().isActive) {
        when {
            now.isAfter(endTime) -> after(title = title)
            now.isBefore(startTime) -> before(startTime = startTime)
            else -> inProgress(endTime = endTime)
        }
    }
}

private suspend fun FlowCollector<TimeStat>.after(title: String) {
    val now = LocalTime.now()
    emit(TimeStat.After)

    val delayInMs = now.until(LocalTime.MAX, ChronoUnit.MILLIS)
    logger.d { "[$title] ended and sleep : $delayInMs ms" }

    delay(delayInMs)
}

private suspend fun FlowCollector<TimeStat>.before(
    startTime: LocalTime
) {
    val now = LocalTime.now()
    val afterMs = now.until(startTime, ChronoUnit.MILLIS)

    emit(TimeStat.Before(afterMs.toLocalTime()))
    delay(MS_1MIN + (afterMs % MS_1MIN))
}

private suspend fun FlowCollector<TimeStat>.inProgress(
    endTime: LocalTime
) {
    val now = LocalTime.now()
    val remainMs = now.until(endTime, ChronoUnit.MILLIS)

    emit(TimeStat.InProgress(remainMs.toLocalTime()))
    delay(MS_1MIN + (remainMs % MS_1MIN))
}
