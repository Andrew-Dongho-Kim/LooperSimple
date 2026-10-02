package com.pnd.android.loop.data.history

import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.state.NOT_SCHEDULED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 완료율의 공통 분모 규칙을 고정한다. 홈 헤더·루프 카드 칩·추세·상세가 모두 이 타입을 쓰므로,
 * 여기가 깨지면 네 화면의 수치가 동시에 달라진다.
 */
class CompletionCountsTest {

    @Test fun `skips stay in the denominator but are not completions`() {
        val counts = countActivity(listOf(DoneState.DONE, DoneState.SKIP))
        assertEquals(2, counts.total)
        assertEquals(50, counts.completionPercent)
        // 건너뜀은 응답한 날이라 응답률에는 들어간다.
        assertEquals(100, counts.responsePercent)
        assertEquals(50, counts.skipPercent)
    }

    @Test fun `unsettled and unscheduled states never reach the denominator`() {
        val counts = countActivity(
            listOf(DoneState.DONE, DoneState.IN_PROGRESS, DoneState.DISABLED, NOT_SCHEDULED),
        )
        assertEquals(1, counts.total)
        assertEquals(100, counts.completionPercent)
    }

    @Test fun `no eligible days yields null rather than zero percent`() {
        val counts = CompletionCounts()
        assertEquals(0, counts.total)
        assertNull(counts.completionRate)
        assertNull(counts.completionPercent)
        assertNull(counts.responsePercent)
        assertNull(counts.skipPercent)
    }

    @Test fun `genuine zero is different from having no records`() {
        val counts = countActivity(listOf(DoneState.NO_RESPONSE, DoneState.NO_RESPONSE))
        assertEquals(2, counts.total)
        assertEquals(0, counts.completionPercent)
    }

    @Test fun `percentages are whole numbers rounded half up`() {
        // 1/3 = 33.33% → 33
        assertEquals(33, countActivity(listOf(DoneState.DONE, DoneState.SKIP, DoneState.SKIP)).completionPercent)
        // 2/3 = 66.67% → 67
        assertEquals(67, countActivity(listOf(DoneState.DONE, DoneState.DONE, DoneState.SKIP)).completionPercent)
        // 1/8 = 12.5% → 13
        val eighth = List(1) { DoneState.DONE } + List(7) { DoneState.NO_RESPONSE }
        assertEquals(13, countActivity(eighth).completionPercent)
    }

    @Test fun `a percentage needs at least the shared minimum sample count`() {
        assertEquals(3, MIN_RELIABLE_SAMPLES)
        assertFalse(countActivity(List(2) { DoneState.DONE }).isReliable)
        assertTrue(countActivity(List(3) { DoneState.DONE }).isReliable)
    }

    @Test fun `the recent window is thirty days for every surface that uses it`() {
        assertEquals(30L, RECENT_COMPLETION_DAYS)
    }
}
