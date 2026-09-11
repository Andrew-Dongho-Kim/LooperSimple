package com.pnd.android.loop.ui.home

import androidx.compose.runtime.Immutable
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
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

/**
 * Calendar-based denominator, as in the home trends: missing responses must not disappear.
 * Use yesterday through 30 days ago, excluding creation day and DISABLED records. Historical
 * schedules are not stored, so eligible weekdays use the loop's current repeat setting.
 * A sample is a scheduled day, not just a day with a response; three misses must show 0%.
 */
internal fun computeRecentLoopCompletion(
    loop: LoopBase,
    history: Map<Long, Int>?,
    today: LocalDate,
): RecentLoopCompletion? {
    if (loop.isMock || !loop.enabled) return null
    val start = maxOf(today.minusDays(RECENT_COMPLETION_DAYS), loop.created.toLocalDate().plusDays(1))
    val end = today.minusDays(1)
    var done = 0
    var skipped = 0
    var unanswered = 0
    var date = start
    while (!date.isAfter(end)) {
        if (loop.isActiveDay(date)) {
            when (history?.get(date.toMs())) {
                DoneState.DISABLED -> Unit
                DoneState.DONE -> done++
                DoneState.SKIP -> skipped++
                else -> unanswered++
            }
        }
        date = date.plusDays(1)
    }
    return RecentLoopCompletion(start, end, done, skipped, unanswered)
        .takeIf { it.totalCount >= RECENT_COMPLETION_MIN_SAMPLES }
}
