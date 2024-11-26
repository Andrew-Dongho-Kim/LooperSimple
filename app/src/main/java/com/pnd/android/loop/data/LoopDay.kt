package com.pnd.android.loop.data

import androidx.annotation.IntDef
import androidx.annotation.IntRange
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

@Target(
    AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION
)
@IntDef(
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, WEEKDAYS, WEEKENDS, EVERYDAY
)
annotation class LoopDay {
    companion object {
        const val SUNDAY = 1 shl 0
        const val MONDAY = 1 shl 1
        const val TUESDAY = 1 shl 2
        const val WEDNESDAY = 1 shl 3
        const val THURSDAY = 1 shl 4
        const val FRIDAY = 1 shl 5
        const val SATURDAY = 1 shl 6

        const val WEEKDAYS = (MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY)
        const val WEEKENDS = (SUNDAY or SATURDAY)
        const val EVERYDAY = (WEEKDAYS or WEEKENDS)
        val ALL = listOf(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)


        @IntRange(from = 0, to = 6)
        fun fromIndex(index: Int) = when (index) {
            0 -> SUNDAY
            1 -> MONDAY
            2 -> TUESDAY
            3 -> WEDNESDAY
            4 -> THURSDAY
            5 -> FRIDAY
            6 -> SATURDAY
            else -> throw IllegalArgumentException("unknown index:$index")
        }

        fun toString(@LoopDay day: Int) = when (day) {
            SUNDAY -> "SUN"
            MONDAY -> "MON"
            TUESDAY -> "TUE"
            WEDNESDAY -> "WEN"
            THURSDAY -> "THU"
            FRIDAY -> "FRI"
            SATURDAY -> "SAT"
            WEEKDAYS -> "WEEKDAYS"
            WEEKENDS -> "WEEKENDS"
            EVERYDAY -> "EVERY DAY"
            else -> throw IllegalStateException("Unknown day : $day")
        }

        fun Int.all() = ALL.filter{ day -> isOn(day) }
        fun Int.isOn(@LoopDay day: Int) = (this and day) == day

        fun Int.set(@LoopDay day: Int) = this or day
        fun Int.unset(@LoopDay day: Int) = this and day.inv()
        fun Int.toggle(@LoopDay day: Int) = if (isOn(day)) unset(day) else set(day)

        fun description(days: Int): String {
            val sb = StringBuilder()
            var day = SUNDAY
            while (day <= SATURDAY) {
                if (days.isOn(day)) {
                    sb.append(toString(day)).append(' ')
                }
                day = day shl 1
            }
            return sb.toString()
        }
    }
}