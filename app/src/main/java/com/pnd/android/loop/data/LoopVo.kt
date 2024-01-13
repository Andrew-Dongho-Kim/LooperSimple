package com.pnd.android.loop.data

import androidx.compose.runtime.Immutable
import androidx.lifecycle.LiveData
import androidx.room.*
import com.pnd.android.loop.alarm.NO_ALARMS
import java.util.*

@Immutable
@Entity(tableName = "loop")
data class LoopVo @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    override val id: Int,
    override val title: String,
    override val color: Int,
    val tickStart: Long,
    override val loopStart: Long,
    override val loopEnd: Long,
    override val loopActiveDays: Int,
    override val interval: Long,
    val alarms: Int,
    override val enabled: Boolean,
    @Ignore override val isMock: Boolean = false,
) : LoopBase {

    override fun copy(
        id: Int,
        title: String,
        color: Int,
        loopStart: Long,
        loopEnd: Long,
        loopActiveDays: Int,
        interval: Long,
        enabled: Boolean,
        isMock: Boolean,
    ): LoopBase = LoopVo(
        id = id,
        title = title,
        color = color,
        loopStart = loopStart,
        loopEnd = loopEnd,
        loopActiveDays = loopActiveDays,
        interval = interval,
        enabled = enabled,
        isMock = isMock,
        // TO BE REMOVED
        tickStart = 0L,
        alarms = NO_ALARMS,
    )
}

fun LoopBase.asLoopVo(
    id: Int = this.id,
    title: String = this.title,
    color: Int = this.color,
    loopStart: Long = this.loopStart,
    loopEnd: Long = this.loopEnd,
    loopActiveDays: Int = this.loopActiveDays,
    interval: Long = this.interval,
    enabled: Boolean = this.enabled
) = LoopVo(
    id = id,
    title = title,
    color = color,
    loopStart = loopStart,
    loopEnd = loopEnd,
    loopActiveDays = loopActiveDays,
    interval = interval,
    enabled = enabled,
    // To be removed,
    tickStart = 0L,
    alarms = NO_ALARMS
)


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