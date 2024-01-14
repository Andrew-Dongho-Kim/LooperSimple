package com.pnd.android.loop.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LoopVo::class, LoopDoneVo::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loopDao(): LoopDao
    abstract fun loopDoneDao(): LoopDoneDao

    abstract fun loopWithDoneDao(): LoopWithDoneDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS 'loop2' (" +
                "'id' INTEGER NOT NULL, " +
                "'title' TEXT NOT NULL, " +
                "'color' INTEGER NOT NULL, " +
                "'created' INTEGER DEFAULT 0 NOT NULL, " +
                "'loopStart' INTEGER NOT NULL, " +
                "'loopEnd' INTEGER NOT NULL, " +
                "'loopActiveDays' INTEGER NOT NULL, " +
                "'interval' INTEGER NOT NULL, " +
                "'enabled' INTEGER DEFAULT 0 NOT NULL, " +
                "PRIMARY KEY('id'))")

        db.execSQL("INSERT INTO loop2(id, title, color, loopStart, loopEnd, loopActiveDays, interval, enabled) " +
                "SELECT id, title, color, loopStart, loopEnd, loopActiveDays, interval, enabled  FROM loop")
        db.execSQL("DROP TABLE loop")
        db.execSQL("ALTER TABLE loop2 RENAME TO loop")
    }
}