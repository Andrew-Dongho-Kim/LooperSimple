package com.pnd.android.loop.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pnd.android.loop.data.dao.FullLoopDao
import com.pnd.android.loop.data.dao.LoopDao
import com.pnd.android.loop.data.dao.LoopDoneDao
import com.pnd.android.loop.data.dao.LoopGroupDao
import com.pnd.android.loop.data.dao.LoopRetrospectDao
import com.pnd.android.loop.data.dao.RoomTypeConverters

@Database(
    version = 1,
    entities = [
        LoopVo::class,
        LoopDoneVo::class,
        LoopGroupVo::class,
        LoopRelationVo::class,
        LoopRetrospectVo::class
    ],
    autoMigrations = [

    ],
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loopDao(): LoopDao
    abstract fun loopGroupDao(): LoopGroupDao
    abstract fun loopDoneDao(): LoopDoneDao
    abstract fun loopRetrospectDao(): LoopRetrospectDao
    abstract fun fullLoopDao(): FullLoopDao
}

