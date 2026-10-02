package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 오늘 탭 헤더의 진행률이 쓰는 "오늘" 창을 고정한다.
 *
 * 이 창만 확정 판정(isSettled)을 쓰지 않는다. 확정 판정으로 거르면 분모에 답한 것만 남아 완료율이
 * 사실상 늘 100%가 되고, 시작 시각이 없어 하루 내내 미응답으로 남는 anytime 루프는 완료하기
 * 전까지 수치에 등장하지 못한다.
 */
class TodayProgressTest {
    private val today: LocalDate = LocalDate.of(2026, 9, 10)
    private val created: LocalDate = LocalDate.of(2026, 9, 1)

    private fun loopOf(id: Int, anytime: Boolean) = LoopVo.create(
        id = id, title = "L$id", color = 1, created = created.toMs(),
        startInDay = if (anytime) LoopVo.ANY_TIME else 9 * 3_600_000L,
        endInDay = if (anytime) LoopVo.ANY_TIME else 10 * 3_600_000L,
        activeDays = LoopDay.EVERYDAY, enabled = true, isAnyTime = anytime,
    )

    private fun history(id: Int, anytime: Boolean, state: Int?): LoopHistory {
        val loop = loopOf(id, anytime)
        val revision = LoopRevisionVo(
            revisionId = id.toLong(), loopId = id, recordedAt = created.toMs(),
            effectiveFrom = created.toEpochDay(), goalEffectiveFrom = created.toEpochDay(),
            knownFrom = created.toEpochDay(), settings = LoopSettings.from(loop),
        )
        val responses = state?.let {
            listOf(LoopDoneVo(id, today.toMs(), -1, -1, it, id.toLong(), today.toEpochDay()))
        }.orEmpty()
        return LoopHistory(loop, listOf(revision), responses, emptyList())
    }

    /** 저장소의 todayOccurrences 와 같은 창. */
    private fun todayStates(vararg histories: LoopHistory): List<Int> =
        LoopHistorySnapshot(histories.toList())
            .days(today, today)
            .filter { it.hasOccurrence }
            .map { it.response.done }

    @Test fun `an anytime completion counts exactly like a timed one`() {
        val states = todayStates(
            history(1, anytime = false, state = DoneState.DONE),
            history(2, anytime = true, state = DoneState.DONE),
        )
        val counts = countTodayProgress(states)
        assertEquals(2, counts.done)
        assertEquals(2, counts.total)
        assertEquals(100, counts.completionPercent)
    }

    @Test fun `unanswered anytime loops stay in today's denominator`() {
        // 저장된 응답 행이 아직 없는 루프(7)도 오늘 몫이 있으면 분모에 든다.
        val states = todayStates(
            history(1, anytime = false, state = DoneState.DONE),
            history(2, anytime = false, state = DoneState.NO_RESPONSE),
            history(3, anytime = true, state = DoneState.DONE),
            history(4, anytime = true, state = DoneState.NO_RESPONSE),
            history(5, anytime = true, state = DoneState.IN_PROGRESS),
            history(6, anytime = false, state = null),
            history(7, anytime = true, state = null),
        )
        val counts = countTodayProgress(states)
        assertEquals(2, counts.done)
        assertEquals(7, counts.total)
        assertEquals(29, counts.completionPercent)

        // 확정 판정을 쓰면 답한 둘만 남아 100%가 됐다. 그게 이 창에서 고친 문제다.
        assertEquals(100, countActivity(states.filter { it != DoneState.NO_RESPONSE }).completionPercent)
    }

    @Test fun `a running loop is today's work that is not done yet`() {
        val counts = countTodayProgress(
            todayStates(history(1, anytime = true, state = DoneState.IN_PROGRESS)),
        )
        assertEquals(0, counts.done)
        assertEquals(1, counts.total)
        assertEquals(0, counts.completionPercent)
    }

    @Test fun `a day without any occurrence has no percentage at all`() {
        val offSchedule = loopOf(1, anytime = true).copy(activeDays = LoopDay.MONDAY)
        val revision = LoopRevisionVo(
            revisionId = 1, loopId = 1, recordedAt = created.toMs(),
            effectiveFrom = created.toEpochDay(), goalEffectiveFrom = created.toEpochDay(),
            knownFrom = created.toEpochDay(), settings = LoopSettings.from(offSchedule),
        )
        // 2026-09-10 은 목요일이라 이 루프의 활동 요일이 아니다.
        val counts = countTodayProgress(
            todayStates(LoopHistory(offSchedule, listOf(revision), emptyList(), emptyList())),
        )
        assertEquals(0, counts.total)
        assertEquals(null, counts.completionPercent)
    }
}
