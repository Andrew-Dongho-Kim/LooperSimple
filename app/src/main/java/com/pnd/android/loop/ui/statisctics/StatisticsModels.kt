package com.pnd.android.loop.ui.statisctics

import androidx.annotation.StringRes
import com.pnd.android.loop.R
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.toMs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Time range that every section of the statistics screen is scoped to.
 *
 * Each entry knows how to label itself ([titleRes]) and how to resolve its
 * inclusive [from]..[to] millisecond range against "today", so the UI only ever
 * deals with the high-level concept and never recomputes month boundaries.
 */
enum class StatisticsPeriod(
    @StringRes private val titleRes: Int? = null,
    private val monthOffset: Int? = null,
) {
    TOTAL(titleRes = R.string.total),
    THIS_MONTH(monthOffset = 0),
    LAST_MONTH(monthOffset = 1),
    TWO_MONTHS_AGO(monthOffset = 2);

    @StringRes
    fun titleRes(today: LocalDate = LocalDate.now()): Int =
        titleRes ?: ABB_MONTHS[today.minusMonths(monthOffset!!.toLong()).monthValue - 1]

    fun from(today: LocalDate = LocalDate.now()): Long = when (monthOffset) {
        null -> 0L
        else -> today.minusMonths(monthOffset.toLong()).withDayOfMonth(1).toMs()
    }

    fun to(today: LocalDate = LocalDate.now()): Long = when (monthOffset) {
        null, 0 -> today.toMs()
        else -> {
            val month = today.minusMonths(monthOffset.toLong())
            month.withDayOfMonth(month.lengthOfMonth()).toMs()
        }
    }
}

/**
 * Headline numbers shown in the KPI cards at the top of the screen.
 *
 * @param completedCount number of loops marked DONE inside the period.
 * @param completionRate done / (done + missed), in 0f..1f.
 * @param activeLoops distinct loops that had any activity in the period.
 * @param activeDays distinct calendar days with at least one completed loop.
 * @param investedTimeMs 기간 내 완료한 루프에 투자한 시간(ms)의 총합.
 */
data class StatisticsSummary(
    val completedCount: Int,
    val completionRate: Float,
    val activeLoops: Int,
    val activeDays: Int,
    val investedTimeMs: Long,
) {
    companion object {
        val Empty = StatisticsSummary(
            completedCount = 0,
            completionRate = 0f,
            activeLoops = 0,
            activeDays = 0,
            investedTimeMs = 0L,
        )
    }
}

/**
 * 연속 달성 스트릭. 완료한 루프가 하루라도 있는 날이 며칠 연속으로 이어졌는지를 센다.
 * 기간 선택과 무관하게 항상 전체 기록을 기준으로 한다.
 *
 * @param current 오늘(또는 아직 오늘 기록이 없다면 어제)부터 거꾸로 이어진 연속 일수.
 * @param longest 전체 기록 중 가장 길었던 연속 일수.
 */
data class StreakStat(
    val current: Int,
    val longest: Int,
)

/**
 * 한 달 동안 루프에 투자한 누적 시간. 월별 막대 차트에서 사용한다.
 * [ratio]는 표시되는 달들 중 가장 큰 값 대비 비율(0f..1f)로, 사용자의 최고 기록을
 * 기준으로 막대가 채워지도록 한다.
 */
data class MonthlyInvestedTime(
    val yearMonth: YearMonth,
    val investedTimeMs: Long,
    val ratio: Float,
)

/**
 * Completion count for a single day of the week, used by the consistency chart.
 * [ratio] is this day's count normalised against the busiest day (0f..1f) so the
 * bars fill relative to the user's own best day.
 */
data class DayOfWeekStat(
    val dayOfWeek: java.time.DayOfWeek,
    val completedCount: Int,
    val ratio: Float,
)

/** Everything one period needs to render the whole screen in a single emission. */
data class StatisticsUiState(
    val summary: StatisticsSummary = StatisticsSummary.Empty,
    val dayOfWeekStats: List<DayOfWeekStat> = emptyList(),
    val isEmpty: Boolean = true,
)

// region 공용 통계 계산기
// 통계 화면과 홈 헤더가 동일한 규칙으로 스트릭/요일 패턴을 구하도록 순수 함수로 분리한다.

/**
 * 완료한 날짜 목록으로부터 현재/최장 연속 달성 스트릭을 계산한다.
 *
 * @param doneDates 완료(DONE) 기록이 하루라도 있는 날짜들(중복·정렬 여부 무관).
 * @param today 현재 연속의 기준일. 테스트를 위해 주입 가능하게 열어 둔다.
 */
fun computeStreak(
    doneDates: List<LocalDate>,
    today: LocalDate = LocalDate.now(),
): StreakStat {
    if (doneDates.isEmpty()) return StreakStat(current = 0, longest = 0)

    // 중복 제거 + 오름차순 정렬(방어적으로 한 번 더).
    val days = doneDates.toSortedSet()

    // 최장 연속: 정렬된 날짜를 훑으며 하루씩 이어지는 구간의 최댓값을 구한다.
    var longest = 1
    var run = 1
    var prev: LocalDate? = null
    for (day in days) {
        prev?.let { run = if (day == it.plusDays(1)) run + 1 else 1 }
        longest = maxOf(longest, run)
        prev = day
    }

    // 현재 연속: 오늘 기록이 있으면 오늘부터, 없으면 어제부터 거꾸로 이어진 일수.
    // (오늘 아직 완료 전이어도 어제까지 이어졌다면 스트릭이 살아있는 것으로 본다.)
    val anchor = when {
        today in days -> today
        today.minusDays(1) in days -> today.minusDays(1)
        else -> null
    }
    var current = 0
    var cursor = anchor
    while (cursor != null && cursor in days) {
        current++
        cursor = cursor.minusDays(1)
    }

    return StreakStat(current = current, longest = longest)
}

/**
 * 완료한 날짜들을 요일(월~일)로 묶어, 각 요일에 며칠씩 완료했는지와 가장 꾸준한 요일 대비
 * 비율([DayOfWeekStat.ratio])을 계산한다. 홈 헤더의 전체 탭 요일 패턴 막대에 사용한다.
 *
 * 여기서 [DayOfWeekStat.completedCount]는 "완료 기록이 있는 날의 수"(요일별 활동 일수)다.
 */
fun computeWeekdayStats(doneDates: List<LocalDate>): List<DayOfWeekStat> {
    val countByDay = doneDates.groupingBy { it.dayOfWeek }.eachCount()
    val busiest = countByDay.values.maxOrNull() ?: 0
    return DayOfWeek.entries.map { day ->
        val count = countByDay[day] ?: 0
        DayOfWeekStat(
            dayOfWeek = day,
            completedCount = count,
            ratio = if (busiest == 0) 0f else count.toFloat() / busiest,
        )
    }
}

// endregion
