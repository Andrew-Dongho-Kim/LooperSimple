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
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.isTimeInLoop
import com.pnd.android.loop.util.occurrenceStartDate
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

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
            isAnyTime -> ""
            time.hour > 0 -> context.getString(R.string.time_stat_before_start_hours, time.hour)
            time.minute >= 5 -> context.getString(R.string.time_stat_before_start_mins, time.minute)
            else -> context.getString(R.string.time_stat_before_start_soon)
        }

        private fun full(context: Context) = when {
            isAnyTime -> ""
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

    class InProgress(
        private val time: LocalTime,
        private val isAnyTime: Boolean,
    ) : TimeStat() {
        override fun asString(context: Context, isAbb: Boolean): String {
            return if (isAbb) abb(context) else full(context)
        }

        private fun abb(context: Context) = if (time.hour > 0) {
            context.getString(
                if (isAnyTime) R.string.time_stat_passed_hours else R.string.time_stat_remain_hours,
                time.hour
            )
        } else {
            context.getString(
                if (isAnyTime) R.string.time_stat_passed_mins else R.string.time_stat_remain_mins,
                time.minute
            )
        }

        private fun full(context: Context) = if (time.hour > 0) {
            context.getString(
                if (isAnyTime) R.string.time_stat_full_passed_hours else R.string.time_stat_full_remain_hours,
                time.hour
            )
        } else {
            context.getString(
                if (isAnyTime) R.string.time_stat_full_passed_mins else R.string.time_stat_full_remain_mins,
                time.minute
            )
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
        // 시간관련 변경이 있을 경우, time stat flow가 재 실행 되도록 해야 한다.
        LaunchedEffect(loopId, startInDay, endInDay, isAnyTime) {
            timeStatFlow.collect { timeStat -> currTimeStat = timeStat }
        }
        return currTimeStat
    }


private val LoopBase.timeStatFlow
    get(): Flow<TimeStat> {
        val loop = this
        return flow {
            while (currentCoroutineContext().isActive) {
                val now = LocalDateTime.now()
                val nowMs = now.toLocalTime().toMs()

                val delayInMs = when {
                    // 지금 시점 기준으로 이 루프의 occurrence가 없는 날이면 표시할 상태가 없다.
                    // (자정을 넘겨 이어지는 아침 구간이면 "어제" 기준으로 활성 요일을 판정한다.)
                    !loop.isActiveDay(loop.occurrenceStartDate(now)) -> none(title = loop.title)

                    // AnyTime 루프의 진행 상태는 시각이 아니라 시작/정지(done) 기록으로 판단한다.
                    loop.isAnyTime -> anyTimeStat(loop = loop)

                    // 진행 중: 시각 창 안(자정을 넘기는 창은 [start,24h)∪[0,end)).
                    loop.isTimeInLoop(nowMs) -> inProgress(
                        title = loop.title,
                        remainMs = loop.msRemainingUntilEnd(nowMs),
                    )

                    // 아직 시작 전: 오늘(또는 오늘 밤) 시작 예정.
                    nowMs < loop.startInDay -> before(
                        title = loop.title,
                        afterMs = loop.startInDay - nowMs,
                    )

                    // 그 밖: 일반 루프가 종료 시각을 지나 끝난 상태.
                    else -> finished(
                        title = loop.title,
                        // actual 시각은 아직 기록되지 않았을 때 ANY_TIME(-1)일 수 있으므로,
                        // toLocalTime() 이 음수로 크래시하지 않도록 MIN/MAX 로 가드한다.
                        startTime = if (loop.actualStartInDay < 0) LocalTime.MIN else loop.actualStartInDay.toLocalTime(),
                        endTime = if (loop.actualEndInDay < 0) LocalTime.MAX else loop.actualEndInDay.toLocalTime(),
                    )
                }

                delay(delayInMs.milliseconds)
            }
        }
    }

/** 진행 중인 시각 창의 종료까지 남은 ms(자정 넘김 보정, 0 ~ 24h). */
private fun LoopBase.msRemainingUntilEnd(nowMs: Long): Long =
    ((endInDay - nowMs) % MS_1DAY + MS_1DAY) % MS_1DAY

/** AnyTime 루프의 표시 상태. 시각이 아니라 시작/정지(done) 기록으로 대기/진행/완료를 가른다. */
private suspend fun FlowCollector<TimeStat>.anyTimeStat(loop: LoopBase): Long {
    val startTime =
        if (loop.actualStartInDay < 0) LocalTime.MIN else loop.actualStartInDay.toLocalTime()
    return when {
        // 아직 시작 전: 진행 중도 아니고 종료 기록(actualEnd)도 없는 상태.
        loop.doneState != DoneState.IN_PROGRESS && loop.actualEndInDay == ANY_TIME -> {
            emit(TimeStat.BeforeStart(time = LocalTime.MIN, isAnyTime = true))
            MS_1MIN
        }
        // 정지(=종료 시각 기록)된 순간 완료로 본다.
        loop.actualEndInDay != ANY_TIME -> {
            emit(
                TimeStat.Finished(
                    startTime = startTime,
                    endTime = if (loop.actualEndInDay < 0) LocalTime.MAX else loop.actualEndInDay.toLocalTime(),
                    isAnyTime = true,
                )
            )
            MS_1MIN
        }
        // 진행 중: 시작 이후 경과 시간을 보여준다.
        else -> {
            val elapsedMs = startTime.until(LocalTime.now(), ChronoUnit.MILLIS).coerceAtLeast(0L)
            emit(TimeStat.InProgress(time = elapsedMs.toLocalTime(), isAnyTime = true))
            (elapsedMs % MS_1MIN).coerceAtLeast(1000L)
        }
    }
}

private suspend fun FlowCollector<TimeStat>.before(title: String, afterMs: Long): Long {
    emit(TimeStat.BeforeStart(time = afterMs.toLocalTime(), isAnyTime = false))
    val delayInMs = (afterMs % MS_1MIN).coerceAtLeast(1000L)
    logger.i { "[TimeStat] ($title) before updateAfter: $delayInMs" }
    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.inProgress(title: String, remainMs: Long): Long {
    emit(TimeStat.InProgress(time = remainMs.toLocalTime(), isAnyTime = false))
    val delayInMs = (remainMs % MS_1MIN).coerceAtLeast(1000L)
    logger.i { "[TimeStat] ($title) inProgress updateAfter: $delayInMs" }
    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.finished(
    title: String,
    startTime: LocalTime,
    endTime: LocalTime,
): Long {
    emit(TimeStat.Finished(startTime = startTime, endTime = endTime, isAnyTime = false))
    val now = LocalTime.now()
    val delayInMs = min(MS_1MIN, now.until(LocalTime.MAX, ChronoUnit.MILLIS))
    logger.i { "[TimeStat] ($title) finished updateAfter: $delayInMs" }
    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.none(title: String): Long {
    emit(TimeStat.NotToday)
    val now = LocalTime.now()
    val delayInMs = now.until(LocalTime.MAX, ChronoUnit.MILLIS)
    logger.i { "[TimeStat] ($title) none updateAfter: $delayInMs" }
    return delayInMs
}
