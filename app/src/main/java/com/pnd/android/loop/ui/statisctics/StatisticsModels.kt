package com.pnd.android.loop.ui.statisctics

import androidx.annotation.StringRes
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopResponseRecord
import com.pnd.android.loop.data.LoopWithStatistics
import com.pnd.android.loop.data.MonthlyCompletionCount
import com.pnd.android.loop.data.NewLoopRecord
import com.pnd.android.loop.data.isDone
import com.pnd.android.loop.data.isSkip
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1HOUR
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

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
 * 루프 순위를 정렬하는 기준. 각 기준은 자신의 라벨([titleRes])과 정렬 값 추출 방법([selector])을
 * 알고 있어, UI는 선택된 기준으로 내림차순 정렬만 하면 된다.
 */
enum class RankingSortOrder(
    @StringRes val titleRes: Int,
    val selector: (LoopWithStatistics) -> Double,
) {
    COMPLETION_RATE(R.string.stat_ranking_sort_rate, { it.doneRate.toDouble() }),
    INVESTED_TIME(R.string.stat_ranking_sort_time, { it.investedTimeMs.toDouble() }),
    DONE_COUNT(R.string.stat_ranking_sort_count, { it.doneCount.toDouble() });
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

/**
 * 통계 화면 상단 탭. 지표 10종이 늘어나면서 한 화면에 다 담으면 스크롤이 과해지므로,
 * 목적별 4개 카테고리로 나눈다. (요약 / 패턴 / 습관 / 성취)
 */
enum class StatisticsTab(@StringRes val titleRes: Int) {
    SUMMARY(R.string.stat_tab_summary),
    PATTERN(R.string.stat_tab_pattern),
    HABIT(R.string.stat_tab_habit),
    ACHIEVEMENT(R.string.stat_tab_achievement),
}

/**
 * 시간대(0~23시)별 완료 횟수. 실제 시작 시각을 기준으로 분류한 히트맵에 사용한다.
 * [ratio]는 가장 많이 완료한 시간대 대비 비율(0f..1f).
 */
data class HourlyCompletion(
    val hour: Int,
    val count: Int,
    val ratio: Float,
)

/**
 * 계획 대비 실제 시작 분석. 계획 시각이 있는(언제든지 아님) 완료 기록만 대상으로 한다.
 *
 * @param sampleCount 분석에 사용된 완료 기록 수.
 * @param avgStartDiffMs 평균 시작 시각 차이(ms). 양수면 계획보다 늦게(미룸), 음수면 일찍 시작.
 * @param onTimeCount 계획 시각 이하에 시작한(정시/일찍) 완료 기록 수.
 */
data class PlanVsActualStat(
    val sampleCount: Int,
    val avgStartDiffMs: Long,
    val onTimeCount: Int,
) {
    val onTimeRate: Float get() = if (sampleCount == 0) 0f else onTimeCount.toFloat() / sampleCount
    val hasData: Boolean get() = sampleCount > 0

    companion object {
        val Empty = PlanVsActualStat(sampleCount = 0, avgStartDiffMs = 0L, onTimeCount = 0)
    }
}

/**
 * 회고 작성 현황. 완료한 기록 중 회고 메모를 남긴 비율을 본다.
 *
 * @param doneCount 완료(DONE) 기록 수.
 * @param writtenCount 그중 회고를 남긴 수.
 */
data class RetrospectStat(
    val doneCount: Int,
    val writtenCount: Int,
) {
    val rate: Float get() = if (doneCount == 0) 0f else writtenCount.toFloat() / doneCount
    val hasData: Boolean get() = doneCount > 0

    companion object {
        val Empty = RetrospectStat(doneCount = 0, writtenCount = 0)
    }
}

/** 습관 건강 상태. 최근 완료율이 그전 대비 얼마나 떨어졌는지로 위험도를 나눈다. */
enum class HabitHealthLevel { WATCH, AT_RISK }

/**
 * 최근 완료율이 하락 중인 루프. (안정적인 루프는 목록에서 제외한다.)
 *
 * @param recentRate 최근 구간 완료율(0f..1f).
 * @param previousRate 직전 구간 완료율(0f..1f).
 */
data class HabitHealth(
    val loopId: Int,
    val title: String,
    val color: Int,
    val recentRate: Float,
    val previousRate: Float,
    val level: HabitHealthLevel,
) {
    val delta: Float get() = recentRate - previousRate
}

/** 신규 루프 정착 상태. 생성 이후 완료율로 판단한다. */
enum class SettlingLevel { SETTLED, SETTLING, STRUGGLING }

/**
 * 최근에 만든 루프의 정착 현황.
 *
 * @param daysSinceCreated 생성 후 지난 일수.
 * @param doneRate 생성 이후 완료율(0f..1f).
 * @param respondedCount 응답한 전체 횟수(표본이 적으면 판단을 보류한다).
 */
data class NewLoopSettling(
    val loopId: Int,
    val title: String,
    val color: Int,
    val daysSinceCreated: Int,
    val doneRate: Float,
    val respondedCount: Int,
    val level: SettlingLevel,
)

/** 특정 월의 완료율. 완료율 추세 라인 차트의 한 점. */
data class CompletionRatePoint(
    val yearMonth: YearMonth,
    val rate: Float,
)

/** 마일스톤 종류. 각 종류가 자신의 라벨과 값 단위를 안다. */
enum class MilestoneType(@StringRes val labelRes: Int) {
    INVESTED_HOURS(R.string.stat_milestone_invested_hours),
    TOTAL_DONE(R.string.stat_milestone_total_done),
    LONGEST_STREAK(R.string.stat_milestone_longest_streak),
}

/**
 * 누적 성취 마일스톤. 현재 값과 달성한 최고 임계값, 다음 목표를 담는다.
 *
 * @param value 현재 값(시간/횟수/일).
 * @param reached 달성한 최고 임계값(0이면 아직 첫 목표 전).
 * @param next 다음 목표 임계값(null이면 최고 단계 달성).
 */
data class Milestone(
    val type: MilestoneType,
    val value: Long,
    val reached: Long,
    val next: Long?,
) {
    // 이전 임계값(reached)에서 다음 임계값(next)까지의 진행률(0f..1f).
    val progress: Float
        get() = when {
            next == null -> 1f
            next <= reached -> 1f
            else -> ((value - reached).toFloat() / (next - reached)).coerceIn(0f, 1f)
        }
}

/**
 * 이번 달 완료 횟수 예측. 현재 페이스(지금까지 완료/경과 일수)를 이번 달 전체 일수로 환산한다.
 *
 * @param doneSoFar 이번 달 지금까지 완료한 횟수.
 * @param projectedTotal 현재 페이스 유지 시 월말 예상 완료 횟수.
 */
data class MonthlyProjection(
    val doneSoFar: Int,
    val projectedTotal: Int,
    val daysElapsed: Int,
    val daysInMonth: Int,
) {
    val hasData: Boolean get() = doneSoFar > 0

    companion object {
        val Empty = MonthlyProjection(doneSoFar = 0, projectedTotal = 0, daysElapsed = 0, daysInMonth = 0)
    }
}

/**
 * 기간(period) 기반 지표를 한 번의 응답 조회에서 모두 계산해 담는 묶음.
 * (요약 KPI · 시간대 히트맵 · 요일 패턴 · 완벽한 날 · 스킵 · 회고 · 계획대비 실제)
 */
data class PeriodStats(
    val summary: StatisticsSummary = StatisticsSummary.Empty,
    val hourlyStats: List<HourlyCompletion> = emptyList(),
    val dayOfWeekStats: List<DayOfWeekStat> = emptyList(),
    val perfectDays: Int = 0,
    val skipCount: Int = 0,
    val retrospect: RetrospectStat = RetrospectStat.Empty,
    val planVsActual: PlanVsActualStat = PlanVsActualStat.Empty,
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

// region 기간 기반 지표 계산기
// 한 기간의 응답 기록(List<LoopResponseRecord>)에서 여러 지표를 한 번에 뽑아낸다.

/**
 * 한 번의 완료 기록에 투자한 시간(ms). 자정을 넘겨 끝난 경우 하루를 더해 보정한다.
 * (DAO의 getInvestedTimeFlow와 동일한 규칙을 Kotlin에서 재현한다.)
 */
private fun LoopResponseRecord.investedTimeMs(): Long {
    val raw = endInDay - startInDay
    return if (raw >= 0) raw else raw + MS_1DAY
}

/**
 * 기간 응답 기록에서 요약 KPI·시간대·요일·완벽한 날·스킵·회고·계획대비 실제를 한 번에 계산한다.
 */
fun computePeriodStats(records: List<LoopResponseRecord>): PeriodStats {
    if (records.isEmpty()) return PeriodStats()

    val doneRecords = records.filter { it.done.isDone() }
    val totalResponses = records.size

    val summary = StatisticsSummary(
        completedCount = doneRecords.size,
        completionRate = if (totalResponses == 0) 0f else doneRecords.size.toFloat() / totalResponses,
        activeLoops = records.map { it.loopId }.distinct().size,
        activeDays = doneRecords.map { it.date }.distinct().size,
        investedTimeMs = doneRecords.sumOf { it.investedTimeMs() },
    )

    return PeriodStats(
        summary = summary,
        hourlyStats = computeHourlyCompletions(doneRecords),
        dayOfWeekStats = computeDoneDayOfWeekStats(doneRecords),
        perfectDays = computePerfectDays(records),
        skipCount = records.count { it.done.isSkip() },
        retrospect = computeRetrospectStat(doneRecords),
        planVsActual = computePlanVsActual(doneRecords),
        isEmpty = false,
    )
}

/** ① 완료 기록을 실제 시작 시각의 시간대(0~23시)로 분류한다. 시각이 없는(-1) 기록은 제외. */
fun computeHourlyCompletions(doneRecords: List<LoopResponseRecord>): List<HourlyCompletion> {
    val countByHour = doneRecords
        .filter { it.startInDay >= 0 }
        .groupingBy { (it.startInDay / MS_1HOUR).toInt().coerceIn(0, 23) }
        .eachCount()
    val busiest = countByHour.values.maxOrNull() ?: 0
    return (0..23).map { hour ->
        val count = countByHour[hour] ?: 0
        HourlyCompletion(
            hour = hour,
            count = count,
            ratio = if (busiest == 0) 0f else count.toFloat() / busiest,
        )
    }
}

/** 완료 기록을 요일별로 묶어 완료 "횟수"와 최다 요일 대비 비율을 계산한다. (요일 꾸준함 차트) */
private fun computeDoneDayOfWeekStats(doneRecords: List<LoopResponseRecord>): List<DayOfWeekStat> {
    val countByDay = doneRecords.groupingBy { it.date.toLocalDate().dayOfWeek }.eachCount()
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

/** ③ 완벽한 날: 하루에 응답한 루프가 모두 완료(DONE)인 날의 수. */
fun computePerfectDays(records: List<LoopResponseRecord>): Int =
    records.groupBy { it.date }.count { (_, dayRecords) ->
        dayRecords.all { it.done.isDone() }
    }

/** ⑧ 완료 기록 중 회고를 남긴 비율. */
fun computeRetrospectStat(doneRecords: List<LoopResponseRecord>): RetrospectStat =
    RetrospectStat(
        doneCount = doneRecords.size,
        writtenCount = doneRecords.count { !it.retrospect.isNullOrBlank() },
    )

/** ⑤ 계획 시각이 있는 완료 기록에서 평균 시작 지연과 정시 비율을 계산한다. */
fun computePlanVsActual(doneRecords: List<LoopResponseRecord>): PlanVsActualStat {
    val samples = doneRecords.filter {
        !it.isAnyTime && it.startInDay >= 0 && it.plannedStartInDay >= 0
    }
    if (samples.isEmpty()) return PlanVsActualStat.Empty

    val diffs = samples.map { it.startInDay - it.plannedStartInDay }
    return PlanVsActualStat(
        sampleCount = samples.size,
        avgStartDiffMs = diffs.sum() / samples.size,
        onTimeCount = diffs.count { it <= 0 },
    )
}

/**
 * ⑥ 최근 완료율이 하락 중인 루프를 골라낸다. (기간 선택과 무관하게 항상 최근 흐름 기준)
 * [records]는 최근 2*[windowDays]일치 응답. 최근/직전 두 구간의 완료율을 비교한다.
 */
fun computeHabitHealth(
    records: List<LoopResponseRecord>,
    today: LocalDate = LocalDate.now(),
    windowDays: Int = 14,
): List<HabitHealth> {
    // 최근 구간 시작 경계(이 날짜 이상이면 '최근', 미만이면 '직전').
    val recentFrom = today.minusDays((windowDays - 1).toLong()).toMs()

    return records.groupBy { it.loopId }.mapNotNull { (_, recs) ->
        val recent = recs.filter { it.date >= recentFrom }
        val previous = recs.filter { it.date < recentFrom }
        // 두 구간 모두 최소 표본(3개)이 있어야 비교가 의미 있다.
        if (recent.size < 3 || previous.size < 3) return@mapNotNull null

        val recentRate = recent.count { it.done.isDone() }.toFloat() / recent.size
        val previousRate = previous.count { it.done.isDone() }.toFloat() / previous.size
        val delta = recentRate - previousRate

        val level = when {
            delta <= -0.25f -> HabitHealthLevel.AT_RISK
            delta <= -0.1f -> HabitHealthLevel.WATCH
            else -> return@mapNotNull null // 안정적이면 목록에서 제외
        }

        val sample = recs.first()
        HabitHealth(
            loopId = sample.loopId,
            title = sample.title,
            color = sample.color,
            recentRate = recentRate,
            previousRate = previousRate,
            level = level,
        )
    }.sortedBy { it.delta } // 가장 많이 하락한 순
}

/** ⑦ 신규 루프의 정착 상태를 완료율로 판단한다. */
fun computeSettling(
    newLoops: List<NewLoopRecord>,
    today: LocalDate = LocalDate.now(),
): List<NewLoopSettling> = newLoops.map { loop ->
    val doneRate = if (loop.respondedCount == 0) 0f else loop.doneCount.toFloat() / loop.respondedCount
    val level = when {
        loop.respondedCount < 3 -> SettlingLevel.SETTLING // 표본이 적어 판단 보류(진행 중으로 표시)
        doneRate >= 0.7f -> SettlingLevel.SETTLED
        doneRate >= 0.4f -> SettlingLevel.SETTLING
        else -> SettlingLevel.STRUGGLING
    }
    NewLoopSettling(
        loopId = loop.loopId,
        title = loop.title,
        color = loop.color,
        daysSinceCreated = ChronoUnit.DAYS.between(loop.created.toLocalDate(), today)
            .toInt().coerceAtLeast(0),
        doneRate = doneRate,
        respondedCount = loop.respondedCount,
        level = level,
    )
}

/** ② 월별 완료율 추세를 최근 [months]개월만 잘라 계산한다. */
fun computeCompletionTrend(
    monthly: List<MonthlyCompletionCount>,
    months: Int = 6,
): List<CompletionRatePoint> = monthly.takeLast(months).map { row ->
    CompletionRatePoint(
        yearMonth = YearMonth.of(row.year, row.month),
        rate = if (row.respondedCount == 0) 0f else row.doneCount.toFloat() / row.respondedCount,
    )
}

// ⑨ 마일스톤 임계값(달성 목표) 단계. 값이 커질수록 다음 목표로 넘어간다.
private val INVESTED_HOUR_THRESHOLDS = listOf(10L, 50L, 100L, 200L, 500L, 1000L)
private val TOTAL_DONE_THRESHOLDS = listOf(50L, 100L, 300L, 500L, 1000L, 3000L)
private val STREAK_THRESHOLDS = listOf(7L, 14L, 30L, 50L, 100L, 365L)

/** ⑨ 누적 투자시간·총 완료 횟수·최장 스트릭에 대한 마일스톤을 계산한다. */
fun computeMilestones(
    totalInvestedMs: Long,
    totalDoneCount: Int,
    longestStreak: Int,
): List<Milestone> = listOf(
    milestoneOf(MilestoneType.INVESTED_HOURS, totalInvestedMs / MS_1HOUR, INVESTED_HOUR_THRESHOLDS),
    milestoneOf(MilestoneType.TOTAL_DONE, totalDoneCount.toLong(), TOTAL_DONE_THRESHOLDS),
    milestoneOf(MilestoneType.LONGEST_STREAK, longestStreak.toLong(), STREAK_THRESHOLDS),
)

private fun milestoneOf(type: MilestoneType, value: Long, thresholds: List<Long>): Milestone =
    Milestone(
        type = type,
        value = value,
        reached = thresholds.filter { it <= value }.maxOrNull() ?: 0L,
        next = thresholds.firstOrNull { it > value },
    )

/** ⑩ 이번 달 완료 횟수와 오늘까지 경과 일수로 월말 완료 횟수를 예측한다. */
fun computeMonthlyProjection(
    doneSoFar: Int,
    today: LocalDate = LocalDate.now(),
): MonthlyProjection {
    val daysElapsed = today.dayOfMonth
    val daysInMonth = today.lengthOfMonth()
    val projected = if (daysElapsed == 0) 0
    else (doneSoFar.toDouble() / daysElapsed * daysInMonth).roundToInt()
    return MonthlyProjection(
        doneSoFar = doneSoFar,
        projectedTotal = projected,
        daysElapsed = daysElapsed,
        daysInMonth = daysInMonth,
    )
}

// endregion
