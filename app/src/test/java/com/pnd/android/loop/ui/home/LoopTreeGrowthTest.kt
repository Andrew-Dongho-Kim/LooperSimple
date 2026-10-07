package com.pnd.android.loop.ui.home

import com.pnd.android.loop.data.history.LoopHistory
import com.pnd.android.loop.data.history.LoopHistorySnapshot
import com.pnd.android.loop.data.history.firstDay
import com.pnd.android.loop.data.history.response
import com.pnd.android.loop.data.history.revision
import com.pnd.android.loop.data.history.testLoop
import com.pnd.android.loop.state.DoneState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoopTreeGrowthTest {
    @Test fun stageBoundariesAndMissingHistory() {
        assertEquals(1, treeGrowthStage(null))
        assertEquals(1, treeGrowthStage(9))
        assertEquals(2, treeGrowthStage(10))
        assertEquals(7, treeGrowthStage(64))
        assertEquals(10, treeGrowthStage(90))
        assertEquals(10, treeGrowthStage(100))
        assertEquals(1, treeGrowthStage(-1))
    }

    @Test fun allFiveRewardThresholds() {
        listOf(7 to 1, 14 to 2, 30 to 3).forEach { (days, level) ->
            assertEquals(level - 1, TreeVitality(days - 1, 0).rewardLevel)
            assertEquals(level, TreeVitality(days, 0).rewardLevel)
        }
        assertEquals(3, TreeVitality(60, 59).rewardLevel)
        assertEquals(4, TreeVitality(60, 60).rewardLevel)
        assertEquals(4, TreeVitality(100, 99).rewardLevel)
        assertEquals(5, TreeVitality(100, 100).rewardLevel)
        assertNull(TreeVitality(100, 100).nextReward)
    }

    @Test fun restDaysPauseAndLowerRatesEndOnlyTheRelevantStreak() {
        assertEquals(TreeVitality(5, 2), sustainedTreeRates(listOf(80, null, 90, 79, 60, 60, 59, 100)))
        assertEquals(TreeVitality(), sustainedTreeRates(listOf(59, 100, 100)))
        assertEquals(TreeVitality(), sustainedTreeRates(listOf(null, null)))
        assertEquals(TreeVitality(), sustainedTreeRates(listOf(-1, 100)))
    }

    @Test fun unfinishedTodayDoesNotBreakHistoricalReward() {
        val loop = testLoop()
        val today = firstDay.plusDays(9)
        val responses = (0L..8L).map { response(firstDay.plusDays(it)) } +
            response(today, DoneState.NO_RESPONSE)
        val history = LoopHistory(loop, listOf(revision(1, loop)), responses, emptyList())
        // The first two days lack the three occurrences required for a reliable rate.
        assertEquals(TreeVitality(7, 7), computeTreeVitality(LoopHistorySnapshot(listOf(history)), today))
    }

    @Test fun estimatedPlansCannotEarnEffects() {
        val loop = testLoop()
        val today = firstDay.plusDays(40)
        val responses = (0L..39L).map { response(firstDay.plusDays(it)) }
        val history = LoopHistory(loop, listOf(revision(1, loop, knownFrom = today)), responses, emptyList())
        assertEquals(TreeVitality(), computeTreeVitality(LoopHistorySnapshot(listOf(history)), today))
    }
}
