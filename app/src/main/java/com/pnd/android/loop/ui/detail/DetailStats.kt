package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.common.NO_WEEKLY_GOAL
import com.pnd.android.loop.ui.statisctics.StreakStat
import com.pnd.android.loop.ui.statisctics.computeLoopStreak
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.toLocalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// 상세 화면이 그리는 모든 수치. Compose 와 무관한 순수 계산만 모아 두어 단위 테스트가 가능하고,
// 실제 계산은 ViewModel 이 백그라운드 디스패처에서 한 번만 돌린다(컴포지션 중에 돌지 않는다).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 이번 주(월~일) 성과.
 *
 * @param done 이번 주에 완료한 횟수(오늘까지)
 * @param target 이번 주에 채워야 하는 횟수. 주간 목표가 있으면 그 값, 없으면 이번 주 활동 요일 수
 * @param hasGoal 사용자가 직접 정한 주간 목표인지(false 면 활동 요일 수에서 유도한 값)
 * @param trend 지난주 같은 시점 대비 완료 수 변화(양수=개선, 0=유지, 음수=주춤)
 */
internal data class WeeklyProgress(
    val done: Int,
    val target: Int,
    val hasGoal: Boolean,
    val trend: Int,
) {
    /** 목표 대비 진행률(0f..1f). 목표가 0이면 진행률을 말할 수 없으므로 0f. */
    val fraction: Float get() = if (target <= 0) 0f else (done.toFloat() / target).coerceIn(0f, 1f)
    val isAchieved: Boolean get() = target > 0 && done >= target

    companion object {
        val Empty = WeeklyProgress(done = 0, target = 0, hasGoal = false, trend = 0)
    }
}

/**
 * 일별 완료율 추세.
 *
 * @param rates 각 날짜의 [windowDays] 일 롤링 완료율(0f..1f), 과거→오늘 순
 * @param deltaPercent 약 2주 전 대비 완료율 변화(%p, 양수=상승)
 */
internal data class DailyTrend(
    val rates: List<Float>,
    val deltaPercent: Int,
)

/** 한 달의 완료율. */
internal data class MonthlyRate(
    val month: YearMonth,
    val rate: Float,
)

/** 상세 화면 전체가 공유하는 계산 결과 묶음. */
internal data class DetailStats(
    val today: LocalDate,
    val createdDate: LocalDate,
    /** 날짜 → [DoneState]. 달력·주간 스트립·스트릭이 모두 이 인덱스를 쓴다. */
    val doneStateByDate: Map<LocalDate, Int>,
    val totalCount: Int,
    val doneCount: Int,
    val skipCount: Int,
    val noResponseCount: Int,
    val donePercent: Int,
    val streak: StreakStat,
    val weekly: WeeklyProgress,
    val dailyTrend: DailyTrend?,
    val weekdayRates: List<Float?>,
    val monthlyRates: List<MonthlyRate>,
    /** 회고 메모를 남긴 날짜들. 달력 마커와 접힌 행 요약이 함께 쓴다. */
    val memoDates: Set<LocalDate>,
) {
    val hasAnyRecord: Boolean get() = totalCount > 0

    companion object {
        fun empty(today: LocalDate = LocalDate.now()) = DetailStats(
            today = today,
            createdDate = today,
            doneStateByDate = emptyMap(),
            totalCount = 0,
            doneCount = 0,
            skipCount = 0,
            noResponseCount = 0,
            donePercent = 0,
            streak = StreakStat(current = 0, longest = 0),
            weekly = WeeklyProgress.Empty,
            dailyTrend = null,
            weekdayRates = List(7) { null },
            monthlyRates = emptyList(),
            memoDates = emptySet(),
        )
    }
}

/**
 * 상세 화면이 필요로 하는 모든 수치를 한 번에 계산한다.
 *
 * 예전에는 요약 헤더와 기록 섹션이 같은 `responses` 로 같은 날짜 인덱스를 각자 만들고,
 * 개수 넷을 DAO 에서 따로 세어 왔다. `responses` 가 이미 이 루프의 모든 행이므로
 * 개수도 여기서 함께 세면 flow 넷과 중복 계산 하나가 사라진다.
 */
internal fun computeDetailStats(
    responses: List<LoopDoneVo>,
    memoDates: Set<LocalDate>,
    activeDays: Int,
    weeklyGoal: Int,
    createdDate: LocalDate,
    today: LocalDate,
): DetailStats {
    val doneStateByDate = responses.associate { it.date.toLocalDate() to it.done }

    var totalCount = 0
    var doneCount = 0
    var skipCount = 0
    responses.forEach { response ->
        if (response.isDisabled()) return@forEach
        totalCount++
        when {
            response.isDone() -> doneCount++
            response.isSkip() -> skipCount++
        }
    }
    val noResponseCount = (totalCount - doneCount - skipCount).coerceAtLeast(0)

    return DetailStats(
        today = today,
        createdDate = createdDate,
        doneStateByDate = doneStateByDate,
        totalCount = totalCount,
        doneCount = doneCount,
        skipCount = skipCount,
        noResponseCount = noResponseCount,
        donePercent = if (totalCount == 0) 0 else (doneCount * 100f / totalCount).roundToInt(),
        streak = computeLoopStreak(
            doneDates = doneStateByDate.filterValues { it == DoneState.DONE }.keys,
            activeDays = activeDays,
            createdDate = createdDate,
            today = today,
        ),
        weekly = computeWeeklyProgress(
            doneStateByDate = doneStateByDate,
            activeDays = activeDays,
            weeklyGoal = weeklyGoal,
            createdDate = createdDate,
            today = today,
        ),
        dailyTrend = computeDailyTrend(
            responses = responses,
            createdDate = createdDate,
            today = today,
        ),
        weekdayRates = computeWeekdayRates(responses = responses),
        monthlyRates = computeMonthlyRates(responses = responses),
        memoDates = memoDates,
    )
}

/** 이번 주의 첫날(월요일). 주간 목표는 달력 주 단위로만 뜻이 통하므로 롤링 7일을 쓰지 않는다. */
internal fun weekStartOf(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

/** [weekStartOf] 기준 이번 주 7일을 월요일부터 순서대로. */
internal fun weekDatesOf(date: LocalDate): List<LocalDate> {
    val start = weekStartOf(date)
    return (0..6).map { start.plusDays(it.toLong()) }
}

internal fun computeWeeklyProgress(
    doneStateByDate: Map<LocalDate, Int>,
    activeDays: Int,
    weeklyGoal: Int,
    createdDate: LocalDate,
    today: LocalDate,
): WeeklyProgress {
    val weekStart = weekStartOf(today)
    val daysElapsed = (today.toEpochDay() - weekStart.toEpochDay()).toInt()

    fun countDone(from: LocalDate, days: Int) = (0..days)
        .count { doneStateByDate[from.plusDays(it.toLong())] == DoneState.DONE }

    val done = countDone(from = weekStart, days = daysElapsed)
    // 지난주는 같은 요일까지만 센다. 주 초에 "지난주보다 주춤"이 뜨는 착시를 막는다.
    val donePrev = countDone(from = weekStart.minusWeeks(1), days = daysElapsed)

    // 목표가 없으면 이번 주 활동 요일 수가 곧 목표다. 이번 주 전체를 세어 두어야 주가
    // 흐르는 동안 분모가 자라지 않는다.
    val activeThisWeek = weekDatesOf(today).count {
        !it.isBefore(createdDate) && activeDays.isOn(dayForLoop(it))
    }

    return WeeklyProgress(
        done = done,
        target = if (weeklyGoal > NO_WEEKLY_GOAL) weeklyGoal else activeThisWeek,
        hasGoal = weeklyGoal > NO_WEEKLY_GOAL,
        trend = done.compareTo(donePrev),
    )
}

/**
 * 최근 [maxPoints]일에 대해 [windowDays]일 롤링 완료율을 계산한다.
 * 각 날짜의 값 = (창 안의 완료 수) / (창 안의 응답 대상 수). 응답 대상은 비활성(DISABLED)이 아닌 기록.
 * 기록이 전혀 없거나 구간이 너무 짧으면 null 을 돌려 타일을 빈 상태로 둔다.
 */
internal fun computeDailyTrend(
    responses: List<LoopDoneVo>,
    createdDate: LocalDate,
    today: LocalDate,
    windowDays: Int = 7,
    maxPoints: Int = 60,
): DailyTrend? {
    val doneByDate = responses
        .filter { !it.isDisabled() }
        .associate { it.date.toLocalDate() to it.isDone() }
    if (doneByDate.isEmpty()) return null

    val start = maxOf(createdDate, today.minusDays((maxPoints - 1).toLong()))
    val totalDays = (today.toEpochDay() - start.toEpochDay()).toInt() + 1
    if (totalDays < 2) return null

    val rates = (0 until totalDays).map { offset ->
        val day = start.plusDays(offset.toLong())
        var enabled = 0
        var done = 0
        var cursor = day.minusDays((windowDays - 1).toLong())
        while (!cursor.isAfter(day)) {
            doneByDate[cursor]?.let { isDone ->
                enabled++
                if (isDone) done++
            }
            cursor = cursor.plusDays(1)
        }
        if (enabled == 0) 0f else done.toFloat() / enabled
    }

    // 약 2주 전 지점과 비교해 최근 추세를 %p 로 낸다(데이터가 짧으면 첫 지점과 비교).
    val referenceIndex = (rates.lastIndex - 14).coerceAtLeast(0)
    val deltaPercent = ((rates.last() - rates[referenceIndex]) * 100).roundToInt()
    return DailyTrend(rates = rates, deltaPercent = deltaPercent)
}

/**
 * 최근 [monthsBack]개월의 월별 완료율. 데이터가 있는 달만, 오래된 달→최신 달 순으로 담는다.
 * 완료율 = 그 달의 완료 수 / 응답 대상(비활성 제외) 수.
 */
internal fun computeMonthlyRates(
    responses: List<LoopDoneVo>,
    monthsBack: Int = 6,
): List<MonthlyRate> {
    val enabled = responses.filter { !it.isDisabled() }
    if (enabled.isEmpty()) return emptyList()

    return enabled
        .groupBy { YearMonth.from(it.date.toLocalDate()) }
        .map { (yearMonth, records) ->
            MonthlyRate(
                month = yearMonth,
                rate = records.count { it.isDone() }.toFloat() / records.size,
            )
        }
        .sortedBy { it.month }
        .takeLast(monthsBack)
}

/**
 * 요일별 완료율(0f..1f). 인덱스 0=일요일 … 6=토요일(달력 헤더와 같은 순서).
 * 그 요일에 응답 대상 기록이 하나도 없으면 해당 칸은 null.
 */
internal fun computeWeekdayRates(
    responses: List<LoopDoneVo>,
): List<Float?> {
    val byDayOfWeek = responses
        .filter { !it.isDisabled() }
        .groupBy { it.date.toLocalDate().dayOfWeek }

    return (0..6).map { index ->
        val dayOfWeek = DayOfWeek.of(if (index == 0) 7 else index)
        val records = byDayOfWeek[dayOfWeek]
        if (records.isNullOrEmpty()) null
        else records.count { it.isDone() }.toFloat() / records.size
    }
}

/** 변화량을 "+12%p" / "-5%p" / "0%p" 형태의 짧은 배지 문자열로 만든다. */
internal fun formatDeltaPercent(delta: Int): String =
    (if (delta > 0) "+$delta" else "$delta") + "%p"

/** 활동 요일 수. 접힌 스케줄 행의 요약과 목표 선택기의 기본값 계산에 쓴다. */
internal fun activeDayCount(activeDays: Int): Int = LoopDay.ALL.count { activeDays.isOn(it) }
