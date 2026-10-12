package com.pnd.android.loop.ui.home

import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopVo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDoneOptionsTest {
    private val hour = 3_600_000L
    private val minute = 60_000L
    private val timed = LoopVo.create(
        id = 1, title = "Run", color = 0, created = 0L,
        startInDay = 7 * hour, endInDay = 8 * hour,
        activeDays = LoopDay.EVERYDAY, enabled = true, weeklyGoal = 0, isMock = false,
    )
    private val anytime = timed.copy(isAnyTime = true, startInDay = -1, endInDay = -1)

    @Test fun `anytime leads with the usual duration and ends with no time`() {
        val now = 21 * hour + 40 * minute
        val options = recordDoneOptions(anytime, now, isInWindow = false, usualMinutes = 25)
        assertEquals(RecordDoneOption.Usual(25, (now - 25 * minute) to now), options.first())
        assertEquals(listOf(10, 30, 60), options.filterIsInstance<RecordDoneOption.Minutes>().map { it.minutes })
        assertEquals(RecordDoneOption.NoTime, options.last())
    }

    @Test fun `anytime preset equal to the usual duration is not repeated`() {
        val options = recordDoneOptions(anytime, 12 * hour, isInWindow = false, usualMinutes = 30)
        assertEquals(listOf(10, 60), options.filterIsInstance<RecordDoneOption.Minutes>().map { it.minutes })
    }

    @Test fun `anytime without history starts with presets`() {
        val options = recordDoneOptions(anytime, 12 * hour, isInWindow = false, usualMinutes = null)
        assertTrue(options.first() is RecordDoneOption.Minutes)
    }

    @Test fun `anytime chip crossing midnight wraps its start`() {
        val options = recordDoneOptions(anytime, 10 * minute, isInWindow = false, usualMinutes = 20)
        assertEquals((23 * hour + 50 * minute) to 10 * minute, options.first().times)
    }

    @Test fun `timed in window offers plan then finished now then usual`() {
        val now = 7 * hour + 40 * minute
        val options = recordDoneOptions(timed, now, isInWindow = true, usualMinutes = 45)
        assertEquals(
            listOf(
                RecordDoneOption.AsPlanned(7 * hour to 8 * hour),
                RecordDoneOption.FinishedNow(7 * hour to now),
                RecordDoneOption.Usual(45, 7 * hour to 7 * hour + 45 * minute),
            ),
            options,
        )
    }

    @Test fun `timed outside the window has no finished now option and never offers no time`() {
        val options = recordDoneOptions(timed, 12 * hour, isInWindow = false, usualMinutes = null)
        assertEquals(listOf(RecordDoneOption.AsPlanned(7 * hour to 8 * hour)), options)
    }
}
