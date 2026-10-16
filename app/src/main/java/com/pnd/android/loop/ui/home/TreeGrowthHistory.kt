package com.pnd.android.loop.ui.home

import java.time.LocalDate

/** A saved observation is authoritative; replayed history is explicitly identified in the UI. */
data class TreeGrowthObservation(
    val date: LocalDate,
    val vitality: TreeVitality,
    val reconstructed: Boolean = true,
)

internal fun mergeTreeGrowthHistory(
    reconstructed: List<TreeGrowthObservation>,
    saved: List<TreeGrowthObservation>,
    current: TreeGrowthObservation?,
): List<TreeGrowthObservation> = buildMap {
    reconstructed.forEach { put(it.date, it) }
    saved.forEach { put(it.date, it) }
    current?.let { put(it.date, it) }
}.values.sortedBy { it.date }.let { ordered ->
    // Later edits can inflate replayed early growth. A saved later appearance bounds that
    // reconstruction, because earned stages/rewards could not have decreased on the way there.
    var stageCeiling = 10
    var rewardCeiling = 5
    val bounded = ordered.asReversed().map { observation ->
        val vitality = observation.vitality
        if (!observation.reconstructed) {
            stageCeiling = vitality.earnedStage
            rewardCeiling = vitality.rewardLevel
            observation
        } else observation.copy(vitality = vitality.copy(
            earnedStage = minOf(vitality.earnedStage, stageCeiling),
            earnedReward = minOf(vitality.rewardLevel, rewardCeiling),
        ))
    }.asReversed()
    var stage = 1
    var reward = 0
    bounded.map { observation ->
        val vitality = observation.vitality
        stage = maxOf(stage, vitality.earnedStage)
        reward = maxOf(reward, vitality.rewardLevel)
        if (observation.reconstructed) observation.copy(vitality = vitality.copy(
            earnedStage = stage, earnedReward = reward,
        )) else observation
    }
}

enum class TreeGrowthChange { GROWTH, WILT, RECOVERY, REWARD }

data class TreeGrowthEvent(val observation: TreeGrowthObservation, val change: TreeGrowthChange)

fun treeGrowthEvents(history: List<TreeGrowthObservation>): List<TreeGrowthEvent> =
    history.zipWithNext().flatMap { (before, after) ->
        buildList {
            if (after.vitality.earnedStage > before.vitality.earnedStage)
                add(TreeGrowthEvent(after, TreeGrowthChange.GROWTH))
            if (after.vitality.wiltLevel > before.vitality.wiltLevel)
                add(TreeGrowthEvent(after, TreeGrowthChange.WILT))
            if (after.vitality.wiltLevel < before.vitality.wiltLevel)
                add(TreeGrowthEvent(after, TreeGrowthChange.RECOVERY))
            if (after.vitality.rewardLevel > before.vitality.rewardLevel)
                add(TreeGrowthEvent(after, TreeGrowthChange.REWARD))
        }
    }
