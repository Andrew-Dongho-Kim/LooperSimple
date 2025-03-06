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
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
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
    class BeforeStart(
        val time: LocalTime,
        val isAnyTime: Boolean,
    ) : TimeStat() {
        override fun asString(context: Context, isAbb: Boolean): String {
            return if (isAbb) abb(context) else full(context)
        }

        private fun abb(context: Context) = when {
            isAnyTime -> context.getString(R.string.anytime)
            time.hour > 0 -> context.getString(R.string.time_stat_before_start_hours, time.hour)
            time.minute >= 5 -> context.getString(R.string.time_stat_before_start_mins, time.minute)
            else -> context.getString(R.string.time_stat_before_start_soon)
        }

        private fun full(context: Context) = when {
            isAnyTime -> context.getString(R.string.anytime)
            time.hour > 0 -> context.getString(
                R.string.time_stat_full_before_start_hours,
                time.hour
            )

            time.minute >= 5 -> context.getString(
                R.string.time_stat_full_before_start_mins,
                time.minute
            )

            else -> context.getString(R.string.time_stat_full_before_start_soon)
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

    data class Finished(
        private val startTime: LocalTime,
        private val endTime: LocalTime,
        private val isAnyTime: Boolean,
    ) : TimeStat() {
        override fun asString(context: Context, isAbb: Boolean): String {
            if (isAnyTime) {
                return context.getString(R.string.finished)
            }
            return context.getString(R.string.finished)
        }
    }

    open fun asString(context: Context, isAbb: Boolean) = ""
    fun isPast(): Boolean {
        return this is Finished
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
        val startTime = if (isAnyTime) LocalTime.MIN else startInDay.toLocalTime()
        val endTime = if (isAnyTime) LocalTime.MIN else endInDay.toLocalTime()

        while (currentCoroutineContext().isActive) {
            val delayInMs = when {

                !isActiveDay() -> none(
                    title = title
                )

                isFinished() -> finished(
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    isAnyTime = isAnyTime
                )

                isBeforeStart() -> before(
                    title = title,
                    startTime = startTime,
                    isAnyTime = isAnyTime
                )

                else -> inProgress(
                    title = title,
                    endTime = endTime
                )
            }

            delay(delayInMs)
        }
    }

private fun LoopBase.isBeforeStart(): Boolean {
    if (isAnyTime) return startInDay == ANY_TIME

    val now = LocalTime.now()
    val startTime = startInDay.toLocalTime()
    return now.isBefore(startTime)
}

private fun LoopBase.isFinished(): Boolean {
    if (isAnyTime) return startInDay != ANY_TIME && endInDay != ANY_TIME

    val now = LocalTime.now()
    val endTime = endInDay.toLocalTime()
    return now.isAfter(endTime)
}

private suspend fun FlowCollector<TimeStat>.finished(
    title: String,
    startTime: LocalTime,
    endTime: LocalTime,
    isAnyTime: Boolean
): Long {
    val now = LocalTime.now()
    emit(
        TimeStat.Finished(
            startTime = startTime,
            endTime = endTime,
            isAnyTime = isAnyTime,
        )
    )

    val delayInMs = min(MS_1MIN, now.until(LocalTime.MAX, ChronoUnit.MILLIS))
    logger.d { "[TimeStat] ($title) after delayInMs : $delayInMs" }

    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.before(
    title: String,
    startTime: LocalTime,
    isAnyTime: Boolean,
): Long {
    val now = LocalTime.now()
    val afterMs = if (isAnyTime) 1000L else now.until(startTime, ChronoUnit.MILLIS)

    val delayInMs: Long = if (afterMs > 0) {
        emit(
            TimeStat.BeforeStart(
                time = afterMs.toLocalTime(),
                isAnyTime = isAnyTime,
            )
        )
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
