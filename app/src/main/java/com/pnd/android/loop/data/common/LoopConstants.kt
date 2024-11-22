package com.pnd.android.loop.data.common


import com.pnd.android.loop.data.LoopDay
import java.time.LocalTime
import java.util.concurrent.TimeUnit

const val NO_REPEAT = 0L
const val DEFAULT_CREATED = 0L
const val DEFAULT_TITLE = ""
const val DEFAULT_COLOR = 0xff000099.toInt()
const val DEFAULT_ACTIVE_DAYS = LoopDay.EVERYDAY
const val DEFAULT_INTERVAL = NO_REPEAT
const val DEFAULT_ENABLED = true
const val DEFAULT_IS_MOCK = false

val defaultStartInDay
    get() =
        TimeUnit.NANOSECONDS.toMillis(
            LocalTime
                .now()
                .withMinute(0)
                .withSecond(0)
                .toNanoOfDay()
        )

val defaultEndInDay
    get() = TimeUnit.NANOSECONDS.toMillis(
        LocalTime
            .now()
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .plusHours(1)
            .toNanoOfDay()
    )


val SUPPORTED_COLORS = listOf(
    DEFAULT_COLOR, 0xff0000cc.toInt(), 0xff3333ff.toInt(),
    0xff00ccff.toInt(), 0xff0099ff.toInt(), 0xff3366ff.toInt(),
    0xff00cc99.toInt(), 0xff00ff99.toInt(), 0xff00ff00.toInt(),
    0xff6600ff.toInt(), 0xff9933ff.toInt(), 0xff9900cc.toInt(),
    0xff99ff33.toInt(), 0xffccff66.toInt(), 0xffffff00.toInt(),
    0xffcccc00.toInt(), 0xffcc9900.toInt(), 0xffcc6600.toInt(),
    0xffff0000.toInt(), 0xffff3399.toInt(), 0xffcc6699.toInt(),
)


const val MAX_LOOPS_TOGETHER = 3