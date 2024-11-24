package com.pnd.android.loop.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pnd.android.loop.data.dao.FullLoopDao
import com.pnd.android.loop.data.dao.LoopDao
import com.pnd.android.loop.data.dao.LoopDoneDao
import com.pnd.android.loop.data.dao.LoopGroupDao
import com.pnd.android.loop.data.dao.LoopRecordDao
import com.pnd.android.loop.data.dao.LoopRecordVo
import com.pnd.android.loop.data.dao.LoopRetrospectDao
import com.pnd.android.loop.data.dao.RoomTypeConverters

@Database(
    version = 2,
    entities = [
        LoopVo::class,
        LoopDoneVo::class,
        LoopGroupVo::class,
        LoopRecordVo::class,
        LoopRelationVo::class,
        LoopRetrospectVo::class
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loopDao(): LoopDao
    abstract fun loopRecordDao(): LoopRecordDao
    abstract fun loopGroupDao(): LoopGroupDao
    abstract fun loopDoneDao(): LoopDoneDao
    abstract fun loopRetrospectDao(): LoopRetrospectDao
    abstract fun fullLoopDao(): FullLoopDao
}

