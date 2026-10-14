package com.pnd.android.loop.ui.home

import android.app.Application
import com.pnd.android.loop.data.history.LoopHistorySnapshot
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Single atomic preference value keeps the ledger and earned milestones together. */
@Singleton
class TreeGardenStore @Inject constructor(application: Application) {
    private val preferences = application.getSharedPreferences("tree_garden", 0)
    private var memory: TreeGardenMemory = preferences.getString("state", null)?.let { text ->
        runCatching {
            val json = JSONObject(text)
            val ledger = json.getJSONObject("points")
            TreeGardenMemory(true, json.getInt("stage"), json.getInt("reward"),
                ledger.keys().asSequence().associate { it.toLong() to ledger.getInt(it) }, json.optInt("credit", 0))
        }.getOrNull()
    } ?: TreeGardenMemory()

    @Synchronized
    fun evaluate(snapshot: LoopHistorySnapshot, today: LocalDate): TreeVitality {
        val result = evaluateTreeGarden(snapshot, today, memory)
        if (result.memory != memory) {
            val json = JSONObject().put("stage", result.memory.earnedStage)
                .put("reward", result.memory.earnedReward)
                .put("credit", result.memory.migrationCredit)
                .put("points", JSONObject().apply {
                    result.memory.dailyPoints.forEach { (date, points) -> put(date.toString(), points) }
                })
            check(preferences.edit().putString("state", json.toString()).commit())
            memory = result.memory
        }
        return result.vitality
    }
}
