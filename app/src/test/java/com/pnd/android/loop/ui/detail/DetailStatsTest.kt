package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.common.NO_WEEKLY_GOAL
import com.pnd.android.loop.ui.statisctics.computeLoopStreak
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 상세 화면이 보여 주는 수치의 규칙을 고정해 둔다.
 *
 * 특히 스트릭은 "연속된 달력 일자"가 아니라 "연속된 활동일"이어야 한다. 예전 규칙으로는 주중
 * 루프가 완벽하게 지켜져도 매주 토요일에 0으로 끊겼다.
 */
class DetailStatsTest {

    private fun date(iso: String) = LocalDate.parse(iso)

    private fun ms(date: LocalDate) =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun done(date: LocalDate, state: Int = DoneState.DONE) =
        LoopDoneVo(loopId = 1, date = ms(date), done = state)

    // 2026-08-10(월) ~ 2026-08-14(금) 은 같은 주의 평일이다.
    private val monday = date("2026-08-10")
    private val friday = date("2026-08-14")

    @Test
    fun `주중 루프의 스트릭은 주말에 끊기지 않는다`() {
        // 지난 금요일과 이번 주 월~금을 모두 지켰다. 사이의 토·일은 활동일이 아니다.
        val doneDates = listOf(
            date("2026-08-07"), // 지난 금요일
            date("2026-08-10"),
            date("2026-08-11"),
            date("2026-08-12"),
            date("2026-08-13"),
            date("2026-08-14"),
        )

        val streak = computeLoopStreak(
            doneDates = doneDates,
            activeDays = LoopDay.WEEKDAYS,
            createdDate = date("2026-08-01"),
            today = friday,
        )

        assertEquals(6, streak.current)
        assertEquals(6, streak.longest)
    }

    @Test
    fun `활동일을 하루 빠뜨리면 스트릭이 끊긴다`() {
        val doneDates = listOf(
            date("2026-08-10"),
            // 8/11(화) 빠짐
            date("2026-08-12"),
            date("2026-08-13"),
            date("2026-08-14"),
        )

        val streak = computeLoopStreak(
            doneDates = doneDates,
            activeDays = LoopDay.WEEKDAYS,
            createdDate = date("2026-08-01"),
            today = friday,
        )

        assertEquals(3, streak.current)
        assertEquals(3, streak.longest)
    }

    @Test
    fun `오늘 아직 하지 않아도 어제까지의 스트릭은 살아 있다`() {
        val streak = computeLoopStreak(
            doneDates = listOf(date("2026-08-12"), date("2026-08-13")),
            activeDays = LoopDay.WEEKDAYS,
            createdDate = date("2026-08-01"),
            today = friday, // 8/14 은 아직 미완료
        )

        assertEquals(2, streak.current)
    }

    @Test
    fun `이번 주 진행은 월요일부터 오늘까지만 센다`() {
        val doneStateByDate = mapOf(
            date("2026-08-09") to DoneState.DONE, // 지난주 일요일 — 세지 않는다
            monday to DoneState.DONE,
            date("2026-08-11") to DoneState.DONE,
        )

        val weekly = computeWeeklyProgress(
            doneStateByDate = doneStateByDate,
            activeDays = LoopDay.WEEKDAYS,
            weeklyGoal = NO_WEEKLY_GOAL,
            createdDate = date("2026-08-01"),
            today = friday,
        )

        assertEquals(2, weekly.done)
        // 목표가 없으면 이번 주 활동 요일 수(평일 5일)가 그대로 분모가 된다.
        assertEquals(5, weekly.target)
        assertEquals(false, weekly.hasGoal)
    }

    @Test
    fun `주간 목표를 정하면 그 값이 분모가 되고 달성 여부가 결정된다`() {
        val doneStateByDate = mapOf(
            monday to DoneState.DONE,
            date("2026-08-11") to DoneState.DONE,
            date("2026-08-12") to DoneState.DONE,
        )

        val weekly = computeWeeklyProgress(
            doneStateByDate = doneStateByDate,
            activeDays = LoopDay.EVERYDAY,
            weeklyGoal = 3,
            createdDate = date("2026-08-01"),
            today = friday,
        )

        assertEquals(3, weekly.done)
        assertEquals(3, weekly.target)
        assertEquals(true, weekly.hasGoal)
        assertEquals(true, weekly.isAchieved)
    }

    @Test
    fun `응답 개수는 비활성 기록을 빼고 센다`() {
        val responses = listOf(
            done(monday, DoneState.DONE),
            done(date("2026-08-11"), DoneState.SKIP),
            done(date("2026-08-12"), DoneState.NO_RESPONSE),
            done(date("2026-08-13"), DoneState.DISABLED),
        )

        val stats = computeDetailStats(
            responses = responses,
            memoDates = emptySet(),
            activeDays = LoopDay.EVERYDAY,
            weeklyGoal = NO_WEEKLY_GOAL,
            createdDate = date("2026-08-01"),
            today = friday,
        )

        assertEquals(3, stats.totalCount)
        assertEquals(1, stats.doneCount)
        assertEquals(1, stats.skipCount)
        assertEquals(1, stats.noResponseCount)
        assertEquals(33, stats.donePercent)
    }
}
