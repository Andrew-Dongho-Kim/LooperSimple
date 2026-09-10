package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class DetailActivityStatsTest {
    private val today = LocalDate.of(2026, 9, 10)
    private val created = today.minusDays(100)

    private fun stats(
        records: Map<LocalDate, Int>,
        createdDate: LocalDate = created,
    ): DetailActivityStats = computeDetailActivityStats(
        resolvedRecords = resolvedActivityRecords(records, createdDate, today),
        createdDate = createdDate,
        today = today,
    )

    @Test
    fun `unsettled and out of lifetime records are excluded`() {
        val records = mapOf(
            created.minusDays(1) to DoneState.DONE,
            today.plusDays(1) to DoneState.DONE,
            today to DoneState.NO_RESPONSE,
            today.minusDays(1) to DoneState.IN_PROGRESS,
            today.minusDays(2) to DoneState.DISABLED,
            today.minusDays(3) to DoneState.NO_RESPONSE,
            today.minusDays(4) to DoneState.DONE,
            today.minusDays(5) to DoneState.SKIP,
        )
        val resolved = resolvedActivityRecords(records, created, today)
        assertEquals(3, resolved.size)
        assertEquals(ActivityCounts(done = 1, skipped = 1, unanswered = 1), countActivity(resolved.values))
    }

    @Test
    fun `today completed and skipped records are reflected immediately`() {
        assertEquals(1, stats(mapOf(today to DoneState.DONE)).counts.done)
        assertEquals(1, stats(mapOf(today to DoneState.SKIP)).counts.skipped)
    }

    @Test
    fun `empty data has no completion rate and no comparison`() {
        val result = stats(emptyMap())
        assertEquals(0, result.counts.total)
        assertNull(result.counts.completionRate)
        assertNull(result.counts.completionPercent)
        assertNull(result.doneDelta)
        assertTrue(result.periods.all { it.counts.total == 0 })
    }

    @Test
    fun `zero completions are different from missing records`() {
        val result = stats(mapOf(today.minusDays(1) to DoneState.SKIP))
        assertEquals(0, result.counts.completionPercent)
        assertEquals(1, result.periods.last().counts.total)
        assertEquals(0, result.periods.last().counts.done)
        assertNull(result.periods.first().counts.completionPercent)
    }

    @Test
    fun `recent window includes exactly today through twenty seven days ago`() {
        val result = stats(
            mapOf(
                today to DoneState.DONE,
                today.minusDays(27) to DoneState.DONE,
                today.minusDays(28) to DoneState.DONE,
            ),
        )
        assertEquals(today.minusDays(27), result.start)
        assertEquals(today, result.end)
        assertEquals(2, result.counts.done)
        assertEquals(1, result.doneDelta)
    }

    @Test
    fun `previous window has no gap or overlap with recent window`() {
        val result = stats(
            mapOf(
                today to DoneState.DONE,
                today.minusDays(28) to DoneState.DONE,
                today.minusDays(55) to DoneState.DONE,
                today.minusDays(56) to DoneState.DONE,
            ),
        )
        assertEquals(-1, result.doneDelta)
    }

    @Test
    fun `period and weekday totals agree with headline counts`() {
        val records = (0L..27L).associate { today.minusDays(it) to DoneState.DONE }
        val result = stats(records)
        assertEquals(28, result.counts.done)
        assertEquals(4, result.periods.size)
        assertTrue(result.periods.all { it.counts.done == 7 })
        assertEquals(28, result.periods.sumOf { it.counts.done })
        assertEquals(28, result.weekdays.sumOf { it.counts.done })
        assertTrue(result.weekdays.all { it.counts.total == 4 })
        assertEquals(DayOfWeek.MONDAY, result.weekdays.first().day)
        assertEquals(DayOfWeek.SUNDAY, result.weekdays.last().day)
    }

    @Test
    fun `weekday samples retain denominator instead of claiming a best day`() {
        val monday = LocalDate.of(2026, 9, 7)
        val result = stats(
            mapOf(
                monday to DoneState.DONE,
                monday.minusWeeks(1) to DoneState.SKIP,
                monday.minusWeeks(2) to DoneState.NO_RESPONSE,
                monday.minusWeeks(3) to DoneState.DISABLED,
            ),
        )
        val counts = result.weekdays.first { it.day == DayOfWeek.MONDAY }.counts
        assertEquals(3, counts.total)
        assertEquals(1, counts.done)
        assertEquals(33, counts.completionPercent)
        assertNull(result.weekdays.first { it.day == DayOfWeek.TUESDAY }.counts.completionRate)
    }

    @Test
    fun `creation within recent period trims only the first visible group`() {
        val creation = today.minusDays(9)
        val result = stats(mapOf(today to DoneState.DONE), createdDate = creation)
        assertEquals(creation, result.start)
        assertEquals(2, result.periods.size)
        assertEquals(creation, result.periods.first().start)
        assertEquals(today.minusDays(7), result.periods.first().end)
        assertEquals(today.minusDays(6), result.periods.last().start)
        assertNull(result.doneDelta)
    }

    @Test
    fun `partial previous lifetime is not compared to a full period`() {
        val records = mapOf(today to DoneState.DONE, today.minusDays(28) to DoneState.DONE)
        assertNull(stats(records, createdDate = today.minusDays(54)).doneDelta)
        assertEquals(0, stats(records, createdDate = today.minusDays(55)).doneDelta)
    }

    @Test
    fun `comparison is unavailable when either period has no records`() {
        assertNull(stats(mapOf(today to DoneState.DONE)).doneDelta)
        assertNull(stats(mapOf(today.minusDays(28) to DoneState.DONE)).doneDelta)
    }

    @Test
    fun `date grouping remains correct across year boundary`() {
        val newYear = LocalDate.of(2027, 1, 3)
        val start = newYear.minusDays(27)
        val result = computeDetailActivityStats(
            resolvedRecords = mapOf(start to DoneState.DONE, newYear to DoneState.DONE),
            createdDate = start.minusYears(1),
            today = newYear,
        )
        assertEquals(start, result.periods.first().start)
        assertEquals(newYear, result.periods.last().end)
        assertEquals(2, result.periods.sumOf { it.counts.done })
    }

    @Test
    fun `summary and detailed stats use the same settled records`() {
        fun response(date: LocalDate, state: Int) = LoopDoneVo(
            loopId = 1,
            date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            done = state,
        )
        val result = computeDetailStats(
            responses = listOf(
                response(today, DoneState.IN_PROGRESS),
                response(today.minusDays(1), DoneState.DONE),
                response(today.minusDays(2), DoneState.SKIP),
                response(today.minusDays(3), DoneState.NO_RESPONSE),
            ),
            memoDates = emptySet(),
            activeDays = LoopDay.EVERYDAY,
            weeklyGoal = 3,
            createdDate = created,
            today = today,
        )
        assertEquals(3, result.totalCount)
        assertEquals(1, result.noResponseCount)
        assertEquals(result.totalCount, result.activity.counts.total)
        assertEquals(result.doneCount, result.activity.counts.done)
        // 진행 중 상태는 달력에 표시할 수 있도록 원본 인덱스에 남겨 둔다.
        assertEquals(DoneState.IN_PROGRESS, result.doneStateByDate[today])
        assertFalse(result.activity.weekdays.all { it.counts.total == 0 })
    }
}
