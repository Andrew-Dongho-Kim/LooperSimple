package com.pnd.android.loop.data

import android.content.Context
import com.pnd.android.loop.R
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.isActiveDay
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

    data object NotToday : TimeStat()
    class BeforeStart(val time: LocalTime) : TimeStat() {
        override fun asString(context: Context, isAbb: Boolean): String {
            return if (isAbb) abb(context) else full(context)
        }

        private fun abb(context: Context) = if (time.hour > 0) {
            context.getString(R.string.time_stat_before_start_hours, time.hour)
        } else if (time.minute > 0) {
            context.getString(R.string.time_stat_before_start_mins, time.minute)
        } else {
            context.getString(R.string.time_stat_before_start_soon)
        }

        private fun full(context: Context) = ""
    }

    class InProgress(val remain: LocalTime) : TimeStat() {
        override fun asString(context: Context, isAbb: Boolean): String {
            return if (isAbb) abb(context) else full(context)
        }

        private fun abb(context: Context) = if (remain.hour > 0) {
            context.getString(R.string.time_stat_remain_hours, remain.hour)
        } else {
            context.getString(R.string.time_stat_remain_mins, remain.minute)
        }

        private fun full(context: Context) = ""
    }

    data object Finished : TimeStat()

    open fun asString(context: Context, isAbb: Boolean) = ""
    fun isPast(): Boolean {
        return this == Finished
    }

    fun isNotToday(): Boolean {
        return this == NotToday
    }
}

fun LoopBase.timeStatAsFlow() = flow {
    val startTime = loopStart.toLocalTime()
    val endTime = loopEnd.toLocalTime()

    while (currentCoroutineContext().isActive) {
        val now = LocalTime.now()

        when {
            !isActiveDay() -> none()
            now.isAfter(endTime) -> after(title = title)
            now.isBefore(startTime) -> before(startTime = startTime)
            else -> inProgress(endTime = endTime)
        }
    }
}

private suspend fun FlowCollector<TimeStat>.after(title: String) {
    val now = LocalTime.now()
    emit(TimeStat.Finished)

    val delayInMs = now.until(LocalTime.MAX, ChronoUnit.MILLIS)
    logger.d { "[$title] ended and sleep : $delayInMs ms" }

    delay(delayInMs)
}

private suspend fun FlowCollector<TimeStat>.before(
    startTime: LocalTime
) {
    val now = LocalTime.now()
    val afterMs = now.until(startTime, ChronoUnit.MILLIS)

    if (afterMs > 0) {
        emit(TimeStat.BeforeStart(afterMs.toLocalTime()))
        delay(MS_1MIN + (afterMs % MS_1MIN))
    } else {
        delay(1000)
    }
}

private suspend fun FlowCollector<TimeStat>.inProgress(
    endTime: LocalTime
) {
    val now = LocalTime.now()
    val remainMs = now.until(endTime, ChronoUnit.MILLIS)

    if (remainMs > 0) {
        emit(TimeStat.InProgress(remainMs.toLocalTime()))
        delay(MS_1MIN + (remainMs % MS_1MIN))
    } else {
        delay(1000)
    }
}

private suspend fun FlowCollector<TimeStat>.none() {
    emit(TimeStat.NotToday)

    val now = LocalTime.now()
    val delayInMs = now.until(LocalTime.MAX, ChronoUnit.MILLIS)

    delay(delayInMs)
}
