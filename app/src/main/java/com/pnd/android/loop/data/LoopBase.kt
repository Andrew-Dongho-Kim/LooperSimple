package com.pnd.android.loop.data

import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import androidx.annotation.IntRange
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
import com.pnd.android.loop.util.h2m2
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.toMs
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

const val NO_REPEAT = 0L
private const val DEFAULT_TITLE = ""
private const val DEFAULT_COLOR = 0xff000099.toInt()
private const val DEFAULT_ACTIVE_DAYS = EVERYDAY
private const val DEFAULT_INTERVAL = NO_REPEAT
private const val DEFAULT_ENABLED = true
private const val DEFAULT_IS_MOCK = false

private val defaultCreated
    get() = LocalDateTime.now().toMs()
private val defaultLoopStart
    get() = TimeUnit.NANOSECONDS.toMillis(LocalTime.now().toNanoOfDay())

private val defaultLoopEnd
    get() = TimeUnit.NANOSECONDS.toMillis(LocalTime.now().plusHours(1).toNanoOfDay())


interface LoopBase {
    val id: Int
    val title: String
    val color: Int
    val created: Long
    val loopStart: Long
    val loopEnd: Long
    val loopActiveDays: Int
    val interval: Long
    val enabled: Boolean
    val isMock: Boolean
    fun copyAs(
        id: Int = this.id,
        title: String = this.title,
        color: Int = this.color,
        created: Long = this.created,
        loopStart: Long = this.loopStart,
        loopEnd: Long = this.loopEnd,
        loopActiveDays: Int = this.loopActiveDays,
        interval: Long = this.interval,
        enabled: Boolean = this.enabled,
        isMock: Boolean = false,
    ): LoopBase

    companion object {
        fun default(isMock: Boolean = false): LoopBase = LoopImpl(isMock = isMock)

        val SUPPORTED_COLORS = listOf(
            DEFAULT_COLOR, 0xff0000cc.toInt(), 0xff3333ff.toInt(),
            0xff00ccff.toInt(), 0xff0099ff.toInt(), 0xff3366ff.toInt(),
            0xff00cc99.toInt(), 0xff00ff99.toInt(), 0xff00ff00.toInt(),
            0xff6600ff.toInt(), 0xff9933ff.toInt(), 0xff9900cc.toInt(),
            0xff99ff33.toInt(), 0xffccff66.toInt(), 0xffffff00.toInt(),
            0xffcccc00.toInt(), 0xffcc9900.toInt(), 0xffcc6600.toInt(),
            0xffff0000.toInt(), 0xffff3399.toInt(), 0xffcc6699.toInt(),
        )
    }
}

private class LoopImpl(
    override val id: Int = 0,
    override val title: String = DEFAULT_TITLE,
    override val color: Int = DEFAULT_COLOR,
    override val created: Long = defaultCreated,
    override val loopStart: Long = defaultLoopStart,
    override val loopEnd: Long = defaultLoopEnd,
    override val loopActiveDays: Int = DEFAULT_ACTIVE_DAYS,
    override val interval: Long = DEFAULT_INTERVAL,
    override val enabled: Boolean = DEFAULT_ENABLED,
    override val isMock: Boolean = DEFAULT_IS_MOCK,
) : LoopBase {

    override fun copyAs(
        id: Int,
        title: String,
        color: Int,
        created: Long,
        loopStart: Long,
        loopEnd: Long,
        loopActiveDays: Int,
        interval: Long,
        enabled: Boolean,
        isMock: Boolean,
    ): LoopBase = LoopImpl(
        id = id,
        title = title,
        color = color,
        created = created,
        loopStart = loopStart,
        loopEnd = loopEnd,
        loopActiveDays = loopActiveDays,
        interval = interval,
        enabled = enabled,
        isMock = isMock
    )
}

fun LoopBase.putTo(intent: Intent) {
    intent.putExtra(EXTRA_ID, id)
    intent.putExtra(EXTRA_COLOR, color)
    intent.putExtra(EXTRA_TITLE, title)
    intent.putExtra(EXTRA_LOOP_CREATED, created)
    intent.putExtra(EXTRA_LOOP_START, loopStart)
    intent.putExtra(EXTRA_LOOP_END, loopEnd)
    intent.putExtra(EXTRA_LOOP_ACTIVE_DAYS, loopActiveDays)
    intent.putExtra(EXTRA_LOOP_INTERVAL, interval)
    intent.putExtra(EXTRA_LOOP_ENABLED, enabled)
    intent.putExtra(EXTRA_LOOP_IS_MOCK, isMock)
}

fun Intent.asLoop(): LoopBase {
    return LoopImpl(
        id = getIntExtra(EXTRA_ID, 0),
        title = getStringExtra(EXTRA_TITLE) ?: DEFAULT_TITLE,
        color = getIntExtra(EXTRA_COLOR, DEFAULT_COLOR),
        created = getLongExtra(EXTRA_LOOP_CREATED, defaultCreated),
        loopStart = getLongExtra(EXTRA_LOOP_START, defaultLoopStart),
        loopEnd = getLongExtra(EXTRA_LOOP_END, defaultLoopEnd),
        loopActiveDays = getIntExtra(EXTRA_LOOP_ACTIVE_DAYS, DEFAULT_ACTIVE_DAYS),
        interval = getLongExtra(EXTRA_LOOP_INTERVAL, DEFAULT_INTERVAL),
        enabled = getBooleanExtra(EXTRA_LOOP_ENABLED, DEFAULT_ENABLED),
        isMock = getBooleanExtra(EXTRA_LOOP_IS_MOCK, DEFAULT_IS_MOCK),
    )
}

fun LoopBase.putTo(map: MutableMap<String, Any?>) {
    map[EXTRA_ID] = id
    map[EXTRA_TITLE] = title
    map[EXTRA_COLOR] = color
    map[EXTRA_LOOP_CREATED] = created
    map[EXTRA_LOOP_START] = loopStart
    map[EXTRA_LOOP_END] = loopEnd
    map[EXTRA_LOOP_ACTIVE_DAYS] = loopActiveDays
    map[EXTRA_LOOP_INTERVAL] = interval
    map[EXTRA_LOOP_ENABLED] = enabled
    map[EXTRA_LOOP_IS_MOCK] = isMock
}

fun Map<String, Any?>.asLoop(): LoopBase {
    return LoopImpl(
        id = getOrDefault(EXTRA_ID, 0) as Int,
        title = getOrDefault(EXTRA_TITLE, DEFAULT_TITLE) as String,
        color = (getOrDefault(EXTRA_COLOR, DEFAULT_COLOR) as Number).toInt(),
        created = (getOrDefault(EXTRA_LOOP_CREATED, defaultCreated) as Number).toLong(),
        loopStart = (getOrDefault(EXTRA_LOOP_START, defaultLoopStart) as Number).toLong(),
        loopEnd = (getOrDefault(EXTRA_LOOP_END, defaultLoopEnd) as Number).toLong(),
        loopActiveDays = (getOrDefault(EXTRA_LOOP_ACTIVE_DAYS, DEFAULT_ACTIVE_DAYS) as Number).toInt(),
        interval = (getOrDefault(EXTRA_LOOP_INTERVAL, DEFAULT_INTERVAL) as Number).toLong(),
        enabled = getOrDefault(EXTRA_LOOP_ENABLED, DEFAULT_ENABLED) as Boolean,
        isMock = getOrDefault(EXTRA_LOOP_IS_MOCK, DEFAULT_IS_MOCK) as Boolean,
    )
}

fun LoopBase.description(context: Context) =
    """ -->
    |*Loop  
    | title : $title
    | loopStart : ${h2m2(loopStart)}
    | loopEnd : ${h2m2(loopEnd)}
    | activeDays : ${Day.description(loopActiveDays)}
    | interval : ${intervalString(context, interval)}
    | enabled : $enabled""".trimMargin()


@Target(
    AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION
)
@IntDef(
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, WEEKDAYS, WEEKENDS, EVERYDAY
)
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
            WEEKDAYS -> "WEEKDAYS"
            WEEKENDS -> "WEEKENDS"
            EVERYDAY -> "EVERY DAY"
            else -> throw IllegalStateException("Unknown day : $day")
        }

        fun Int.isOn(@Day day: Int) = (this and day) == day
        fun Int.set(@Day day: Int) = this or day
        private fun Int.unset(@Day day: Int) = this and day.inv()
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


private const val EXTRA_ID = "extra_loop_id"
private const val EXTRA_COLOR = "extra_loop_color"
private const val EXTRA_TITLE = "extra_loop_title"
private const val EXTRA_LOOP_CREATED = "extra_loop_created"
private const val EXTRA_LOOP_START = "extra_loop_start"
private const val EXTRA_LOOP_END = "extra_loop_end"
private const val EXTRA_LOOP_ACTIVE_DAYS = "extra_loop_active_days"
private const val EXTRA_LOOP_INTERVAL = "extra_loop_interval"
private const val EXTRA_LOOP_ENABLED = "extra_loop_enabled"
private const val EXTRA_LOOP_IS_MOCK = "extra_loop_is_mock"