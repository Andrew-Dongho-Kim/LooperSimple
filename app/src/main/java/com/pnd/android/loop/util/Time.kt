package com.pnd.android.loop.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pnd.android.loop.R
import com.pnd.android.loop.data.Day
import com.pnd.android.loop.data.Day.Companion.EVERYDAY
import com.pnd.android.loop.data.Day.Companion.FRIDAY
import com.pnd.android.loop.data.Day.Companion.MONDAY
import com.pnd.android.loop.data.Day.Companion.SATURDAY
import com.pnd.android.loop.data.Day.Companion.SUNDAY
import com.pnd.android.loop.data.Day.Companion.THURSDAY
import com.pnd.android.loop.data.Day.Companion.TUESDAY
import com.pnd.android.loop.data.Day.Companion.WEDNESDAY
import com.pnd.android.loop.data.Day.Companion.WEEKDAYS
import com.pnd.android.loop.data.Day.Companion.WEEKENDS
import com.pnd.android.loop.data.Day.Companion.isOn
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.Blue500
import com.pnd.android.loop.ui.theme.BlueGreen
import com.pnd.android.loop.ui.theme.Red300
import com.pnd.android.loop.ui.theme.Red500
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
fun LoopBase.formatStartEndTime(context: Context = LocalContext.current) =
    "${
        loopStart.formatHourMinute(
            context = context,
            withAmPm = false
        )
    } ~ ${
        loopEnd.formatHourMinute(
            context = context,
            withAmPm = false
        )
    }"

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

fun dayForLoop(localDate: LocalDate = LocalDate.now()): @Day Int = dayForLoop(localDate.dayOfWeek)

fun dayForLoop(dayOfWeek: DayOfWeek): @Day Int = when (dayOfWeek) {
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


fun LoopBase.isPast(localDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
    val localTime = localDateTime.toLocalTime()
    val timeInMs = TimeUnit.MILLISECONDS.convert(localTime.toNanoOfDay(), TimeUnit.NANOSECONDS)

    val end = if (loopStart > loopEnd) loopEnd + MS_1DAY else loopEnd
    return timeInMs >= end
}

fun LoopBase.isActive(localDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
    return enabled &&
            isActiveDay(localDate = localDateTime.toLocalDate()) &&
            isActiveTime(localDateTime = localDateTime)
}

fun LoopBase.isActiveDay(localDate: LocalDate = LocalDate.now()): Boolean {
    return loopActiveDays.isOn(dayForLoop(localDate))
}

fun LoopBase.isActiveTime(localDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
    val localTime = localDateTime.toLocalTime()
    val timeInMs = TimeUnit.MILLISECONDS.convert(localTime.toNanoOfDay(), TimeUnit.NANOSECONDS)

    val start = loopStart
    val end = if (loopStart > loopEnd) loopEnd + MS_1DAY else loopEnd
    return timeInMs in start..end
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
