package com.pnd.android.loop.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.EVERYDAY
import com.pnd.android.loop.data.LoopDay.Companion.FRIDAY
import com.pnd.android.loop.data.LoopDay.Companion.MONDAY
import com.pnd.android.loop.data.LoopDay.Companion.SATURDAY
import com.pnd.android.loop.data.LoopDay.Companion.SUNDAY
import com.pnd.android.loop.data.LoopDay.Companion.THURSDAY
import com.pnd.android.loop.data.LoopDay.Companion.TUESDAY
import com.pnd.android.loop.data.LoopDay.Companion.WEDNESDAY
import com.pnd.android.loop.data.LoopDay.Companion.WEEKDAYS
import com.pnd.android.loop.data.LoopDay.Companion.WEEKENDS
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.actualEndInDay
import com.pnd.android.loop.data.actualStartInDay
import com.pnd.android.loop.data.doneState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.BlueGreen
import com.pnd.android.loop.ui.theme.Red300
import com.pnd.android.loop.ui.theme.onSurface
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

val MS_1SEC = TimeUnit.SECONDS.toMillis(1)
val MS_1MIN = TimeUnit.MINUTES.toMillis(1)
val MS_1HOUR = TimeUnit.HOURS.toMillis(1)
val MS_1DAY = TimeUnit.DAYS.toMillis(1)
val MS_1WEEK = TimeUnit.DAYS.toMillis(7)

val ABB_MONTHS = arrayOf(
    R.string.jan,
    R.string.fab,
    R.string.mar,
    R.string.apr,
    R.string.may,
    R.string.jun,
    R.string.jul,
    R.string.aug,
    R.string.sep,
    R.string.oct,
    R.string.nov,
    R.string.dec
)

val ABB_DAYS = arrayOf(
    R.string.abb_sunday,
    R.string.abb_monday,
    R.string.abb_tuesday,
    R.string.abb_wednesday,
    R.string.abb_thursday,
    R.string.abb_friday,
    R.string.abb_saturday
)

val DAYS_WITH_3CHARS = arrayOf(
    R.string.mon,
    R.string.tue,
    R.string.wed,
    R.string.thu,
    R.string.fri,
    R.string.sat,
    R.string.sun,
)

val DAYS_WITH_3CHARS_SUNDAY_FIRST = arrayOf(
    R.string.sun,
    R.string.mon,
    R.string.tue,
    R.string.wed,
    R.string.thu,
    R.string.fri,
    R.string.sat,
)

val DAY_STRING_MAP = mapOf(
    EVERYDAY to R.string.everyday,
    WEEKDAYS to R.string.weekdays,
    WEEKENDS to R.string.weekends
)

@Composable
fun LoopBase.formatStartEndTime(context: Context = LocalContext.current): String {
    val start = if (actualStartInDay >= 0) actualStartInDay else startInDay
    val end = if (actualEndInDay >= 0) actualEndInDay else endInDay
    if (start < 0 || end < 0) return ""

    return "${
        start.formatHourMinute(
            context = context,
            withAmPm = false
        )
    } ~ ${
        end.formatHourMinute(
            context = context,
            withAmPm = false
        )
    }"
}

@Composable
fun LocalDate.formatYearMonthDateDays(): String {
    val args = listOf(
        "$year",
        stringResource(id = ABB_MONTHS[monthValue - 1]),
        "$dayOfMonth",
        stringResource(id = DAYS_WITH_3CHARS[dayOfWeek.value - 1])
    )
    return stringResource(
        id = R.string.format_year_month_date_day,
        formatArgs = args.toTypedArray()
    )
}

@Composable
fun LocalDate.formatYearMonth(): String {
    val args = listOf(
        year,
        stringResource(id = ABB_MONTHS[monthValue - 1]),
    )
    return stringResource(
        id = R.string.format_year_montn,
        formatArgs = args.toTypedArray()
    )
}

@Composable
fun LocalDate.formatMonthDateDay(): String {
    val args = listOf(
        stringResource(id = ABB_MONTHS[monthValue - 1]),
        "$dayOfMonth",
        stringResource(id = DAYS_WITH_3CHARS[dayOfWeek.value - 1]),
    )
    return stringResource(
        id = R.string.format_month_date_day,
        formatArgs = args.toTypedArray()
    )
}

@Composable
fun Long.formatHourMinute(
    context: Context = LocalContext.current,
    withAmPm: Boolean = true
): String {
    return toLocalTime().formatHourMinute(
        context = context,
        withAmPm = withAmPm
    )
}

@Composable
fun LocalTime.formatHourMinute(
    context: Context = LocalContext.current,
    withAmPm: Boolean = true
): String {
    val resultHour = if (withAmPm) (hour % 12).run { if (this == 0) 12 else this } else hour

    return context.getString(
        if (withAmPm) {
            if (hour < 12) R.string.format_am_hour_minute else R.string.format_pm_hour_minute
        } else {
            R.string.format_hour_minute_24
        },
        resultHour,
        minute
    )
}

@Composable
fun rememberDayColor(day: Int): Color {
    val commonColor = AppColor.onSurface
    return remember(day) {
        when (day) {
            SUNDAY -> Red300
            SATURDAY -> BlueGreen
            else -> commonColor
        }
    }
}

@Composable
fun DayOfWeek.color() = when (this) {
    DayOfWeek.SUNDAY -> Red300
    DayOfWeek.SATURDAY -> BlueGreen
    else -> AppColor.onSurface
}

fun LocalDate.isSameMonth(other: LocalDate): Boolean {
    val me = this
    return me.year == other.year && me.month == other.month
}

fun dayForLoop(localDate: LocalDate = LocalDate.now()): @LoopDay Int =
    dayForLoop(localDate.dayOfWeek)

fun dayForLoop(dayOfWeek: DayOfWeek): @LoopDay Int = when (dayOfWeek) {
    DayOfWeek.SUNDAY -> SUNDAY
    DayOfWeek.MONDAY -> MONDAY
    DayOfWeek.TUESDAY -> TUESDAY
    DayOfWeek.WEDNESDAY -> WEDNESDAY
    DayOfWeek.THURSDAY -> THURSDAY
    DayOfWeek.FRIDAY -> FRIDAY
    DayOfWeek.SATURDAY -> SATURDAY
    else -> throw IllegalStateException("Unknown value for day of week")
}

fun Long.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()

fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

fun Long.toLocalTime(): LocalTime = LocalTime.ofNanoOfDay(
    TimeUnit.NANOSECONDS.convert(this, TimeUnit.MILLISECONDS)
)

fun LocalDateTime.toMs(zoneId: ZoneId = ZoneId.systemDefault()) =
    atZone(zoneId).toInstant().toEpochMilli()

fun LocalDate.toMs(zoneId: ZoneId = ZoneId.systemDefault()) =
    atStartOfDay(zoneId).toInstant().toEpochMilli()

fun LocalTime.toMs() = TimeUnit.NANOSECONDS.toMillis(toNanoOfDay())

fun LocalDate.toLocalTime(zoneId: ZoneId = ZoneId.systemDefault()) =
    atStartOfDay(zoneId).toInstant().toEpochMilli()

/**
 * 종료 시각이 시작 시각보다 앞서면(=하루 안에서 되돌아가면) 이 루프는 자정을 넘겨 다음 날로 이어진다.
 * 예) 23:00 ~ 02:00. (시간이 없는 AnyTime 루프는 해당 없음)
 */
val LoopBase.isOvernight: Boolean
    get() = !isAnyTime && endInDay < startInDay

/** 하루 기준 지속 시간(ms). 자정을 넘기면 하루를 더해 항상 양수가 되게 한다. 예) 23:00~02:00 → 3시간. */
val LoopBase.durationInDay: Long
    get() = if (isOvernight) endInDay - startInDay + MS_1DAY else endInDay - startInDay

/**
 * 하루 기준 시각 [nowMsInDay](0 ~ 24h ms)가 이 루프의 진행 구간 안에 있는지 판정한다.
 * - 일반 루프(start ≤ end): [start, end)
 * - 자정을 넘기는 루프(end < start): [start, 24h) ∪ [0, end)  ← 자정을 사이에 둔 두 조각
 */
fun LoopBase.isTimeInLoop(nowMsInDay: Long): Boolean =
    if (isOvernight) nowMsInDay >= startInDay || nowMsInDay < endInDay
    else nowMsInDay in startInDay until endInDay

/**
 * 두 루프가 원형(24시간) 시간축에서 겹치는지 판정한다. 자정을 넘겨 00:00 을 가로질러 감기는 구간까지 고려한다.
 *
 * 한쪽의 시작점이 다른 호의 길이 구간 안에 들어오면 겹친 것으로 본다(둘 중 하나라도 하루를 꽉 채우면 항상 겹침).
 * 끝 시각만 선형으로 비교하면 자정을 넘긴 감긴 부분을 놓치므로, 시작점 사이 거리를 원 둘레(mod 24h)로 재서 판정한다.
 */
fun LoopBase.overlapsInTime(other: LoopBase): Boolean {
    val day = MS_1DAY
    val startA = ((startInDay % day) + day) % day
    val startB = ((other.startInDay % day) + day) % day
    val lenA = durationInDay
    val lenB = other.durationInDay
    if (lenA >= day || lenB >= day) return true
    val bFromA = ((startB - startA) % day + day) % day
    val aFromB = ((startA - startB) % day + day) % day
    return bFromA < lenA || aFromB < lenB
}

/**
 * 지금([now]) 판정 대상이 되는 occurrence가 "시작한 날".
 * 자정을 넘겨 아직 이어지는 아침 구간([0, end))이라면 그 루프는 어제 시작한 것이므로 어제를 돌려준다.
 * 나머지는 오늘. 활성 요일(activeDays) 판정을 올바른 날짜에 대해 하기 위한 기준이다.
 */
fun LoopBase.occurrenceStartDate(now: LocalDateTime = LocalDateTime.now()): LocalDate {
    val nowMs = now.toLocalTime().toMs()
    return if (isOvernight && nowMs < endInDay) now.toLocalDate().minusDays(1) else now.toLocalDate()
}

fun LoopBase.isPast(localDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
    val nowMs = localDateTime.toLocalTime().toMs()
    // 자정을 넘기는 루프는 종료(end)와 다음 시작(start) 사이 구간에서만 "지난" 상태다.
    // 그 밖(진행 중이거나 다시 시작 예정)은 지난 것이 아니다.
    return if (isOvernight) nowMs in endInDay until startInDay
    else nowMs >= endInDay
}

fun LoopBase.isActive(localDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
    if (!enabled) return false
    if (isMock) return false
    if (isAnyTime) return doneState == LoopDoneVo.DoneState.IN_PROGRESS

    // 시각 창 안에 있고(자정 넘김 포함), 그 occurrence가 시작한 날이 활성 요일이어야 한다.
    if (!isTimeInLoop(localDateTime.toLocalTime().toMs())) return false
    return isActiveDay(occurrenceStartDate(localDateTime))
}

fun LoopBase.isActiveDay(localDate: LocalDate = LocalDate.now()): Boolean {
    return activeDays.isOn(dayForLoop(localDate))
}

fun LoopBase.isActiveTime(localDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
    if (isAnyTime) {
        return doneState == LoopDoneVo.DoneState.IN_PROGRESS
    }
    return isTimeInLoop(localDateTime.toLocalTime().toMs())
}

@Composable
fun intervalString(
    msTime: Long,
    highlight: String = ""
): String {
    return intervalString(LocalContext.current, msTime, highlight)
}

fun intervalString(
    context: Context,
    msTime: Long,
    highlight: String = "",
): String {
    val res = context.resources
    if (msTime <= 0) {
        return "$highlight${res.getString(R.string.no_repeat)}"
    }

    val time: Int
    val pluralResTimeUnit = when {
        msTime < MS_1MIN -> {
            time = (msTime / MS_1SEC).toInt()
            R.plurals.second
        }

        msTime < MS_1HOUR -> {
            time = (msTime / MS_1MIN).toInt()
            R.plurals.minute
        }

        msTime < MS_1DAY -> {
            time = (msTime / MS_1HOUR).toInt()
            R.plurals.hour
        }

        msTime < MS_1WEEK -> {
            time = (msTime / MS_1DAY).toInt()
            R.plurals.day
        }

        else -> {
            time = (msTime / MS_1WEEK).toInt()
            R.plurals.week
        }
    }

    val interval = res.getQuantityString(pluralResTimeUnit, time, time)
    val result = "$highlight$time $interval"

    return res.getString(R.string.every, result)
}


fun h2m2(msTime: Long): String {
    val timeInDay = msTime % MS_1DAY
    return String.format("%02d:%02d", timeInDay / MS_1HOUR, (timeInDay % MS_1HOUR) / MS_1MIN)
}

fun dh2m2(msTime: Long): String {
    val days = msTime / MS_1DAY

    return if (days == 0L) {
        h2m2(msTime)
    } else {
        String.format("%d days, %s", msTime / MS_1DAY, h2m2(msTime))
    }
}
