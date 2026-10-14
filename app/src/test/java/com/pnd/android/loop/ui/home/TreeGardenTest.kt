package com.pnd.android.loop.ui.home

import com.pnd.android.loop.data.history.*
import com.pnd.android.loop.state.DoneState
import org.junit.Assert.*
import org.junit.Test

class TreeGardenTest {
    private fun history(count: Int, loops: Int = 1, state: Int = DoneState.DONE): LoopHistorySnapshot =
        LoopHistorySnapshot((1..loops).map { id ->
            val loop = testLoop().copy(loopId = id)
            LoopHistory(loop, listOf(revision(id.toLong(), loop)), (0 until count).map {
                response(firstDay.plusDays(it.toLong()), state, id.toLong()).copy(loopId = id)
            }, emptyList())
        })

    @Test fun dailyCapIncludesDiversityBonus() {
        assertEquals(listOf(0, 1, 3, 3, 3), (0..4).map(::dailyTreePoints))
    }

    @Test fun oneLoopCanGrowAndThereIsNoWeeklyCap() {
        val result = evaluateTreeGarden(history(40), firstDay.plusDays(40), TreeGardenMemory(initialized = true))
        assertEquals(37, result.vitality.growthPoints)
        assertEquals(4, result.vitality.earnedStage)
        assertEquals(7, result.memory.dailyPoints.entries.filter {
            it.key in firstDay.plusDays(7).toEpochDay()..firstDay.plusDays(13).toEpochDay()
        }.sumOf { it.value })
    }

    @Test fun twoLoopsEarnBonusButAddingMoreCannotFarmPoints() {
        val two = evaluateTreeGarden(history(40, 2), firstDay.plusDays(40), TreeGardenMemory(true))
        val four = evaluateTreeGarden(history(40, 4), firstDay.plusDays(40), TreeGardenMemory(true))
        assertEquals(111, two.vitality.growthPoints)
        assertEquals(two.vitality.growthPoints, four.vitality.growthPoints)
    }

    @Test fun repeatedEvaluationAndHistoryRemovalKeepEarnedProgress() {
        val first = evaluateTreeGarden(history(40), firstDay.plusDays(40), TreeGardenMemory(true))
        val again = evaluateTreeGarden(history(40), firstDay.plusDays(40), first.memory)
        assertEquals(first, again)
        val removed = evaluateTreeGarden(LoopHistorySnapshot(emptyList()), firstDay.plusDays(41), first.memory)
        assertEquals(first.memory, removed.memory)
    }

    @Test fun initialMigrationPreservesTheCurrentTreeAndEffects() {
        val result = evaluateTreeGarden(history(40), firstDay.plusDays(40), TreeGardenMemory())
        assertEquals(10, result.vitality.earnedStage)
        assertEquals(210, result.vitality.growthPoints)
        assertTrue(result.vitality.rewardLevel >= 3)
    }

    @Test fun currentDayDoesNotAffectPointsOrHealth() {
        val today = firstDay.plusDays(40)
        val before = evaluateTreeGarden(history(40), today, TreeGardenMemory(true))
        val withToday = evaluateTreeGarden(history(41), today, TreeGardenMemory(true))
        assertEquals(before, withToday)
    }

    @Test fun wiltWaitsForSevenFourteenAndTwentyOneDays() {
        assertEquals(0, treeWiltLevel(List(6) { 59 }))
        assertEquals(1, treeWiltLevel(List(7) { 59 }))
        assertEquals(1, treeWiltLevel(List(13) { 39 }))
        assertEquals(2, treeWiltLevel(List(14) { 39 }))
        assertEquals(2, treeWiltLevel(List(20) { 24 }))
        assertEquals(3, treeWiltLevel(List(21) { 24 }))
        assertEquals(0, treeWiltLevel(List(30) { 60 }))
    }

    @Test fun recoveryTakesTwoDaysPerStepAndSixtyToBecomeHealthy() {
        val severe = List(21) { 0 }
        assertEquals(3, treeWiltLevel(severe + 50))
        assertEquals(2, treeWiltLevel(severe + listOf(50, 50)))
        assertEquals(1, treeWiltLevel(severe + List(4) { 50 }))
        assertEquals(1, treeWiltLevel(severe + List(8) { 50 }))
        assertEquals(0, treeWiltLevel(severe + List(4) { 50 } + listOf(60, 60)))
    }

    @Test fun insufficientOrUnknownRecordsDoNotWilt() {
        assertEquals(0, treeWiltLevel(List(30) { null }))
        assertEquals(0, treeWiltLevel(List(6) { 0 } + listOf(null) + List(6) { 0 }))
        val result = evaluateTreeGarden(history(6, state = DoneState.NO_RESPONSE), firstDay.plusDays(6), TreeGardenMemory(true))
        assertEquals(0, result.vitality.wiltLevel)
    }

    @Test fun pointsAccumulateWhileWiltedButLevelDoesNotUpgrade() {
        val previous = TreeGardenMemory(true, earnedStage = 3, earnedReward = 2,
            dailyPoints = mapOf(firstDay.minusDays(1).toEpochDay() to 3))
        val snapshot = LoopHistorySnapshot(history(35, 5, DoneState.NO_RESPONSE).timelines.map {
            if (it.current.loopId == 1) it.history.copy(responses = it.history.responses.map { row ->
                row.copy(done = DoneState.DONE)
            }) else it.history
        })
        val result = evaluateTreeGarden(snapshot, firstDay.plusDays(35), previous)
        assertEquals(3, result.vitality.wiltLevel)
        assertEquals(3, result.vitality.earnedStage)
        assertEquals(2, result.vitality.rewardLevel)
        assertEquals(35, result.vitality.growthPoints)
    }

    @Test fun uncertainHistoricalSchedulesCannotEarnPoints() {
        val loop = testLoop()
        val today = firstDay.plusDays(40)
        val snapshot = LoopHistorySnapshot(listOf(LoopHistory(loop,
            listOf(revision(1, loop, knownFrom = today)),
            (0L..39L).map { response(firstDay.plusDays(it)) }, emptyList())))
        val result = evaluateTreeGarden(snapshot, today, TreeGardenMemory(true))
        assertEquals(0, result.vitality.growthPoints)
        assertEquals(0, result.vitality.rewardLevel)
        assertEquals(0, result.vitality.wiltLevel)
    }

    @Test fun scheduledRestDaysPauseWilting() {
        val loop = testLoop()
        val stop = firstDay.plusDays(20)
        val snapshot = LoopHistorySnapshot(listOf(LoopHistory(loop,
            listOf(revision(1, loop), revision(2, loop.copy(enabled = false), from = stop)),
            (0L..19L).map { response(firstDay.plusDays(it), DoneState.NO_RESPONSE) }, emptyList())))
        val before = evaluateTreeGarden(snapshot, stop, TreeGardenMemory(true))
        val after = evaluateTreeGarden(snapshot, stop.plusDays(30), before.memory)
        assertEquals(before.vitality.wiltLevel, after.vitality.wiltLevel)
    }

    @Test fun offlineHistoricalGrowthIsRetainedAfterLaterWilting() {
        val loop = testLoop()
        val snapshot = LoopHistorySnapshot(listOf(LoopHistory(loop, listOf(revision(1, loop)),
            (0L..119L).map { response(firstDay.plusDays(it),
                if (it < 80) DoneState.DONE else DoneState.NO_RESPONSE) }, emptyList())))
        val result = evaluateTreeGarden(snapshot, firstDay.plusDays(120), TreeGardenMemory(true))
        assertEquals(3, result.vitality.wiltLevel)
        assertEquals(6, result.vitality.earnedStage)
        assertEquals(77, result.vitality.growthPoints)
    }

    @Test fun easyFrequentLoopDoesNotHideAnotherLoopsFailures() {
        val a = history(10).settled(firstDay, firstDay.plusDays(9), firstDay.plusDays(10))
        val b = history(1, state = DoneState.NO_RESPONSE).settled(firstDay, firstDay, firstDay.plusDays(10))
            .map { it.copy(loop = it.loop.copy(loopId = 2), response = it.response.copy(loopId = 2)) }
        assertEquals(50, balancedTreePercent(a + b))
    }
}
