package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.TimeSource
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1HOUR
import com.pnd.android.loop.util.MS_1MIN
import java.time.LocalDate
import kotlin.math.roundToLong

/** 평소 소요시간을 낼 때 거슬러 볼 기간과 표본 수. */
private const val USUAL_DURATION_SCAN_DAYS = 60L
private const val USUAL_DURATION_SAMPLE_LIMIT = 15
internal const val USUAL_DURATION_MIN_SAMPLES = 3

/** 타이머를 끄지 않고 둔 기록 등 소요시간으로 믿기 어려운 값은 표본에서 뺀다. */
private val USUAL_DURATION_RANGE = MS_1MIN..12 * MS_1HOUR

/**
 * 이 루프를 한 번 하는 데 보통 걸린 시간(분). 표본이 [USUAL_DURATION_MIN_SAMPLES] 개 미만이면 null.
 *
 * 실제로 잰 기록(MEASURED)과 사용자가 입력한 기록(USER_ENTERED)만 쓴다. 계획 시각(PLANNED)은
 * 실제로 걸린 시간이 아니므로 뺀다.
 *
 * 중앙값을 쓰는 이유는 가끔 섞이는 긴 기록(멈춤을 늦게 누른 날)에 끌려가지 않기 위해서다.
 * 화면에 "평소 25분"으로 보이므로 10분 이상은 5분 단위로 반올림한다.
 */
fun LoopTimeline.usualDurationMinutes(today: LocalDate): Int? {
    val samples = history.responses.asSequence()
        .filter { it.isDone() }
        .filter { it.timeSource == TimeSource.MEASURED || it.timeSource == TimeSource.USER_ENTERED }
        .filter { it.localDate() in today.minusDays(USUAL_DURATION_SCAN_DAYS)..today }
        .sortedByDescending { it.localDate() }
        .mapNotNull { it.durationMs()?.takeIf { ms -> ms in USUAL_DURATION_RANGE } }
        .take(USUAL_DURATION_SAMPLE_LIMIT)
        .sorted()
        .toList()
    if (samples.size < USUAL_DURATION_MIN_SAMPLES) return null

    val middle = samples.size / 2
    val median = if (samples.size % 2 == 1) samples[middle] else (samples[middle - 1] + samples[middle]) / 2
    val minutes = (median.toDouble() / MS_1MIN).roundToLong()
    val rounded = if (minutes < 10) minutes.coerceAtLeast(1) else (minutes / 5.0).roundToLong() * 5
    return rounded.toInt()
}

/** 잰 시간이 있으면 그것을, 없으면 시작·종료 시각의 차(자정을 넘기면 하루를 더한다). */
private fun LoopDoneVo.durationMs(): Long? {
    measuredDurationMs()?.let { return it }
    if (startInDay !in 0 until MS_1DAY || endInDay !in 0 until MS_1DAY) return null
    val raw = endInDay - startInDay
    return if (raw >= 0) raw else raw + MS_1DAY
}
