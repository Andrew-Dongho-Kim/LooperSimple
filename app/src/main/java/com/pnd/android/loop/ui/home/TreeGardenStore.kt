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

    private var observations: List<TreeGrowthObservation> = preferences.getString("state", null)?.let { text ->
        runCatching {
            val rows = JSONObject(text).optJSONArray("observations") ?: return@runCatching emptyList()
            (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                TreeGrowthObservation(LocalDate.ofEpochDay(row.getLong("date")), TreeVitality(
                    daysAt60 = row.optInt("days60", 0), daysAt80 = row.optInt("days80", 0),
                    earnedStage = row.getInt("stage").coerceIn(1, 10),
                    earnedReward = row.getInt("reward").coerceIn(0, 5),
                    growthPoints = row.getInt("points"),
                    growthPercent = row.optInt("growth", -1).takeIf { it >= 0 },
                    healthPercent = row.optInt("health", -1).takeIf { it >= 0 },
                    wiltLevel = row.getInt("wilt").coerceIn(0, 3),
                ), reconstructed = false)
            }
        }.getOrNull()
    }.orEmpty()
    private var latestHistory: List<TreeGrowthObservation> = observations

    @Synchronized
    fun evaluate(snapshot: LoopHistorySnapshot, today: LocalDate): TreeVitality {
        val replay = mutableListOf<TreeGrowthObservation>()
        val result = evaluateTreeGarden(snapshot, today, memory) { date, vitality ->
            replay.add(TreeGrowthObservation(date, vitality))
        }
        val current = if (replay.isNotEmpty() || observations.isNotEmpty() || result.memory.dailyPoints.isNotEmpty())
            TreeGrowthObservation(today.minusDays(1), result.vitality, reconstructed = false) else null
        val saved = mergeTreeGrowthHistory(emptyList(), observations, current)
        latestHistory = mergeTreeGrowthHistory(replay, saved, null).filter { it.date < today }
        if (result.memory != memory || saved != observations) {
            val json = JSONObject().put("stage", result.memory.earnedStage)
                .put("reward", result.memory.earnedReward)
                .put("credit", result.memory.migrationCredit)
                .put("points", JSONObject().apply {
                    result.memory.dailyPoints.forEach { (date, points) -> put(date.toString(), points) }
                })
                .put("observations", org.json.JSONArray().apply {
                    saved.forEach { observation ->
                        val value = observation.vitality
                        put(JSONObject().put("date", observation.date.toEpochDay())
                            .put("days60", value.daysAt60).put("days80", value.daysAt80)
                            .put("stage", value.earnedStage).put("reward", value.rewardLevel)
                            .put("points", value.growthPoints).put("wilt", value.wiltLevel)
                            .put("growth", value.growthPercent ?: -1)
                            .put("health", value.healthPercent ?: -1))
                    }
                })
            check(preferences.edit().putString("state", json.toString()).commit())
            memory = result.memory
            observations = saved
        }
        return result.vitality
    }

    @Synchronized
    fun history(snapshot: LoopHistorySnapshot, today: LocalDate): List<TreeGrowthObservation> {
        evaluate(snapshot, today)
        return latestHistory
    }
}
