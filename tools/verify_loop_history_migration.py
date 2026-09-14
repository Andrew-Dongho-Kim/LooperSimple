"""Exercise Room's generated 9 -> 10 SQL against SQLite with existing user records.

Run after :app:kspDebugKotlin. Reads project files and uses in-memory databases only.
The seed statements are extracted from the production migration, not duplicated here.
"""
import datetime as dt
import json
import re
import sqlite3
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "app/schemas/com.pnd.android.loop.data.AppDatabase"
GENERATED = ROOT / "app/build/generated/ksp/debug/java/com/pnd/android/loop/data/AppDatabase_AutoMigration_9_10_Impl.java"
SEED = ROOT / "app/src/main/java/com/pnd/android/loop/data/history/SeedLoopHistory.kt"


def schema(version):
    return json.loads((SCHEMAS / f"{version}.json").read_text(encoding="utf-8"))["database"]


def old_database():
    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys=ON")
    for entity in schema(9)["entities"]:
        db.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        for index in entity["indices"]:
            db.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
    created = int(dt.datetime(2026, 9, 1).timestamp() * 1000)
    db.execute("INSERT INTO loop(loopId,title,color,created,startInDay,endInDay,activeDays,enabled,isAnyTime,weeklyGoal) VALUES(42,?,?,?,?,?,?,?,?,?)",
               ("old name", -123, created, 82800000, 3600000, 127, 1, 0, 4))
    db.execute("INSERT INTO loop(loopId,title,color,created,startInDay,endInDay,activeDays,enabled,isAnyTime,weeklyGoal) VALUES(99,?,?,?,?,?,?,?,?,?)",
               ("disabled", 55, created, -1, -1, 127, 0, 1, 0))
    for index, state in enumerate((-1, 0, 1, 2, 3)):
        date = int(dt.datetime(2026, 9, index + 1).timestamp() * 1000)
        db.execute("INSERT INTO loop_done(loopId,date,startInDay,endInDay,done) VALUES(42,?,?,?,?)",
                   (date, 82800000, 3600000, state))
    db.execute("INSERT INTO loop_memo(loopId,date,text) VALUES(42,?,?)", (created, "memo, with unicode 회고"))
    db.commit()
    return db


def migrate(db):
    java = GENERATED.read_text(encoding="utf-8")
    for sql in re.findall(r'db.execSQL\("([^"\n]+)"\);', java):
        db.execute(sql)
    kotlin = SEED.read_text(encoding="utf-8")
    variables = dict(re.findall(r'val (\w+) = "([^"\n]+)"', kotlin))
    # Both regular and triple-quoted production execSQL arguments, in source order.
    pattern = r'db.execSQL\(\s*(?:"""(.*?)"""|"([^"\n]*)")'
    now = int(dt.datetime.now().timestamp() * 1000)
    known = (dt.date.today() - dt.date(1970, 1, 1)).days
    for match in re.finditer(pattern, kotlin, re.S):
        sql = match[1] if match[1] is not None else match[2]
        for name, value in variables.items():
            sql = sql.replace("$" + name, value)
        db.execute(sql, (now, known) if "?" in sql else ())


class MigrationTest(unittest.TestCase):
    def setUp(self):
        self.db = old_database()
        self.before = self.db.execute("SELECT * FROM loop_done ORDER BY date").fetchall()
        self.memo = self.db.execute("SELECT * FROM loop_memo").fetchall()
        with self.db:
            migrate(self.db)

    def tearDown(self):
        self.db.close()

    def test_original_records_and_memos_are_preserved(self):
        actual = self.db.execute("SELECT loopId,date,startInDay,endInDay,done FROM loop_done ORDER BY date").fetchall()
        # Select explicitly because Room's schema field order is not a public contract.
        expected = [(42, int(dt.datetime(2026,9,i+1).timestamp()*1000),82800000,3600000,i-1) for i in range(5)]
        self.assertEqual(expected, actual)
        self.assertEqual("memo, with unicode 회고", self.db.execute("SELECT text FROM loop_memo").fetchone()[0])
        self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM loop_done WHERE timeSource != 0 OR startedAt IS NOT NULL OR endedAt IS NOT NULL").fetchone()[0])

    def test_baselines_and_local_dates_are_seeded(self):
        self.assertEqual(2, self.db.execute("SELECT COUNT(*) FROM loop_revision").fetchone()[0])
        first = (dt.date(2026,9,1)-dt.date(1970,1,1)).days
        values = self.db.execute("SELECT title, effectiveFrom, goalEffectiveFrom, knownFrom FROM loop_revision WHERE loopId=42").fetchone()
        self.assertEqual(("old name", first, first), values[:3])
        self.assertEqual((dt.date.today()-dt.date(1970,1,1)).days, values[3])
        self.assertEqual(list(range(first,first+5)), [r[0] for r in self.db.execute("SELECT localEpochDay FROM loop_done ORDER BY date")])
        self.assertEqual(5, self.db.execute("SELECT COUNT(*) FROM loop_done d JOIN loop_revision r ON d.revisionId=r.revisionId AND d.loopId=r.loopId").fetchone()[0])
        self.db.execute("UPDATE loop SET title='changed',activeDays=1 WHERE loopId=42")
        self.assertEqual("old name", self.db.execute("SELECT title FROM loop_revision WHERE loopId=42").fetchone()[0])

    def test_result_matches_room_schema_columns_indices_and_foreign_keys(self):
        for entity in schema(10)["entities"]:
            table = entity["tableName"]
            columns = {r[1]:r for r in self.db.execute(f"PRAGMA table_info({table})")}
            self.assertEqual({f["columnName"] for f in entity["fields"]}, set(columns))
            for field in entity["fields"]:
                actual = columns[field["columnName"]]
                self.assertEqual(field["affinity"], actual[2])
                self.assertEqual(field["notNull"], bool(actual[3]))
            indices = {r[1] for r in self.db.execute(f"PRAGMA index_list({table})")}
            self.assertTrue({i["name"] for i in entity["indices"]}.issubset(indices))
        self.assertEqual([], self.db.execute("PRAGMA foreign_key_check").fetchall())
        self.assertEqual("ok", self.db.execute("PRAGMA integrity_check").fetchone()[0])

    def test_delete_cascades_and_undo_can_restore_all_original_identities(self):
        tables = ("loop", "loop_revision", "loop_done", "loop_memo")
        rows = {t:self.db.execute(f"SELECT * FROM {t} WHERE loopId=42").fetchall() for t in tables}
        self.db.execute("DELETE FROM loop WHERE loopId=42")
        for table in tables:
            self.assertEqual(0,self.db.execute(f"SELECT COUNT(*) FROM {table} WHERE loopId=42").fetchone()[0])
        for table in tables:
            for row in rows[table]:
                self.db.execute(f"INSERT INTO {table} VALUES({','.join('?' for _ in row)})",row)
            self.assertEqual(rows[table],self.db.execute(f"SELECT * FROM {table} WHERE loopId=42").fetchall())
        self.assertEqual([],self.db.execute("PRAGMA foreign_key_check").fetchall())


if __name__ == "__main__":
    if not GENERATED.exists():
        raise SystemExit("Run :app:kspDebugKotlin before this migration verification.")
    unittest.main(verbosity=2)
