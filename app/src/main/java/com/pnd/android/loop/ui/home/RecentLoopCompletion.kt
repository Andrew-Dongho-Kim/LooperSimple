package com.pnd.android.loop.ui.home

import androidx.compose.runtime.Immutable
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.history.CompletionCounts
import com.pnd.android.loop.data.history.LoopHistory
import com.pnd.android.loop.data.history.LoopTimeline
import com.pnd.android.loop.data.history.RECENT_COMPLETION_DAYS
import com.pnd.android.loop.data.history.completionCounts
import java.time.LocalDate

/** The chip and its explanation share this snapshot, including the denominator and date range. */
@Immutable
data class RecentLoopCompletion(
    val start: LocalDate,
    val end: LocalDate,
    val counts: CompletionCounts,
) {
    val doneCount: Int get() = counts.done
    val skipCount: Int get() = counts.skipped
    val unansweredCount: Int get() = counts.unanswered
    val totalCount: Int get() = counts.total

    /** 집계 대상이 없는 인스턴스는 만들어지지 않으므로(아래 takeIf) 이 값은 항상 실제 비율이다. */
    val percent: Int get() = counts.completionPercent ?: 0
}

/** The same occurrence resolver as monthly and detailed statistics, bounded to 30 days. */
internal fun computeRecentLoopCompletion(timeline: LoopTimeline, today: LocalDate): RecentLoopCompletion? {
    val loop = timeline.current
    // 후보 선정: 꺼 둔 루프와 입력창 초안은 카드에 칩을 달지 않는다. 분모 규칙이 아니라
    // "이 카드에 칩을 보여 줄지"의 문제이므로 공용 리듀서에 넣지 않는다.
    if (loop.isMock || !loop.enabled) return null
    val start = maxOf(today.minusDays(RECENT_COMPLETION_DAYS), timeline.createdDate)
    val end = today.minusDays(1)
    return RecentLoopCompletion(start, end, timeline.days(start, end).completionCounts(today))
        .takeIf { it.counts.isReliable }
}

/** Legacy fixture adapter; production passes the complete timeline. */
internal fun computeRecentLoopCompletion(loop: LoopBase, history: Map<Long, Int>?, today: LocalDate): RecentLoopCompletion? =
    computeRecentLoopCompletion(LoopTimeline(LoopHistory(
        loop.asLoopVo(), emptyList(), history.orEmpty().map { (date, state) ->
            LoopDoneVo(loop.loopId, date, done = state)
        }, emptyList(),
    )), today)
