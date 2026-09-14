package com.pnd.android.loop.data.history

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

/** Freeze the old settings once; never claim that unknown past settings were observed. */
class SeedLoopHistory : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        val knownFrom = LocalDate.now().toEpochDay()
        val createdDay = "CAST(julianday(date(created / 1000, 'unixepoch', 'localtime')) - 2440587.5 AS INTEGER)"
        db.execSQL(
            """INSERT INTO loop_revision (
                loopId, recordedAt, effectiveFrom, goalEffectiveFrom, knownFrom,
                title, color, startInDay, endInDay, activeDays, enabled, isAnyTime, weeklyGoal
            ) SELECT loopId, ?, $createdDay, $createdDay, ?,
                title, color, startInDay, endInDay, activeDays, enabled, isAnyTime, weeklyGoal
              FROM loop""",
            arrayOf(now, knownFrom),
        )
        val localDay = "CAST(julianday(date(date / 1000, 'unixepoch', 'localtime')) - 2440587.5 AS INTEGER)"
        db.execSQL("UPDATE loop_done SET localEpochDay = $localDay")
        db.execSQL("UPDATE loop_memo SET localEpochDay = $localDay")
        db.execSQL("""UPDATE loop_done SET revisionId = (
            SELECT revisionId FROM loop_revision WHERE loop_revision.loopId = loop_done.loopId
        )""")
    }
}
