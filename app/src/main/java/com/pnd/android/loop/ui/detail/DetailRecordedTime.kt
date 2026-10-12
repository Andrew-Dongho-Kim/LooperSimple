package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.state.isInProgress
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Only stored response times are displayed; current schedule settings are never substituted. */
internal data class DetailRecordedTime(
    val start: LocalTime,
    val end: LocalTime?,
    val endDayOffset: Long,
    val durationMs: Long?,
    val source: Int,
    val running: Boolean,
)

internal fun recordedTimeOf(
    response: LoopDoneVo,
    date: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): DetailRecordedTime? {
    val running = response.done.isInProgress()
    if (response.timeSource == LoopDoneVo.TimeSource.MEASURED && response.startedAt != null) {
        val start = Instant.ofEpochMilli(response.startedAt).atZone(zone)
        val end = response.endedAt?.takeIf { !running && it >= response.startedAt }
            ?.let { Instant.ofEpochMilli(it).atZone(zone) }
        return DetailRecordedTime(
            start.toLocalTime(), end?.toLocalTime(),
            end?.let { ChronoUnit.DAYS.between(date, it.toLocalDate()).coerceAtLeast(0) } ?: 0,
            if (running) null else response.measuredDurationMs(), response.timeSource, running,
        )
    }
    val dayMs = 86_400_000L
    val startMs = response.startInDay.takeIf { it in 0 until dayMs } ?: return null
    // Legacy default 0/0 contains no trustworthy timing information.
    if (!running && response.timeSource == LoopDoneVo.TimeSource.UNKNOWN && startMs == 0L && response.endInDay == 0L) return null
    val endMs = response.endInDay.takeIf { !running && it in 0 until dayMs }
    val overnight = endMs != null && endMs < startMs
    return DetailRecordedTime(
        LocalTime.ofNanoOfDay(startMs * 1_000_000),
        endMs?.let { LocalTime.ofNanoOfDay(it * 1_000_000) },
        if (overnight) 1 else 0,
        endMs?.let { it - startMs + if (overnight) dayMs else 0 },
        response.timeSource, running,
    )
}
