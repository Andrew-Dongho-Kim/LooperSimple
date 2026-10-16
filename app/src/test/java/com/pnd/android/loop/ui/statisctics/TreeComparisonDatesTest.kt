package com.pnd.android.loop.ui.statisctics

import com.pnd.android.loop.ui.home.TreeGrowthObservation
import com.pnd.android.loop.ui.home.TreeVitality
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TreeComparisonDatesTest {
    private val first = LocalDate.of(2026, 9, 1)
    private val history = (0L..60L).map { TreeGrowthObservation(first.plusDays(it), TreeVitality()) }

    @Test fun defaultsToLatestThirtyDays() {
        val (before, after) = treeComparisonDates(history, null, null)!!
        assertEquals(first.plusDays(30), before.date)
        assertEquals(first.plusDays(60), after.date)
    }

    @Test fun invalidOrReversedSelectionFallsBackToAnAvailableEarlierDate() {
        val (before, after) = treeComparisonDates(history, first.plusDays(50).toEpochDay(), first.plusDays(20).toEpochDay())!!
        assertEquals(first, before.date)
        assertEquals(first.plusDays(20), after.date)
    }

    @Test fun singleRecordCanBeDisplayedWithoutInventingAnEarlierAppearance() {
        val pair = treeComparisonDates(history.take(1), null, null)!!
        assertEquals(pair.first, pair.second)
        assertNull(treeComparisonDates(emptyList(), null, null))
    }
}
