package com.pnd.android.loop.data

import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.lifecycle.LiveData
import androidx.room.*
import com.pnd.android.loop.alarm.NO_ALARMS
import com.pnd.android.loop.data.LoopVo.Day.Companion.FRIDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.MONDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.SATURDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.SUNDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.THURSDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.TUESDAY
import com.pnd.android.loop.data.LoopVo.Day.Companion.WEDNESDAY
import com.pnd.android.loop.util.h2m2
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.localTime
import java.util.*
import java.util.concurrent.TimeUnit


@Entity(tableName = "loop")
data class LoopVo(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var icon: Int = 0,
    var title: String = "",
    var tickStart: Long = 0L,
    var loopStart: Long = TimeUnit.HOURS.toMillis(8),
    var loopEnd: Long = TimeUnit.HOURS.toMillis(22),
    var loopEnableDays: Int = Day.EVERYDAY,
    var interval: Long = TimeUnit.HOURS.toMillis(1),
    var alarms: Int = NO_ALARMS,
    var enabled: Boolean = true
) {
    fun putToIntent(intent: Intent) {
        intent.putExtra(EXTRA_ID, id)
        intent.putExtra(EXTRA_ICON, icon)
        intent.putExtra(EXTRA_TITLE, title)
        intent.putExtra(EXTRA_TICK_START, tickStart)
        intent.putExtra(EXTRA_LOOP_START, loopStart)
        intent.putExtra(EXTRA_LOOP_END, loopEnd)
        intent.putExtra(EXTRA_LOOP_ENABLE_DAYS, loopEnableDays)
        intent.putExtra(EXTRA_ALARM_INTERVAL, interval)
        intent.putExtra(EXTRA_LOOP_ALARMS, alarms)
        intent.putExtra(EXTRA_ALARM_ENABLED, enabled)
    }


    @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
    @IntDef(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
    annotation class Day {
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

            fun toString(@Day day: Int) = when (day) {
                SUNDAY -> "SUN"
                MONDAY -> "MON"
                TUESDAY -> "TUE"
                WEDNESDAY -> "WEN"
                THURSDAY -> "THU"
                FRIDAY -> "FRI"
                SATURDAY -> "SAT"
                EVERYDAY -> "EVERY DAY"
                else -> throw IllegalStateException("Unknown day : $day")
            }

            fun Int.isOn(@Day day: Int) = (this and day) == day
            fun Int.set(@Day day: Int) = this or day
            fun Int.unset(@Day day: Int) = this and day.inv()
            fun Int.toggle(@Day day: Int) = if (isOn(day)) unset(day) else set(day)
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

    companion object {
        fun popFromIntent(intent: Intent): LoopVo {
            return LoopVo(
                id = intent.getIntExtra(EXTRA_ID, 0),
                icon = intent.getIntExtra(EXTRA_ICON, 0),
                title = intent.getStringExtra(EXTRA_TITLE) ?: "",
                tickStart = intent.getLongExtra(EXTRA_TICK_START, localTime()),
                loopStart = intent.getLongExtra(EXTRA_LOOP_START, 8),
                loopEnd = intent.getLongExtra(EXTRA_LOOP_END, 22),
                loopEnableDays = intent.getIntExtra(EXTRA_LOOP_ENABLE_DAYS, Day.EVERYDAY),
                interval = intent.getLongExtra(EXTRA_ALARM_INTERVAL, TimeUnit.HOURS.toMillis(1)),
                alarms = intent.getIntExtra(EXTRA_LOOP_ALARMS, NO_ALARMS),
                enabled = intent.getBooleanExtra(EXTRA_ALARM_ENABLED, true)
            )
        }

        private const val EXTRA_ID = "extra_loop_id"
        private const val EXTRA_ICON = "extra_loop_icon"
        private const val EXTRA_TITLE = "extra_loop_title"
        private const val EXTRA_TICK_START = "extra_tick_start"
        private const val EXTRA_LOOP_START = "extra_loop_allowed_start"
        private const val EXTRA_LOOP_END = "extra_loop_allowed_end"
        private const val EXTRA_LOOP_ENABLE_DAYS = "extra_loop_enable_days"
        private const val EXTRA_ALARM_INTERVAL = "extra_loop_alarm_interval"
        private const val EXTRA_LOOP_ALARMS = "extra_loop_alarms"
        private const val EXTRA_ALARM_ENABLED = "extra_loop_alarm_enabled"
    }

}


fun LoopVo.description(context: Context) =
    """ -->
    |*Loop  
    | title : $title
    | tickStart : ${Date(tickStart)}
    | loopStart : ${h2m2(loopStart)}
    | loopEnd : ${h2m2(loopEnd)}
    | enabledDays : ${LoopVo.Day.description(loopEnableDays)}
    | interval : ${intervalString(context, interval)}
    | alarms : $alarms
    | enabled : $enabled""".trimMargin()

@Dao
interface LoopDao {
    @Query("SELECT * FROM loop")
    fun getAll(): LiveData<List<LoopVo>>

    @Query("SELECT * FROM loop")
    suspend fun syncGetAll(): List<LoopVo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(vararg loops: LoopVo): List<Long>

    @Query("DELETE FROM loop WHERE id = :id")
    suspend fun remove(id: Int)
}