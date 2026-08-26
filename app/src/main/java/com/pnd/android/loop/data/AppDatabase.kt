package com.pnd.android.loop.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.pnd.android.loop.data.dao.FullLoopDao
import com.pnd.android.loop.data.dao.LoopDao
import com.pnd.android.loop.data.dao.LoopDoneDao
import com.pnd.android.loop.data.dao.LoopRetrospectDao
import com.pnd.android.loop.data.dao.RoomTypeConverters

@Database(
    version = 9,
    entities = [
        LoopVo::class,
        LoopDoneVo::class,
        LoopRetrospectVo::class
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6), // adds indices on loop_done(loopId, date)
        // 그룹 기능 제거: loop_group / loop_relation 테이블을 드롭한다. 기존 사용자의 DB에는
        // 두 테이블이 남아 있으므로, 마이그레이션 없이 엔티티만 지우면 스키마 검증에서
        // IllegalStateException 으로 앱이 죽는다.
        AutoMigration(from = 6, to = 7, spec = AppDatabase.DropGroupTables::class),
        // loop.weeklyGoal 추가(기본값 0 = 목표 없음). 기존 루프는 지금까지와 똑같이
        // "활동 요일 전부"를 기준으로 계산된다.
        AutoMigration(from = 7, to = 8),
        // 알람 반복 주기(interval) 기능 제거: loop.interval 컬럼을 드롭한다. 기존 사용자의 DB에는
        // 컬럼이 남아 있으므로, 마이그레이션 없이 엔티티에서만 지우면 스키마 검증에서
        // IllegalStateException 으로 앱이 죽는다.
        AutoMigration(from = 8, to = 9, spec = AppDatabase.DropLoopInterval::class),
    ],
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loopDao(): LoopDao
    abstract fun loopDoneDao(): LoopDoneDao
    abstract fun loopRetrospectDao(): LoopRetrospectDao
    abstract fun fullLoopDao(): FullLoopDao

    /**
     * 6 → 7 자동 마이그레이션 명세. loop_relation 이 loop_group 을 FK 로 참조하므로
     * 자식 테이블(loop_relation)을 먼저 지우도록 순서를 둔다.
     */
    @DeleteTable.Entries(
        DeleteTable(tableName = "loop_relation"),
        DeleteTable(tableName = "loop_group"),
    )
    class DropGroupTables : AutoMigrationSpec

    /** 8 → 9 자동 마이그레이션 명세. 더 이상 쓰지 않는 알람 반복 주기 컬럼을 지운다. */
    @DeleteColumn(tableName = "loop", columnName = "interval")
    class DropLoopInterval : AutoMigrationSpec
}
