package com.pnd.android.loop.data

import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
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
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit

@Immutable
@Entity(tableName = "loop")
data class LoopVo(
    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
    override val color: Int = DEFAULT_COLOR,
    override val title: String = "",
    val tickStart: Long = 0L,
    override val loopStart: Long = 0L,
    override val loopEnd: Long = 0L,
    override val loopActiveDays: Int = Day.EVERYDAY,
    val interval: Long = NO_REPEAT,
    val alarms: Int = NO_ALARMS,
    override val enabled: Boolean = true
) : LoopBase {
    fun putTo(intent: Intent) {
        intent.putExtra(EXTRA_ID, id)
        intent.putExtra(EXTRA_COLOR, color)
        intent.putExtra(EXTRA_TITLE, title)
        intent.putExtra(EXTRA_LOOP_START, loopStart)
        intent.putExtra(EXTRA_LOOP_END, loopEnd)
        intent.putExtra(EXTRA_LOOP_ACTIVE_DAYS, loopActiveDays)
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

        const val NO_REPEAT = 0L
        const val DEFAULT_COLOR = 0xff000099.toInt()

        val SUPPORTED_COLORS = listOf(
            DEFAULT_COLOR, 0xff0000cc.toInt(), 0xff3333ff.toInt(),
            0xff00ccff.toInt(), 0xff0099ff.toInt(), 0xff3366ff.toInt(),
            0xff00cc99.toInt(), 0xff00ff99.toInt(), 0xff00ff00.toInt(),
            0xff6600ff.toInt(), 0xff9933ff.toInt(), 0xff9900cc.toInt(),
            0xff99ff33.toInt(), 0xffccff66.toInt(), 0xffffff00.toInt(),
            0xffcccc00.toInt(), 0xffcc9900.toInt(), 0xffcc6600.toInt(),
            0xffff0000.toInt(), 0xffff3399.toInt(), 0xffcc6699.toInt(),
        )

        fun default() = LoopVo(
            loopStart = TimeUnit.NANOSECONDS.toMillis(LocalTime.now().toNanoOfDay()),
            loopEnd = TimeUnit.NANOSECONDS.toMillis(LocalTime.now().plusHours(1).toNanoOfDay()),
        )

        fun Intent.asLoop(): LoopVo {
            return LoopVo(
                id = getIntExtra(EXTRA_ID, 0),
                color = getIntExtra(EXTRA_COLOR, 0),
                title = getStringExtra(EXTRA_TITLE) ?: "",
                loopStart = getLongExtra(EXTRA_LOOP_START, 8),
                loopEnd = getLongExtra(EXTRA_LOOP_END, 22),
                loopActiveDays = getIntExtra(EXTRA_LOOP_ACTIVE_DAYS, Day.EVERYDAY),
                interval = getLongExtra(EXTRA_ALARM_INTERVAL, TimeUnit.HOURS.toMillis(1)),
                alarms = getIntExtra(EXTRA_LOOP_ALARMS, NO_ALARMS),
                enabled = getBooleanExtra(EXTRA_ALARM_ENABLED, true)
            )
        }

        private const val EXTRA_ID = "extra_loop_id"
        private const val EXTRA_COLOR = "extra_loop_color"
        private const val EXTRA_TITLE = "extra_loop_title"
        private const val EXTRA_LOOP_START = "extra_loop_allowed_start"
        private const val EXTRA_LOOP_END = "extra_loop_allowed_end"
        private const val EXTRA_LOOP_ACTIVE_DAYS = "extra_loop_active_days"
        private const val EXTRA_ALARM_INTERVAL = "extra_loop_alarm_interval"
        private const val EXTRA_LOOP_ALARMS = "extra_loop_alarms"
        private const val EXTRA_ALARM_ENABLED = "extra_loop_alarm_enabled"
    }

}

fun LoopVo.description(context: Context) =
    """ -->
    |*Loop  
    | title : $title
    | loopStart : ${h2m2(loopStart)}
    | loopEnd : ${h2m2(loopEnd)}
    | activeDays : ${LoopVo.Day.description(loopActiveDays)}
    | interval : ${intervalString(context, interval)}
    | alarms : $alarms
    | enabled : $enabled""".trimMargin()

@Dao
interface LoopDao {
    @Query("SELECT * FROM loop ORDER BY loopStart ASC, loopEnd ASC")
    fun allLoopsLiveData(): LiveData<List<LoopVo>>

    @Query("SELECT * FROM loop")
    suspend fun allLoops(): List<LoopVo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(vararg loops: LoopVo): List<Long>

    @Query("DELETE FROM loop WHERE id = :id")
    suspend fun remove(id: Int)
}