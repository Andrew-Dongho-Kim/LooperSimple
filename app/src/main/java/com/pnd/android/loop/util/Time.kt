package com.pnd.android.loop.util

import android.content.Context
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopVo.Day.Companion.EVERYDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.FRIDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.MONDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.SATURDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.SUNDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.THURSDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.TUESDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.WEDNESDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.WEEKDAYS
import com.pnd.android.loop.data.LoopVo.Day.Companion.WEEKENDS
import com.pnd.android.loop.data.LoopVo.Day.Companion.isOn
import com.pnd.android.loop.ui.theme.Blue500
import com.pnd.android.loop.ui.theme.Red500
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit


val MS_1SEC = TimeUnit.SECONDS.toMillis(1)
val MS_1MIN = TimeUnit.MINUTES.toMillis(1)
val MS_1HOUR = TimeUnit.HOURS.toMillis(1)
val MS_1DAY = TimeUnit.DAYS.toMillis(1)
val MS_1WEEK = TimeUnit.DAYS.toMillis(7)
val MS_1MONTH = TimeUnit.DAYS.toMillis(7 * 4)

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

val DAY_STRING_MAP = mapOf(
    EVERYDAY to R.string.everyday,
    WEEKDAYS to R.string.weekdays,
    WEEKENDS to R.string.weekends
)

val AmPm = listOf(
    R.string.am,
    R.string.pm
)


@Composable
fun LocalDate.toYearMonthDateDaysString(): String {
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

fun Long.toLocalTime(): LocalTime {
    return LocalTime.ofSecondOfDay(this / 1000)
}

@Composable
fun Long.toHourMinute(withAmPm: Boolean = true): String {
    return toLocalTime().toHourMinute(withAmPm)
}

@Composable
fun LocalTime.toHourMinute(withAmPm: Boolean = true): String {
    val resultHour = if (withAmPm) (hour % 12).run { if (this == 0) 12 else this } else hour

    return stringResource(
        id = if (withAmPm) {
            if (hour < 12) R.string.format_am_hour_minute else R.string.format_pm_hour_minute
        } else {
            R.string.format_hour_minute_24
        },
        formatArgs = arrayOf(
            resultHour,
            minute
        )
    )
}

@Composable
fun rememberDayColor(day: Int): Color {
    val commonColor = MaterialTheme.colors.onSurface
    return remember(day) {
        when (day) {
            SUNDAY -> Red500
            SATURDAY -> Blue500
            else -> commonColor
        }
    }
}

fun day(msTime: Long): @LoopVo.Day Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = msTime - TimeZone.getDefault().getOffset(msTime)
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> SUNDAY
        Calendar.MONDAY -> MONDAY
        Calendar.TUESDAY -> TUESDAY
        Calendar.WEDNESDAY -> WEDNESDAY
        Calendar.THURSDAY -> THURSDAY
        Calendar.FRIDAY -> FRIDAY
        Calendar.SATURDAY -> SATURDAY
        else -> throw IllegalStateException("Unknown value for day of week")
    }
}

fun isAm(msTime: Long) = ((msTime % MS_1DAY) / MS_1HOUR).toInt() < 12

fun hourIn24(msTime: Long): Int {
    return (if (msTime == MS_1DAY) 24 else (msTime % MS_1DAY) / MS_1HOUR).toInt()
}

fun hourIn12(msTime: Long): Int {
    val h = ((msTime % MS_1DAY) / MS_1HOUR).toInt()
    return when {
        h > 12 -> h - 12
        h == 0 -> 12
        else -> h
    }
}

fun min(msTime: Long) = ((msTime % MS_1HOUR) / MS_1MIN).toInt()

fun localTime(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return LocalDateTime.now().atZone(zoneId).toInstant().toEpochMilli()
}

fun localTimeInDay() = with(LocalTime.now()) {
    hour * MS_1HOUR + minute * MS_1MIN + second
}

fun LoopVo.isActive(currTime: Long = localTime()): Boolean {
    return isActiveDay(currTime = currTime) && isActiveTime(timeInDay = currTime % MS_1DAY)
}

fun LoopVo.isActiveDay(currTime: Long = localTime()): Boolean {
    return loopActiveDays.isOn(day(currTime))
}

fun LoopVo.isActiveTime(timeInDay: Long = localTime() % MS_1DAY): Boolean {
    val start = loopStart
    val end = if (loopStart > loopEnd) loopEnd + MS_1DAY else loopEnd
    return timeInDay in start..end
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

        msTime < MS_1MONTH -> {
            time = (msTime / MS_1WEEK).toInt()
            R.plurals.week
        }

        else -> {
            time = (msTime / MS_1MONTH).toInt()
            R.plurals.month
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


