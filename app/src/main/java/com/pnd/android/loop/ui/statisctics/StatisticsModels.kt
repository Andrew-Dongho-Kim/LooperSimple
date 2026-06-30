package com.pnd.android.loop.ui.statisctics

import androidx.annotation.StringRes
import com.pnd.android.loop.R
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.toMs
import java.time.LocalDate

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
 */
data class StatisticsSummary(
    val completedCount: Int,
    val completionRate: Float,
    val activeLoops: Int,
    val activeDays: Int,
) {
    companion object {
        val Empty = StatisticsSummary(
            completedCount = 0,
            completionRate = 0f,
            activeLoops = 0,
            activeDays = 0,
        )
    }
}

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
