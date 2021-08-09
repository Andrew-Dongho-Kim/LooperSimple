package com.pnd.android.loop.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.pnd.android.loop.R
import com.pnd.android.loop.data.Loop
import com.pnd.android.loop.data.Loop.Day.Companion.EVERYDAY
import com.pnd.android.loop.data.Loop.Day.Companion.FRIDAY
import com.pnd.android.loop.data.Loop.Day.Companion.MONDAY
import com.pnd.android.loop.data.Loop.Day.Companion.SATURDAY
import com.pnd.android.loop.data.Loop.Day.Companion.SUNDAY
import com.pnd.android.loop.data.Loop.Day.Companion.THURSDAY
import com.pnd.android.loop.data.Loop.Day.Companion.TUESDAY
import com.pnd.android.loop.data.Loop.Day.Companion.WEDNESDAY
import com.pnd.android.loop.data.Loop.Day.Companion.WEEKDAYS
import com.pnd.android.loop.data.Loop.Day.Companion.WEEKENDS
import com.pnd.android.loop.data.Loop.Day.Companion.isOn
import java.util.*
import java.util.concurrent.TimeUnit


val MS_1SEC = TimeUnit.SECONDS.toMillis(1)
val MS_1MIN = TimeUnit.MINUTES.toMillis(1)
val MS_1HOUR = TimeUnit.HOURS.toMillis(1)
val MS_1DAY = TimeUnit.DAYS.toMillis(1)
val MS_1WEEK = TimeUnit.DAYS.toMillis(7)
val MS_1MONTH = TimeUnit.DAYS.toMillis(7 * 4)

val MONTHS = arrayOf(
    R.string.january,
    R.string.february,
    R.string.march,
    R.string.april,
    R.string.may,
    R.string.june,
    R.string.july,
    R.string.august,
    R.string.september,
    R.string.october,
    R.string.november,
    R.string.december
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

val ABB2_DAYS = arrayOf(
    R.string.abb2_sunday,
    R.string.abb2_monday,
    R.string.abb2_tuesday,
    R.string.abb2_wednesday,
    R.string.abb2_thursday,
    R.string.abb2_friday,
    R.string.abb2_saturday
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

fun day(msTime: Long): @Loop.Day Int {
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

fun localTime(): Long {
    val cal = Calendar.getInstance()
    val msTime = cal.timeInMillis
    return msTime + TimeZone.getDefault().getOffset(msTime)
}

fun Loop.isAllowedDay(currTime: Long = localTime()): Boolean {
    return loopEnableDays.isOn(day(currTime))
}

fun Loop.isAllowedTime(timeInDay: Long = localTime() % MS_1DAY): Boolean {
    val start = loopStart
    val end = if (loopStart > loopEnd) loopEnd + MS_1DAY else loopEnd
    return timeInDay in start..end
}

@Composable
fun intervalString(msTime: Long, highlight: String = "", isAbb: Boolean = false): String {
    return intervalString(LocalContext.current, msTime, highlight, isAbb)
}

fun intervalString(
    context: Context,
    msTime: Long,
    highlight: String = "",
    isAbb: Boolean = false
): String {
    val time: Int
    val pluralResTimeUnit = when {
        msTime < MS_1MIN -> {
            time = (msTime / MS_1SEC).toInt()
            if (isAbb) R.plurals.sec else R.plurals.second
        }
        msTime < MS_1HOUR -> {
            time = (msTime / MS_1MIN).toInt()
            if (isAbb) R.plurals.min else R.plurals.minute
        }
        msTime < MS_1DAY -> {
            time = (msTime / MS_1HOUR).toInt()
            R.plurals.hour
        }
        msTime < MS_1WEEK -> {
            time = (msTime / MS_1DAY).toInt()
            if (isAbb) R.plurals.d else R.plurals.day
        }
        msTime < MS_1MONTH -> {
            time = (msTime / MS_1WEEK).toInt()
            if (isAbb) R.plurals.w else R.plurals.week
        }
        else -> {
            time = (msTime / MS_1MONTH).toInt()
            if (isAbb) R.plurals.M else R.plurals.month
        }
    }

    val res = context.resources
    val interval = res.getQuantityString(pluralResTimeUnit, time, time)
    val result = "$highlight$time $interval"

    return if (isAbb) result else res.getString(R.string.every, result)
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

fun d3m3d2(context: Context, msTime: Long): String {
    val day = context.getString(ABB2_DAYS[0])
    val month = context.getString(MONTHS[0])
    val date = 1

    return context.getString(R.string.desc_day_month_date, day, month, date)
}
