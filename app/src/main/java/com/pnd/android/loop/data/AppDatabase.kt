package com.pnd.android.loop.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LoopVo::class, LoopDoneVo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loopDao(): LoopDao
    abstract fun loopDoneDao(): LoopDoneDao

    abstract fun loopWithDoneDao(): LoopWithDoneDao
}