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
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
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
    get() = flow {
        val startTime = if (isAnyTime) LocalTime.MIN else startInDay.toLocalTime()

        while (currentCoroutineContext().isActive) {
            val delayInMs = when {

                !isActiveDay() -> none(
                    title = title
                )

                isBeforeStart() -> before(
                    title = title,
                    startTime = startTime,
                    isAnyTime = isAnyTime
                )

                isFinished() -> finished(
                    title = title,
                    // actual 시각은 아직 기록되지 않았을 때 ANY_TIME(-1)일 수 있으므로,
                    // toLocalTime() 이 음수로 크래시하지 않도록 MIN/MAX 로 가드한다.
                    startTime = if (actualStartInDay < 0) LocalTime.MIN else actualStartInDay.toLocalTime(),
                    endTime = if (actualEndInDay < 0) LocalTime.MAX else actualEndInDay.toLocalTime(),
                    isAnyTime = isAnyTime
                )

                else -> inProgress(
                    title = title,
                    startTime = if (actualStartInDay < 0) LocalTime.MIN else actualStartInDay.toLocalTime(),
                    endTime = if (actualEndInDay < 0) LocalTime.MAX else actualEndInDay.toLocalTime(),
                    isAnyTime = isAnyTime,
                )
            }

            delay(delayInMs.milliseconds)
        }
    }

private fun LoopBase.isBeforeStart(): Boolean {
    // anytime 루프의 실제 상태는 base start/end(항상 ANY_TIME)가 아니라 done 기록에 있다.
    // 아직 시작 전 = 진행 중도 아니고, 종료 기록(actualEnd)도 없는 상태.
    if (isAnyTime) return doneState != DoneState.IN_PROGRESS && actualEndInDay == ANY_TIME

    val now = LocalTime.now()
    val startTime = startInDay.toLocalTime()
    return now.isBefore(startTime)
}

private fun LoopBase.isFinished(): Boolean {
    // anytime 루프는 정지(=종료 시각 기록)된 순간 완료로 본다. base end 는 항상 ANY_TIME 이라
    // 참고할 수 없으므로, done 기록의 actualEnd 유무로 판단한다.
    if (isAnyTime) return actualEndInDay != ANY_TIME

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
    logger.i { "[TimeStat] ($title) after updateAfter : $delayInMs, start:$startTime, end:$endTime, isAnyTime:$isAnyTime" }

    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.before(
    title: String,
    startTime: LocalTime,
    isAnyTime: Boolean,
): Long {
    val now = LocalTime.now()
    val afterMs = if (isAnyTime) {
        LocalTime.MAX.toMs()
    } else {
        now.until(startTime, ChronoUnit.MILLIS)
    }

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

    logger.i { "[TimeStat] ($title) before updateAfter: $delayInMs, start:$startTime, isAnyTime:$isAnyTime" }
    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.inProgress(
    title: String,
    startTime: LocalTime,
    endTime: LocalTime,
    isAnyTime: Boolean,
): Long {
    val now = LocalTime.now()
    val remainMs = if (isAnyTime) {
        startTime.until(now, ChronoUnit.MILLIS)
    } else {
        now.until(endTime, ChronoUnit.MILLIS)
    }

    val delayInMs: Long = if (remainMs > 0) {
        emit(
            TimeStat.InProgress(
                time = remainMs.toLocalTime(),
                isAnyTime = isAnyTime,
            )
        )
        remainMs % MS_1MIN
    } else {
        1000L
    }

    logger.i { "[TimeStat] ($title) inProgress updateAfter: $delayInMs, start:$startTime, end:$endTime, isAnyTime:$isAnyTime" }
    return delayInMs
}

private suspend fun FlowCollector<TimeStat>.none(
    title: String,
): Long {
    emit(TimeStat.NotToday)

    val now = LocalTime.now()
    val delayInMs = now.until(LocalTime.MAX, ChronoUnit.MILLIS)

    logger.i { "[TimeStat] ($title) none updateAfter: $delayInMs" }
    return delayInMs
}
