package com.pnd.android.loop.data.history

import com.pnd.android.loop.state.isInProgress
import com.pnd.android.loop.state.isRespond
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import java.time.LocalDateTime

internal fun nextWeekStart(date: LocalDate): LocalDate = date.plusDays((8 - date.dayOfWeek.value).toLong())

internal fun shouldPreserveOccurrence(day: ResolvedLoopDay?, now: LocalDateTime): Boolean {
    if (day == null) return false
    if (day.response.done.isRespond() || day.response.done.isInProgress()) return true
    return day.scheduled && !day.loop.isAnyTime && day.loop.startInDay <= now.toLocalTime().toMs()
}
