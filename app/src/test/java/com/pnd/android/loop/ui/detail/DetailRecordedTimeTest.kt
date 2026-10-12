package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.state.DoneState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DetailRecordedTimeTest {
    private val date = LocalDate.of(2026, 10, 8)
    private val zone = ZoneId.of("Asia/Seoul")
    private fun record(start: Long, end: Long, source: Int = LoopDoneVo.TimeSource.USER_ENTERED) =
        LoopDoneVo(1, 0, start, end, DoneState.DONE, timeSource = source)

    @Test fun enteredOvernightTimeHasNextDayAndCorrectDuration() {
        val result = recordedTimeOf(record(23 * 3_600_000L, 30 * 60_000L), date, zone)!!
        assertEquals(LocalTime.of(23, 0), result.start)
        assertEquals(LocalTime.of(0, 30), result.end)
        assertEquals(1L, result.endDayOffset)
        assertEquals(90 * 60_000L, result.durationMs)
    }
    @Test fun measuredTimeUsesActualInstantsAcrossMultipleDays() {
        val start = date.atTime(23, 0).atZone(zone).toInstant().toEpochMilli()
        val end = date.plusDays(2).atTime(1, 0).atZone(zone).toInstant().toEpochMilli()
        val result = recordedTimeOf(record(0, 0, LoopDoneVo.TimeSource.MEASURED)
            .copy(startedAt = start, endedAt = end), date, zone)!!
        assertEquals(LocalTime.of(23, 0), result.start)
        assertEquals(2L, result.endDayOffset)
        assertEquals(26 * 3_600_000L, result.durationMs)
    }
    @Test fun runningTimerNeverDisplaysPlannedEndAsAnActualEnd() {
        val result = recordedTimeOf(record(8 * 3_600_000L, 9 * 3_600_000L)
            .copy(done = DoneState.IN_PROGRESS), date, zone)!!
        assertTrue(result.running)
        assertNull(result.end)
        assertNull(result.durationMs)
    }
    @Test fun legacyDefaultAndMissingStartHaveNoRecordedInterval() {
        assertNull(recordedTimeOf(record(0, 0, LoopDoneVo.TimeSource.UNKNOWN), date, zone))
        assertNull(recordedTimeOf(record(-1, -1), date, zone))
    }
    @Test fun midnightWithKnownSourceIsAValidRecordedTime() {
        val result = recordedTimeOf(record(0, 45 * 60_000L), date, zone)!!
        assertEquals(LocalTime.MIDNIGHT, result.start)
        assertEquals(45 * 60_000L, result.durationMs)
    }
    @Test fun scheduledTimesKeepTheirSource() {
        val result = recordedTimeOf(record(8 * 3_600_000L, 9 * 3_600_000L, LoopDoneVo.TimeSource.PLANNED), date, zone)!!
        assertEquals(LoopDoneVo.TimeSource.PLANNED, result.source)
    }
    @Test fun missingEndIsNotAssumedToBeMidnight() {
        val result = recordedTimeOf(record(8 * 3_600_000L, -1), date, zone)!!
        assertNull(result.end)
        assertNull(result.durationMs)
    }
    @Test fun measuredRunningStateIgnoresStaleEndTimestamp() {
        val start = date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val result = recordedTimeOf(record(0, 0, LoopDoneVo.TimeSource.MEASURED)
            .copy(done = DoneState.IN_PROGRESS, startedAt = start, endedAt = start + 60_000), date, zone)!!
        assertTrue(result.running)
        assertNull(result.end)
        assertNull(result.durationMs)
    }
}
