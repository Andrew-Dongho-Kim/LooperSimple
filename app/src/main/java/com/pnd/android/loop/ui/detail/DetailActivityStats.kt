package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.history.CompletionCounts
import com.pnd.android.loop.data.history.countActivity
import com.pnd.android.loop.state.isSettledOn
import java.time.DayOfWeek
import java.time.LocalDate

internal const val ACTIVITY_WINDOW_DAYS = 28L
private const val DAYS_PER_ACTIVITY_GROUP = 7L

/** 최근 28일을 겹치지 않는 7일 구간으로 나눈다. 달력 주나 주간 목표 달성률이 아니다. */
internal data class ActivityPeriod(
    val start: LocalDate,
    val end: LocalDate,
    val counts: CompletionCounts,
)

internal data class WeekdayActivity(
    val day: DayOfWeek,
    val counts: CompletionCounts,
)

/** UI에는 표시할 값만 전달한다. 집계와 비교 기준은 이 파일에서 관리한다. */
internal data class DetailActivityStats(
    val start: LocalDate,
    val end: LocalDate,
    val counts: CompletionCounts,
    val periods: List<ActivityPeriod>,
    val weekdays: List<WeekdayActivity>,
    /** 직전의 동일한 28일과 비교한다. 두 기간을 비교할 기록이 부족하면 null. */
    val doneDelta: Int?,
) {
    companion object {
        fun empty(today: LocalDate) = DetailActivityStats(
            start = today,
            end = today,
            counts = CompletionCounts(),
            periods = emptyList(),
            weekdays = emptyList(),
            doneDelta = null,
        )
    }
}

/**
 * 완료율에 사용할 확정 기록만 남긴다.
 * - 비활성·진행 중·미래·생성 전 기록은 제외한다.
 * - 오늘의 미응답은 아직 완료할 수 있으므로 제외한다. 오늘의 완료·건너뜀은 바로 반영한다.
 * - 입력 날짜는 LoopTimeline에서 당시 일정과 저장 기록을 함께 해석한 결과다.
 */
internal fun resolvedActivityRecords(
    statesByDate: Map<LocalDate, Int>,
    createdDate: LocalDate,
    today: LocalDate,
): Map<LocalDate, Int> = statesByDate.filter { (date, state) ->
    date >= createdDate && state.isSettledOn(date, today)
}

/** [resolvedRecords]는 [resolvedActivityRecords]를 통과한 날짜별 기록이다. */
internal fun computeDetailActivityStats(
    resolvedRecords: Map<LocalDate, Int>,
    createdDate: LocalDate,
    today: LocalDate,
): DetailActivityStats {
    val windowStart = today.minusDays(ACTIVITY_WINDOW_DAYS - 1)
    val previousStart = windowStart.minusDays(ACTIVITY_WINDOW_DAYS)
    val current = resolvedRecords.within(windowStart, today)
    val previous = resolvedRecords.within(previousStart, windowStart.minusDays(1))
    val currentCounts = countActivity(current.values)
    val previousCounts = countActivity(previous.values)

    // 생성 직후의 짧은 기간을 온전한 28일 기간과 비교하지 않는다.
    val canCompare = createdDate <= previousStart && currentCounts.total > 0 && previousCounts.total > 0

    val periods = (0 until 4).mapNotNull { index ->
        val start = windowStart.plusDays(index * DAYS_PER_ACTIVITY_GROUP)
        val end = start.plusDays(DAYS_PER_ACTIVITY_GROUP - 1)
        if (end < createdDate) return@mapNotNull null
        ActivityPeriod(
            start = maxOf(start, createdDate),
            end = end,
            counts = countActivity(current.within(start, end).values),
        )
    }
    val recordsByWeekday = current.entries.groupBy { it.key.dayOfWeek }
    val weekdays = DayOfWeek.values().map { day ->
        WeekdayActivity(day, countActivity(recordsByWeekday[day].orEmpty().map { it.value }))
    }

    return DetailActivityStats(
        start = maxOf(windowStart, createdDate).coerceAtMost(today),
        end = today,
        counts = currentCounts,
        periods = periods,
        weekdays = weekdays,
        doneDelta = if (canCompare) currentCounts.done - previousCounts.done else null,
    )
}

private fun Map<LocalDate, Int>.within(start: LocalDate, end: LocalDate): Map<LocalDate, Int> =
    filterKeys { it >= start && it <= end }
