package com.pnd.android.loop.ui.home

import com.pnd.android.loop.data.history.LoopHistorySnapshot
import com.pnd.android.loop.data.history.RECENT_COMPLETION_DAYS
import com.pnd.android.loop.data.history.countActivity
import java.time.LocalDate

/** Special effects are earned from historical rolling rates, independently of today's water. */
data class TreeReward(val level: Int, val minimumPercent: Int, val days: Int)

val treeRewards = listOf(
    TreeReward(1, 60, 7), TreeReward(2, 60, 14), TreeReward(3, 60, 30),
    TreeReward(4, 80, 60), TreeReward(5, 80, 100),
)

data class TreeVitality(val daysAt60: Int = 0, val daysAt80: Int = 0) {
    val rewardLevel: Int get() = treeRewards.lastOrNull {
        daysFor(it) >= it.days
    }?.level ?: 0
    val nextReward: TreeReward? get() = treeRewards.firstOrNull { it.level > rewardLevel }
    fun daysFor(reward: TreeReward): Int = if (reward.minimumPercent == 80) daysAt80 else daysAt60
}

fun treeGrowthStage(percent: Int?): Int = ((percent ?: 0).coerceIn(0, 100) / 10 + 1).coerceAtMost(10)

/**
 * Each settled day evaluates the preceding 30-day window ending on that day. Today's
 * unfinished tasks never revoke a reward. Rest days pause the count; they cannot earn it.
 * Estimated schedules cannot establish a streak (or hide a missed day in its denominator).
 * Scan at most one year: enough for 100 active days with a twice-weekly schedule.
 */
fun computeTreeVitality(snapshot: LoopHistorySnapshot, today: LocalDate): TreeVitality {
    val from = today.minusDays(366 + RECENT_COMPLETION_DAYS)
    val byDate = snapshot.settled(from, today.minusDays(1), today)
        .filterNot { it.loop.isMock }
        .groupBy { it.date }
    val rates = (1L..366L).map { offset ->
        val date = today.minusDays(offset)
        val day = byDate[date].orEmpty()
        if (day.isEmpty()) null else {
            val window = (0L until RECENT_COMPLETION_DAYS).flatMap {
                byDate[date.minusDays(it)].orEmpty()
            }
            if (window.any { it.estimated }) -1 else {
                val counts = countActivity(window.map { it.response.done })
                if (counts.isReliable) counts.completionPercent ?: -1 else -1
            }
        }
    }
    return sustainedTreeRates(rates)
}

/** Newest first. A null means no scheduled occurrences, not a failed day. */
internal fun sustainedTreeRates(rates: List<Int?>): TreeVitality {
    fun count(threshold: Int): Int = rates.asSequence().filterNotNull()
        .takeWhile { it >= threshold }.count()
    return TreeVitality(count(60), count(80))
}
