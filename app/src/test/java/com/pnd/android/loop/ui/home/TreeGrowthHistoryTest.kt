package com.pnd.android.loop.ui.home

import com.pnd.android.loop.data.history.*
import org.junit.Assert.*
import org.junit.Test

class TreeGrowthHistoryTest {
    private fun observation(day: Long, stage: Int = 1, wilt: Int = 0, reward: Int = 0,
        reconstructed: Boolean = true) = TreeGrowthObservation(firstDay.plusDays(day),
        TreeVitality(earnedStage = stage, earnedReward = reward, wiltLevel = wilt), reconstructed)

    @Test fun savedAppearanceWinsOverRecalculationAndNewObservationReplacesSameDate() {
        val replay = listOf(observation(0, stage = 2), observation(1, stage = 3))
        val saved = listOf(observation(0, stage = 5, reconstructed = false))
        val current = observation(1, stage = 6, reconstructed = false)
        assertEquals(listOf(saved.single(), current), mergeTreeGrowthHistory(replay, saved, current))
    }

    @Test fun savedObservationsSurviveDeletedSourceHistory() {
        val saved = listOf(observation(3, stage = 6, reconstructed = false))
        assertEquals(saved, mergeTreeGrowthHistory(emptyList(), saved, null))
    }

    @Test fun reconstructionCarriesPreviouslyObservedStageAndRewardForward() {
        val saved = observation(1, stage = 6, reward = 2, reconstructed = false)
        val merged = mergeTreeGrowthHistory(listOf(observation(0), observation(2, wilt = 1)), listOf(saved), null)
        assertEquals(1, merged[0].vitality.earnedStage)
        assertEquals(saved, merged[1])
        assertEquals(6, merged[2].vitality.earnedStage)
        assertEquals(2, merged[2].vitality.rewardLevel)
        assertEquals(1, merged[2].vitality.wiltLevel)
        assertTrue(merged[2].reconstructed)
    }

    @Test fun simultaneousGrowthAndRewardAreBothShownAndPointsAloneDoNotCreateEvents() {
        val history = listOf(observation(0), observation(1, stage = 2, reward = 1),
            observation(2, stage = 2, reward = 1).copy(vitality = TreeVitality(
                earnedStage = 2, earnedReward = 1, growthPoints = 10)))
        assertEquals(listOf(TreeGrowthChange.GROWTH, TreeGrowthChange.REWARD),
            treeGrowthEvents(history).map { it.change })
    }

    @Test fun retroactiveEditsCannotInventAStageAboveALaterSavedAppearance() {
        val saved = observation(1, stage = 4, reward = 1, reconstructed = false)
        val history = mergeTreeGrowthHistory(
            listOf(observation(0, stage = 8, reward = 3), observation(2, stage = 2)), listOf(saved), null)
        assertEquals(listOf(4, 4, 4), history.map { it.vitality.earnedStage })
        assertEquals(listOf(1, 1, 1), history.map { it.vitality.rewardLevel })
        assertEquals(saved, history[1])
    }

    @Test fun healthChangesAreSeparateFromEarnedStages() {
        val events = treeGrowthEvents(listOf(observation(0, stage = 6),
            observation(1, stage = 6, wilt = 1), observation(2, stage = 6)))
        assertEquals(listOf(TreeGrowthChange.WILT, TreeGrowthChange.RECOVERY), events.map { it.change })
    }

    @Test fun replayExcludesTodayAndDoesNotChangeGrowthCalculation() {
        val loop = testLoop()
        val snapshot = LoopHistorySnapshot(listOf(LoopHistory(loop, listOf(revision(1, loop)),
            (0L..40L).map { response(firstDay.plusDays(it)) }, emptyList())))
        val today = firstDay.plusDays(40)
        val memory = TreeGardenMemory(initialized = true)
        val replay = mutableListOf<TreeGrowthObservation>()
        val result = evaluateTreeGarden(snapshot, today, memory) { date, vitality ->
            replay.add(TreeGrowthObservation(date, vitality))
        }
        assertEquals(evaluateTreeGarden(snapshot, today, memory), result)
        assertEquals(today.minusDays(1), replay.last().date)
        assertEquals(result.vitality, replay.last().vitality)
        assertTrue(replay.zipWithNext().all { (a, b) -> a.vitality.earnedStage <= b.vitality.earnedStage })
        assertEquals(0, replay.first().vitality.growthPoints)
        assertEquals(37, replay.last().vitality.growthPoints)
    }

    @Test fun replayDoesNotPaintTodaysStageOntoThePast() {
        val loop = testLoop()
        val snapshot = LoopHistorySnapshot(listOf(LoopHistory(loop, listOf(revision(1, loop)),
            (0L..9L).map { response(firstDay.plusDays(it)) }, emptyList())))
        val stages = mutableListOf<Int>()
        evaluateTreeGarden(snapshot, firstDay.plusDays(10), TreeGardenMemory(true, earnedStage = 10)) { _, value ->
            stages.add(value.earnedStage)
        }
        assertEquals(1, stages.first())
        assertTrue(stages.last() < 10)
    }
}
