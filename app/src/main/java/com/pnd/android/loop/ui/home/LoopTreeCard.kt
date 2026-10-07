package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface

internal val treeArt = listOf(
    R.drawable.loop_tree_01, R.drawable.loop_tree_02, R.drawable.loop_tree_03,
    R.drawable.loop_tree_04, R.drawable.loop_tree_05, R.drawable.loop_tree_06,
    R.drawable.loop_tree_07, R.drawable.loop_tree_08, R.drawable.loop_tree_09,
    R.drawable.loop_tree_10,
)
internal val rewardNames = listOf(
    R.string.tree_reward_light, R.string.tree_reward_fireflies, R.string.tree_reward_flowers,
    R.string.tree_reward_butterflies, R.string.tree_reward_aurora,
)

@Composable
internal fun treeCardColor(): Color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    Color(0xFF1B2923) else Color(0xFFF0F6E8)

@Composable
fun LoopTreeCard(
    overallRates: LoopRates,
    vitality: TreeVitality,
    hasEstimatedHistory: Boolean,
) {
    val percent = overallRates.donePercent.takeIf { overallRates.isReliable }
    val stage = treeGrowthStage(percent)
    var showGuide by rememberSaveable { mutableStateOf(false) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val treeGreen = if (dark) Color(0xFFB4D99B) else Color(0xFF3D7147)
    val subtle = AppColor.onSurface.copy(alpha = 0.68f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tree_title), Modifier.weight(1f),
                style = AppTypography.titleMedium.copy(color = AppColor.onSurface, fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.tree_stage, stage),
                Modifier.clip(RoundedCornerShape(20.dp)).background(treeGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                style = AppTypography.labelMedium.copy(color = treeGreen))
        }
        LoopTreeScene(treeArt[stage - 1], vitality.rewardLevel,
            stringResource(R.string.tree_image_description, stage),
            Modifier.fillMaxWidth().height(210.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tree_rate_period), Modifier.weight(1f),
                style = AppTypography.bodySmall.copy(color = subtle))
            Text(percent?.let { "$it%" } ?: stringResource(R.string.tree_rate_pending),
                style = AppTypography.titleSmall.copy(color = treeGreen, fontWeight = FontWeight.Bold))
        }
        LinearProgressIndicator(progress = { (percent ?: 0) / 100f },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = treeGreen, trackColor = treeGreen.copy(alpha = 0.12f))
        if (hasEstimatedHistory) Text(stringResource(R.string.tree_estimated),
            style = AppTypography.labelSmall.copy(color = subtle))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showGuide = true }
            .heightIn(min = 48.dp).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (vitality.rewardLevel == 0) stringResource(R.string.tree_reward_start) else
                    stringResource(R.string.tree_reward_current, vitality.rewardLevel,
                        stringResource(rewardNames[vitality.rewardLevel - 1])),
                    style = AppTypography.labelLarge.copy(color = treeGreen))
                val next = vitality.nextReward
                Text(if (next == null) stringResource(R.string.tree_reward_max) else
                    stringResource(R.string.tree_reward_next, next.minimumPercent,
                        vitality.daysFor(next).coerceAtMost(next.days), next.days),
                    style = AppTypography.bodySmall.copy(color = subtle))
            }
            Icon(Icons.Outlined.Info, stringResource(R.string.tree_guide_title),
                Modifier.size(20.dp), tint = subtle)
        }
    }
    if (showGuide) TreeGuide(stage, vitality) { showGuide = false }
}
