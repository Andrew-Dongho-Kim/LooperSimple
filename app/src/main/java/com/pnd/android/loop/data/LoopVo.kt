package com.pnd.android.loop.data

import androidx.compose.runtime.Immutable
import androidx.lifecycle.LiveData
import androidx.room.*
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.util.*

@Immutable
@Entity(tableName = "loop")
data class LoopVo @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    override val id: Int,
    override val title: String,
    override val color: Int,
    override val created: Long,
    override val loopStart: Long,
    override val loopEnd: Long,
    override val loopActiveDays: Int,
    override val interval: Long,
    override val enabled: Boolean,
    @Ignore override val isMock: Boolean = false,
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
    ): LoopBase = LoopVo(
        id = id,
        title = title,
        color = color,
        created = created,
        loopStart = loopStart,
        loopEnd = loopEnd,
        loopActiveDays = loopActiveDays,
        interval = interval,
        enabled = enabled,
        isMock = isMock,
    )
}

fun LoopBase.asLoopVo(
    id: Int = this.id,
    title: String = this.title,
    color: Int = this.color,
    created: Long = this.created,
    loopStart: Long = this.loopStart,
    loopEnd: Long = this.loopEnd,
    loopActiveDays: Int = this.loopActiveDays,
    interval: Long = this.interval,
    enabled: Boolean = this.enabled
) = LoopVo(
    id = id,
    title = title,
    color = color,
    created = created,
    loopStart = loopStart,
    loopEnd = loopEnd,
    loopActiveDays = loopActiveDays,
    interval = interval,
    enabled = enabled,
)


@Dao
interface LoopDao {
    @Query("SELECT * FROM loop ORDER BY loopStart ASC, loopEnd ASC")
    fun allLoopsLiveData(): LiveData<List<LoopVo>>

    @Query("SELECT * FROM loop WHERE id=:loopId")
    suspend fun loop(loopId: Int): LoopVo

    @Query("SELECT * FROM loop WHERE id=:loopId")
    fun flowLoop(loopId: Int): Flow<LoopVo>

    @Query("SELECT * FROM loop")
    suspend fun allLoops(): List<LoopVo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(vararg loops: LoopVo): List<Long>

    @Query("DELETE FROM loop WHERE id = :id")
    suspend fun remove(id: Int)
}