package com.pnd.android.loop.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.isAllowedDay
import com.pnd.android.loop.util.isAllowedTime
import com.pnd.android.loop.util.localTime

@Entity(tableName = "loop_filter")
data class LoopFilter(
    @PrimaryKey val id: Int = 0,
    val onlyToday: Boolean,
    val onlyEnabled: Boolean,
    val onlyProgress: Boolean
) {
    fun copy(
        onlyToday: Boolean = this.onlyToday,
        onlyEnabled: Boolean = this.onlyEnabled,
        onlyProgress: Boolean = this.onlyProgress
    ): LoopFilter = LoopFilter(
        onlyToday = onlyToday,
        onlyEnabled = onlyEnabled,
        onlyProgress = onlyProgress
    )

    companion object {
        val DEFAULT = LoopFilter(
            onlyToday = false,
            onlyEnabled = false,
            onlyProgress = false
        )

        fun Loop.filter(filter: LoopFilter): Boolean {
            return filter.filterEnabled(this) &&
                    filter.filterProgress(this) &&
                    filter.filterToday(this)
        }

        private fun LoopFilter.filterEnabled(loop: Loop): Boolean {
            return if (onlyEnabled) loop.enabled else true
        }

        private fun LoopFilter.filterProgress(loop: Loop): Boolean {
            return if (onlyProgress) loop.isAllowedDay() && loop.isAllowedTime() else true
        }

        private fun LoopFilter.filterToday(loop: Loop): Boolean {
            return if (onlyToday) loop.isAllowedDay() else true
        }
    }
}


@Dao
interface LoopFilterDao {
    @Query("SELECT * FROM loop_filter")
    fun get(): LiveData<LoopFilter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(loopFilter: LoopFilter)
}