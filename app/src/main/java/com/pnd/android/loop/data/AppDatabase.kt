package com.pnd.android.loop.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LoopVo::class, LoopDoneVo::class], version = 3)
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


val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS 'loop_done2' (" +
                    "'loopId' INTEGER NOT NULL, " +
                    "'date' INTEGER NOT NULL, " +
                    "'done' INTEGER NOT NULL, " +
                    "PRIMARY KEY('loopId', 'date'), " +
                    "CONSTRAINT fk_loop_id FOREIGN KEY(loopId) REFERENCES loop(id) ON DELETE CASCADE);"
        )

        db.execSQL(
            "INSERT INTO loop_done2(loopId, date, done) " +
                    "SELECT loopId, date, done FROM loop_done"
        )
        db.execSQL("DROP TABLE loop_done")
        db.execSQL("ALTER TABLE loop_done2 RENAME TO loop_done")
    }
}