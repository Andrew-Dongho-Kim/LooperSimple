package com.pnd.android.loop.ui.home

import com.pnd.android.loop.data.history.LoopHistorySnapshot
import com.pnd.android.loop.data.history.ResolvedLoopDay
import com.pnd.android.loop.data.history.countActivity
import com.pnd.android.loop.state.DoneState
import java.time.LocalDate
import kotlin.math.roundToInt

val treeGrowthPoints = listOf(0, 5, 15, 32, 53, 77, 105, 137, 172, 210)

data class TreeGardenMemory(
    val initialized: Boolean = false,
    val earnedStage: Int = 1,
    val earnedReward: Int = 0,
    val dailyPoints: Map<Long, Int> = emptyMap(),
    val migrationCredit: Int = 0,
)

data class TreeGardenResult(val memory: TreeGardenMemory, val vitality: TreeVitality)

/** Equal weight per loop, including historical misses of loops archived since then. */
internal fun balancedTreePercent(days: List<ResolvedLoopDay>): Int? {
    if (days.isEmpty() || days.any { it.estimated }) return null
    return days.groupBy { it.loop.loopId }.values.mapNotNull {
        countActivity(it.map { day -> day.response.done }).completionRate
    }.takeIf { it.isNotEmpty() }?.average()?.times(100)?.roundToInt()
}

internal fun dailyTreePoints(completedLoops: Int): Int =
    (completedLoops + if (completedLoops >= 2) 1 else 0).coerceIn(0, 3)

/** Chronological settled activity-day rates; null is unknown data, not a missed day. */
internal fun treeWiltLevel(rates: List<Int?>): Int {
    return TreeHealthTracker().apply { rates.forEach(::accept) }.wilt
}

private class TreeHealthTracker {
    var wilt = 0
        private set
    private var below60 = 0
    private var below40 = 0
    private var below25 = 0
    private var recovery = 0
    fun accept(rate: Int?) {
        if (rate == null) {
            below60 = 0; below40 = 0; below25 = 0; recovery = 0
        } else {
            below60 = if (rate < 60) below60 + 1 else 0
            below40 = if (rate < 40) below40 + 1 else 0
            below25 = if (rate < 25) below25 + 1 else 0
            val target = when {
                below25 >= 21 -> 3
                below40 >= 14 -> 2
                below60 >= 7 -> 1
                else -> 0
            }
            if (target > wilt) { wilt = target; recovery = 0 }
            if (wilt > 0 && rate >= 50) {
                recovery++
                if (recovery >= 2 && (wilt > 1 || rate >= 60)) {
                    wilt--; recovery = 0
                }
            } else recovery = 0
        }
    }
}

/** Re-evaluation is idempotent. Earned points/levels never disappear after edits or deletion. */
fun evaluateTreeGarden(
    snapshot: LoopHistorySnapshot, today: LocalDate, previous: TreeGardenMemory,
    onDayEvaluated: ((LocalDate, TreeVitality) -> Unit)? = null,
): TreeGardenResult {
    val days = snapshot.firstDate?.let { first ->
        snapshot.settled(first, today.minusDays(1), today).filter { !it.loop.isMock }
    }.orEmpty()
    val byDate = days.groupBy { it.date }.toSortedMap()
    val points = previous.dailyPoints.toMutableMap()
    val activity = mutableListOf<List<ResolvedLoopDay>>()
    val healthRates = mutableListOf<Int?>()
    val tracker = TreeHealthTracker()
    var candidateStage = 1
    var newPointsSoFar = 0
    var carriedPoints = 0
    val priorLedger = previous.dailyPoints.entries.sortedBy { it.key }
    var ledgerIndex = 0
    var days60 = 0
    var days80 = 0
    var migrationStage = 1
    var migrationReward = 0
    var legacy60 = 0
    var legacy80 = 0
    val scheduledCounts = mutableMapOf<Int, Int>()
    byDate.forEach { (date, day) ->
        day.filter { it.scheduled && !it.estimated }.forEach {
            val id = it.loop.loopId
            scheduledCounts[id] = (scheduledCounts[id] ?: 0) + 1
        }
        activity.add(day)
        val health = if (activity.size >= 7) balancedTreePercent(activity.takeLast(7).flatten()) else null
        healthRates.add(health)
        tracker.accept(health)
        val window = (0L..13L).flatMap { byDate[date.minusDays(it)].orEmpty() }
        val rate = balancedTreePercent(window)
        if (!previous.initialized) {
            val legacyWindow = (0L..29L).flatMap { byDate[date.minusDays(it)].orEmpty() }
            val counts = countActivity(legacyWindow.map { it.response.done })
            if (counts.isReliable) migrationStage = maxOf(migrationStage, treeGrowthStage(counts.completionPercent))
            val legacyRate = counts.completionPercent.takeIf { counts.isReliable && legacyWindow.none { it.estimated } }
            legacy60 = if (legacyRate != null && legacyRate >= 60) legacy60 + 1 else 0
            legacy80 = if (legacyRate != null && legacyRate >= 80) legacy80 + 1 else 0
            migrationReward = maxOf(migrationReward, TreeVitality(legacy60, legacy80).rewardLevel)
        }
        val eligible = day.filter { occurrence ->
            occurrence.scheduled && !occurrence.estimated &&
                snapshot.byId[occurrence.loop.loopId]?.let { timeline ->
                    date >= timeline.createdDate.plusDays(3) &&
                        (scheduledCounts[occurrence.loop.loopId] ?: 0) >= 2
                } == true
        }
        val completed = eligible.filter { it.response.done == DoneState.DONE }
            .map { it.loop.loopId }.distinct().size
        val epoch = date.toEpochDay()
        while (ledgerIndex < priorLedger.size && priorLedger[ledgerIndex].key <= epoch) {
            carriedPoints += priorLedger[ledgerIndex++].value
        }
        val added = (dailyTreePoints(completed) - (points[epoch] ?: 0)).coerceAtLeast(0)
        points[epoch] = maxOf(points[epoch] ?: 0, dailyTreePoints(completed))
        newPointsSoFar += added
        if (tracker.wilt == 0 && rate != null && rate >= 60) {
            candidateStage = maxOf(candidateStage,
                treeGrowthPoints.indexOfLast { carriedPoints + newPointsSoFar + previous.migrationCredit >= it } + 1)
        }
        // One settled practice day at most; estimates cannot earn rewards.
        if (rate != null && day.none { it.estimated } && eligible.isNotEmpty()) {
            if (rate >= 60) days60++
            if (rate >= 80) days80++
        }
        // Historical reconstruction must not apply today's earned-stage floor to older days.
        onDayEvaluated?.invoke(date, TreeVitality(
            daysAt60 = days60, daysAt80 = days80,
            earnedStage = maxOf(candidateStage, migrationStage),
            earnedReward = maxOf(migrationReward, TreeVitality(days60, days80).rewardLevel),
            growthPoints = carriedPoints + newPointsSoFar + previous.migrationCredit,
            growthPercent = rate, healthPercent = health, wiltLevel = tracker.wilt,
        ))
    }
    val credit = if (!previous.initialized)
        (treeGrowthPoints[migrationStage - 1] - points.values.sum()).coerceAtLeast(0)
        else previous.migrationCredit
    val total = points.values.sum() + credit
    val recent14 = balancedTreePercent(days.filter { it.date >= today.minusDays(14) })
    val wilt = tracker.wilt
    val candidate = if (wilt == 0 && recent14 != null && recent14 >= 60)
        treeGrowthPoints.indexOfLast { total >= it } + 1 else 1
    val earnedStage = maxOf(previous.earnedStage, migrationStage, candidateStage, candidate)
    val earnedReward = maxOf(previous.earnedReward, migrationReward, TreeVitality(days60, days80).rewardLevel)
    val memory = TreeGardenMemory(true, earnedStage, earnedReward, points.filterValues { it > 0 }, credit)
    return TreeGardenResult(memory, TreeVitality(days60, days80, earnedStage, earnedReward, total,
        recent14, healthRates.lastOrNull(), wilt))
}
