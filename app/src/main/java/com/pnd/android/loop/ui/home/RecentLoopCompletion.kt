package com.pnd.android.loop.ui.home

import androidx.compose.runtime.Immutable
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.history.LoopHistory
import com.pnd.android.loop.data.history.LoopTimeline
import java.time.LocalDate
import kotlin.math.roundToInt

internal const val RECENT_COMPLETION_DAYS = 30L
internal const val RECENT_COMPLETION_MIN_SAMPLES = 3

/** The chip and its explanation share this snapshot, including the denominator and date range. */
@Immutable
data class RecentLoopCompletion(
    val start: LocalDate,
    val end: LocalDate,
    val doneCount: Int,
    val skipCount: Int,
    val unansweredCount: Int,
) {
    val totalCount: Int get() = doneCount + skipCount + unansweredCount
    val percent: Int get() = if (totalCount == 0) 0 else
        (100.0 * doneCount / totalCount).roundToInt()
}

/** The same occurrence resolver as monthly and detailed statistics, bounded to 30 days. */
internal fun computeRecentLoopCompletion(timeline: LoopTimeline, today: LocalDate): RecentLoopCompletion? {
    val loop = timeline.current
    if (loop.isMock || !loop.enabled) return null
    val start = maxOf(today.minusDays(RECENT_COMPLETION_DAYS), timeline.createdDate)
    val end = today.minusDays(1)
    val days = timeline.days(start, end).filter { it.isSettled(today) }
    return RecentLoopCompletion(
        start, end, days.count { it.response.isDone() }, days.count { it.response.isSkip() },
        days.count { it.response.done == DoneState.NO_RESPONSE },
    ).takeIf { it.totalCount >= RECENT_COMPLETION_MIN_SAMPLES }
}

/** Legacy fixture adapter; production passes the complete timeline. */
internal fun computeRecentLoopCompletion(loop: LoopBase, history: Map<Long, Int>?, today: LocalDate): RecentLoopCompletion? =
    computeRecentLoopCompletion(LoopTimeline(LoopHistory(
        loop.asLoopVo(), emptyList(), history.orEmpty().map { (date, state) ->
            LoopDoneVo(loop.loopId, date, done = state)
        }, emptyList(),
    )), today)
