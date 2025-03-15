package com.pnd.android.loop.data

import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.pnd.android.loop.data.common.DEFAULT_ACTIVE_DAYS
import com.pnd.android.loop.data.common.DEFAULT_COLOR
import com.pnd.android.loop.data.common.DEFAULT_CREATED
import com.pnd.android.loop.data.common.DEFAULT_ENABLED
import com.pnd.android.loop.data.common.DEFAULT_INTERVAL
import com.pnd.android.loop.data.common.DEFAULT_IS_ANY_TIME
import com.pnd.android.loop.data.common.DEFAULT_IS_MOCK
import com.pnd.android.loop.data.common.DEFAULT_TITLE
import com.pnd.android.loop.data.common.defaultEndInDay
import com.pnd.android.loop.data.common.defaultStartInDay
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

// JvmOverloads is necessary because of ignore field for room
@Immutable
@Entity(tableName = "loop")
data class LoopVo @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    override val loopId: Int,
    override val title: String,
    override val color: Int,
    override val created: Long,
    override val startInDay: Long,
    override val endInDay: Long,
    override val activeDays: Int,
    override val interval: Long,
    override val enabled: Boolean,
    @ColumnInfo(defaultValue = "false")
    override val isAnyTime: Boolean,
    @Ignore override val isMock: Boolean = false,
) : LoopBase {

    override fun copyAs(
        loopId: Int,
        title: String,
        color: Int,
        created: Long,
        startInDay: Long,
        endInDay: Long,
        activeDays: Int,
        interval: Long,
        enabled: Boolean,
        isAnyTime: Boolean,
        isMock: Boolean,
    ): LoopBase = LoopVo(
        loopId = loopId,
        title = title,
        color = color,
        created = created,
        startInDay = startInDay,
        endInDay = endInDay,
        activeDays = activeDays,
        interval = interval,
        enabled = enabled,
        isAnyTime = isAnyTime,
        isMock = isMock,
    )

    companion object Factory {
        const val ANY_TIME = -1L

        fun create(
            id: Int = 0,
            title: String = DEFAULT_TITLE,
            color: Int = DEFAULT_COLOR,
            created: Long = DEFAULT_CREATED,
            startInDay: Long = defaultStartInDay,
            endInDay: Long = defaultEndInDay,
            activeDays: Int = DEFAULT_ACTIVE_DAYS,
            interval: Long = DEFAULT_INTERVAL,
            enabled: Boolean = DEFAULT_ENABLED,
            isAnyTime: Boolean = DEFAULT_IS_ANY_TIME,
            isMock: Boolean = DEFAULT_IS_MOCK,
        ) = LoopVo(
            loopId = id,
            title = title,
            color = color,
            created = created,
            startInDay = startInDay,
            endInDay = endInDay,
            activeDays = activeDays,
            interval = interval,
            enabled = enabled,
            isAnyTime = isAnyTime,
            isMock = isMock
        )

        fun anytime(
            id: Int = 0,
            title: String = DEFAULT_TITLE,
            color: Int = DEFAULT_COLOR,
            created: Long = DEFAULT_CREATED,
            activeDays: Int = DEFAULT_ACTIVE_DAYS,
            interval: Long = DEFAULT_INTERVAL,
            enabled: Boolean = DEFAULT_ENABLED,
            isMock: Boolean = DEFAULT_IS_MOCK,
        ) = create(
            id = id,
            title = title,
            color = color,
            created = created,
            startInDay = ANY_TIME,
            endInDay = ANY_TIME,
            activeDays = activeDays,
            interval = interval,
            enabled = enabled,
            isMock = isMock,
        )

        const val MIDNIGHT_RESERVATION_ID = -10
        fun midnight(): LoopBase = create(
            id = MIDNIGHT_RESERVATION_ID,
            title = "Midnight Sync",
            startInDay = LocalTime.MAX.toMs(),
            endInDay = LocalTime.MAX.toMs(),
            isMock = true
        )
    }
}

fun LoopBase.asLoopVo(
    id: Int = this.loopId,
    title: String = this.title,
    color: Int = this.color,
    created: Long = this.created,
    startInDay: Long = this.startInDay,
    endInDay: Long = this.endInDay,
    loopActiveDays: Int = this.activeDays,
    interval: Long = this.interval,
    enabled: Boolean = this.enabled,
    isAnyTime: Boolean = this.isAnyTime,
    isMock: Boolean = this.isMock,
) = LoopVo.create(
    id = id,
    title = title,
    color = color,
    created = created,
    startInDay = startInDay,
    endInDay = endInDay,
    activeDays = loopActiveDays,
    interval = interval,
    enabled = enabled,
    isAnyTime = isAnyTime,
    isMock = isMock
)

fun LoopBase.putTo(intent: Intent) {
    intent.putExtra(EXTRA_ID, loopId)
    intent.putExtra(EXTRA_COLOR, color)
    intent.putExtra(EXTRA_TITLE, title)
    intent.putExtra(EXTRA_LOOP_CREATED, created)
    intent.putExtra(EXTRA_LOOP_START, startInDay)
    intent.putExtra(EXTRA_LOOP_END, endInDay)
    intent.putExtra(EXTRA_LOOP_ACTIVE_DAYS, activeDays)
    intent.putExtra(EXTRA_LOOP_INTERVAL, interval)
    intent.putExtra(EXTRA_LOOP_ENABLED, enabled)
    intent.putExtra(EXTRA_LOOP_IS_MOCK, isMock)
}

fun LoopBase.putTo(map: MutableMap<String, Any?>) {
    map[EXTRA_ID] = loopId
    map[EXTRA_TITLE] = title
    map[EXTRA_COLOR] = color
    map[EXTRA_LOOP_CREATED] = created
    map[EXTRA_LOOP_START] = startInDay
    map[EXTRA_LOOP_END] = endInDay
    map[EXTRA_LOOP_ACTIVE_DAYS] = activeDays
    map[EXTRA_LOOP_INTERVAL] = interval
    map[EXTRA_LOOP_ENABLED] = enabled
    map[EXTRA_LOOP_IS_MOCK] = isMock
}

fun Intent.asLoop(): LoopBase {
    return LoopVo(
        loopId = getIntExtra(EXTRA_ID, 0),
        title = getStringExtra(EXTRA_TITLE) ?: DEFAULT_TITLE,
        color = getIntExtra(EXTRA_COLOR, DEFAULT_COLOR),
        created = getLongExtra(EXTRA_LOOP_CREATED, DEFAULT_CREATED),
        startInDay = getLongExtra(EXTRA_LOOP_START, defaultStartInDay),
        endInDay = getLongExtra(EXTRA_LOOP_END, defaultEndInDay),
        activeDays = getIntExtra(EXTRA_LOOP_ACTIVE_DAYS, DEFAULT_ACTIVE_DAYS),
        interval = getLongExtra(EXTRA_LOOP_INTERVAL, DEFAULT_INTERVAL),
        enabled = getBooleanExtra(EXTRA_LOOP_ENABLED, DEFAULT_ENABLED),
        isAnyTime = getBooleanExtra(EXTRA_LOOP_IS_ANY_TIME, DEFAULT_IS_ANY_TIME),
        isMock = getBooleanExtra(EXTRA_LOOP_IS_MOCK, DEFAULT_IS_MOCK),
    )
}

fun Map<String, Any?>.asLoop(): LoopBase {
    return LoopVo(
        loopId = getOrDefault(EXTRA_ID, 0) as Int,
        title = getOrDefault(EXTRA_TITLE, DEFAULT_TITLE) as String,
        color = getOrDefault(EXTRA_COLOR, DEFAULT_COLOR) as Int,
        created = getOrDefault(EXTRA_LOOP_CREATED, DEFAULT_CREATED) as Long,
        startInDay = (getOrDefault(EXTRA_LOOP_START, defaultStartInDay) as Number).toLong(),
        endInDay = (getOrDefault(EXTRA_LOOP_END, defaultEndInDay) as Number).toLong(),
        activeDays = getOrDefault(
            EXTRA_LOOP_ACTIVE_DAYS,
            DEFAULT_ACTIVE_DAYS
        ) as Int,
        interval = (getOrDefault(EXTRA_LOOP_INTERVAL, DEFAULT_INTERVAL) as Number).toLong(),
        enabled = getOrDefault(EXTRA_LOOP_ENABLED, DEFAULT_ENABLED) as Boolean,
        isAnyTime = getOrDefault(EXTRA_LOOP_IS_ANY_TIME, DEFAULT_IS_ANY_TIME) as Boolean,
        isMock = getOrDefault(EXTRA_LOOP_IS_MOCK, DEFAULT_IS_MOCK) as Boolean,
    )
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
private const val EXTRA_LOOP_IS_ANY_TIME = "extra_loop_is_any_time"
private const val EXTRA_LOOP_IS_MOCK = "extra_loop_is_mock"

