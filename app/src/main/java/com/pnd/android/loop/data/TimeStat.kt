package com.pnd.android.loop.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.math.min

private val logger = Logger(tag = "TimeStat")

sealed class TimeStat {

    data object NotToday : TimeStat()
    class BeforeStart(val time: LocalTime) : TimeStat() {
        override fun asString(context: Context, isAbb: Boolean): String {
            return if (isAbb) abb(context) else full(context)
        }

        private fun abb(context: Context) = if (time.hour > 0) {
            context.getString(R.string.time_stat_before_start_hours, time.hour)
        } else if (time.minute >= 5) {
            context.getString(R.string.time_stat_before_start_mins, time.minute)
        } else {
            context.getString(R.string.time_stat_before_start_soon)
        }

        private fun full(context: Context) = if (time.hour > 0) {
            context.getString(R.string.time_stat_full_before_start_hours, time.hour)
        } else if (time.minute >= 5) {
            context.getString(R.string.time_stat_full_before_start_mins, time.minute)
        } else {
            context.getString(R.string.time_stat_full_before_start_soon)
        }
    }

    class InProgress(private val remain: LocalTime) : TimeStat() {
        override fun asString(context: Context, isAbb: Boolean): String {
            return if (isAbb) abb(context) else full(context)
        }

        private fun abb(context: Context) = if (remain.hour > 0) {
            context.getString(R.string.time_stat_remain_hours, remain.hour)
        } else {
            context.getString(R.string.time_stat_remain_mins, remain.minute)
        }

        private fun full(context: Context) = if (remain.hour > 0) {
            context.getString(R.string.time_stat_full_remain_hours, remain.hour)
        } else {
            context.getString(R.string.time_stat_full_remain_mins, remain.minute)
        }
    }

    data object Finished : TimeStat() {
        override fun asString(context: Context, isAbb: Boolean): String {
            return context.getString(R.string.finished)
        }
    }

    open fun asString(context: Context, isAbb: Boolean) = ""
    fun isPast(): Boolean {
        return this == Finished
    }

    fun isNotToday(): Boolean {
        return this == NotToday
    }
}


val LoopBase.currentTimeStat: TimeStat
    @Composable get() {
        var currTimeStat by remember(loopId) { mutableStateOf<TimeStat>(TimeStat.NotToday) }

        // Recompose 로 인해 매번 flow 가 생성 되는 것을 막고, 기존의  flow를 사용하도록 하기 위한 우회방법
        LaunchedEffect(loopId) { timeStatFlow.collect { timeStat -> currTimeStat = timeStat } }
        return currTimeStat
    }


private val LoopBase.timeStatFlow
    get() = flow {
        val startTime = startInDay.toLocalTime()
        val endTime = endInDay.toLocalTime()

        while (currentCoroutineContext().isActive) {
            val now = LocalTime.now()

            val delayInMs = when {
                !isActiveDay() -> none(
                    title = title
                )

                now.isAfter(endTime) -> after(
                    title = title
                )

                now.isBefore(startTime) -> before(
                    title = title,
                    startTime = startTime
                )

                else -> inProgress(
                    title = title,
                    endTime = endTime
                )
            }

            delay(delayInMs)
        }
    }

private suspend fun FlowCollector<TimeStat>.after(
    title: String
): Long {
    val now = LocalTime.now()
    emit(TimeStat.Finished)

    val delayInMs = min(MS_1MIN, now.until(LocalTime.MAX, ChronoUnit.MILLIS))
    logger.d { "[TimeStat] ($title) after delayInMs : $delayInMs" }

    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.before(
    title: String,
    startTime: LocalTime
): Long {
    val now = LocalTime.now()
    val afterMs = now.until(startTime, ChronoUnit.MILLIS)

    val delayInMs: Long = if (afterMs > 0) {
        emit(TimeStat.BeforeStart(afterMs.toLocalTime()))
        afterMs % MS_1MIN
    } else {
        1000L
    }

    logger.d { "[TimeStat] ($title) before delayInMs: $delayInMs" }
    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.inProgress(
    title: String,
    endTime: LocalTime
): Long {
    val now = LocalTime.now()
    val remainMs = now.until(endTime, ChronoUnit.MILLIS)

    val delayInMs: Long = if (remainMs > 0) {
        emit(TimeStat.InProgress(remainMs.toLocalTime()))
        remainMs % MS_1MIN
    } else {
        1000L
    }

    logger.d { "[TimeStat] ($title) inProgress delayInMs: $delayInMs" }
    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.none(
    title: String,
): Long {
    emit(TimeStat.NotToday)

    val now = LocalTime.now()
    val delayInMs = now.until(LocalTime.MAX, ChronoUnit.MILLIS)

    logger.d { "[TimeStat] ($title) none delayInMs: $delayInMs" }
    return delayInMs
}
