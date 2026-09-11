package com.pnd.android.loop.ui.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate
import java.time.YearMonth

sealed interface AchievementLoadState<out T> {
    data object Loading : AchievementLoadState<Nothing>
    data object Error : AchievementLoadState<Nothing>
    data class Ready<T>(val value: T) : AchievementLoadState<T>
}

internal fun <T> Flow<T>.asAchievementState(): Flow<AchievementLoadState<T>> =
    map<T, AchievementLoadState<T>> { AchievementLoadState.Ready(it) }
        .onStart { emit(AchievementLoadState.Loading) }
        .catch { emit(AchievementLoadState.Error) }

data class AchievementCalendarDay(val doneCount: Int, val totalCount: Int, val hasNote: Boolean)

internal fun achievementWeekStart(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value % 7).toLong())

internal fun achievementMonthDates(month: YearMonth): List<LocalDate> {
    val start = achievementWeekStart(month.atDay(1))
    return List(42) { start.plusDays(it.toLong()) }
}
