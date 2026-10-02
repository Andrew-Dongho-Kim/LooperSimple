package com.pnd.android.loop.ui.home.viewmodel

import com.pnd.android.loop.data.history.CompletionCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 홈 헤더가 읽는 값들. 이 래퍼에는 계산이 없고 [CompletionCounts] 를 그대로 드러내는 것이
 * 전부이므로, 여기서 고정하는 것은 "헤더가 어떤 이름으로 무엇을 읽는지"다.
 */
class LoopRatesTest {

    @Test fun `percentages are whole numbers and the denominator is exposed`() {
        val rates = LoopRates(CompletionCounts(done = 52, skipped = 15, unanswered = 60))
        assertEquals(127, rates.totalCount)
        assertEquals(52, rates.doneCount)
        assertEquals(41, rates.donePercent)
        assertEquals(53, rates.responsePercent)
        assertEquals(12, rates.skipPercent)
    }

    @Test fun `an empty scope has no percentage at all`() {
        assertEquals(0, LoopRates.Empty.totalCount)
        assertNull(LoopRates.Empty.donePercent)
        assertNull(LoopRates.Empty.responsePercent)
        assertNull(LoopRates.Empty.skipPercent)
        assertFalse(LoopRates.Empty.isReliable)
    }

    @Test fun `a single record is not enough to claim a hundred percent`() {
        val one = LoopRates(CompletionCounts(done = 1))
        assertEquals(100, one.donePercent)
        // 값 자체는 계산되지만 헤더는 isReliable 이 false 인 동안 퍼센트를 숨긴다.
        assertFalse(one.isReliable)
        assertTrue(LoopRates(CompletionCounts(done = 1, unanswered = 2)).isReliable)
    }
}
