package com.pnd.android.loop.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppDialog
import com.pnd.android.loop.ui.common.AppSegmentedControl
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import kotlinx.coroutines.launch

private val rewardMotionDescriptions = listOf(
    R.string.tree_preview_light, R.string.tree_preview_fireflies, R.string.tree_preview_flowers,
    R.string.tree_preview_butterflies, R.string.tree_preview_aurora,
)

/** A freely browsable gallery. Preview choices never change the user's earned growth or rewards. */
@Composable
internal fun TreeGuide(currentStage: Int, vitality: TreeVitality, onDismiss: () -> Unit) {
    var stage by rememberSaveable { mutableIntStateOf(currentStage.coerceIn(1, 10)) }
    var effect by rememberSaveable { mutableIntStateOf(vitality.rewardLevel) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val configuration = LocalConfiguration.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    fun showSelection() { scope.launch { scroll.animateScrollTo(0) } }
    AppDialog(onDismiss = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 560.dp)
            .heightIn(max = (configuration.screenHeightDp * 0.9f).dp),
    ) {
        // The body can scroll even in landscape/large fonts; the close action stays reachable.
        Column(Modifier.weight(1f, fill = false).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.tree_guide_title),
                style = AppTypography.titleLarge.copy(color = AppColor.onSurface))
            Text(stringResource(R.string.tree_preview_hint),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
            Column(Modifier.fillMaxWidth().clip(RoundShapes.medium)
                .background(AppColor.surfaceContainer).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.tree_preview_combination, stage,
                    if (effect == 0) stringResource(R.string.tree_preview_no_effect) else
                        stringResource(rewardNames[effect - 1])),
                    style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
                LoopTreeScene(treeArt[stage - 1], effect,
                    stringResource(R.string.tree_image_description, stage),
                    Modifier.fillMaxWidth().height(180.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { stage = (stage - 1).coerceAtLeast(1) }, enabled = stage > 1,
                        modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.tree_preview_previous),
                            style = AppTypography.labelLarge.copy(color = if (stage > 1) AppColor.primary else AppColor.onSurfaceVariant))
                    }
                    TextButton(onClick = { stage = (stage + 1).coerceAtMost(10) }, enabled = stage < 10,
                        modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.tree_preview_next),
                            style = AppTypography.labelLarge.copy(color = if (stage < 10) AppColor.primary else AppColor.onSurfaceVariant))
                    }
                }
                TextButton(onClick = { stage = currentStage; effect = vitality.rewardLevel },
                    modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.tree_preview_current),
                        style = AppTypography.labelLarge.copy(color = AppColor.primary))
                }
            }
            AppSegmentedControl(options = listOf(0, 1), selected = tab, onSelected = { tab = it },
                label = { stringResource(if (it == 0) R.string.tree_preview_growth_tab else R.string.tree_preview_effect_tab) })
            if (tab == 0) {
                Text(stringResource(R.string.tree_guide_growth),
                    style = AppTypography.bodyMedium.copy(color = AppColor.onSurface))
                Text(stringResource(R.string.tree_preview_formula),
                    style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(2) { column ->
                                val number = row * 2 + column + 1
                                Column(Modifier.weight(1f).guideSelection(stage == number) { stage = number; showSelection() }
                                    .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TreeGuideThumbnail(number - 1)
                                    Text(stringResource(R.string.tree_stage, number),
                                        style = AppTypography.labelLarge.copy(color = AppColor.onSurface),
                                        textAlign = TextAlign.Center)
                                    Text(stringResource(R.string.tree_preview_range,
                                        (number - 1) * 10, if (number == 10) 100 else number * 10 - 1),
                                        style = AppTypography.labelMedium.copy(color = AppColor.onSurfaceVariant))
                                }
                            }
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.tree_guide_rewards),
                    style = AppTypography.bodyMedium.copy(color = AppColor.onSurface))
                Text(stringResource(R.string.tree_preview_effect_help),
                    style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tree_preview_no_effect),
                        Modifier.fillMaxWidth().guideSelection(effect == 0) { effect = 0; showSelection() }.padding(12.dp),
                        style = AppTypography.bodyMedium.copy(color = AppColor.onSurface))
                    treeRewards.forEach { reward ->
                        val days = vitality.daysFor(reward).coerceAtMost(reward.days)
                        Column(Modifier.fillMaxWidth().guideSelection(effect == reward.level) { effect = reward.level; showSelection() }
                            .padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.tree_reward_requirement, reward.level,
                                stringResource(rewardNames[reward.level - 1]), reward.minimumPercent, reward.days),
                                style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
                            Text(stringResource(rewardMotionDescriptions[reward.level - 1]),
                                style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                            Text(if (reward.level <= vitality.rewardLevel) stringResource(R.string.tree_preview_earned) else
                                stringResource(R.string.tree_preview_progress, days, reward.days, reward.days - days),
                                style = AppTypography.labelMedium.copy(color = AppColor.primary))
                        }
                    }
                }
                HorizontalDivider(color = AppColor.outlineVariant)
                Text(stringResource(R.string.tree_preview_how_to),
                    style = AppTypography.titleMedium.copy(color = AppColor.onSurface))
                Text(stringResource(R.string.tree_preview_actions),
                    style = AppTypography.bodyMedium.copy(color = AppColor.onSurface))
                Text(stringResource(R.string.tree_guide_rules),
                    style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
            Text(stringResource(R.string.tree_guide_close),
                style = AppTypography.titleMedium.copy(color = AppColor.primary, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun Modifier.guideSelection(selected: Boolean, onSelect: () -> Unit): Modifier =
    clip(RoundShapes.medium)
        .background(if (selected) AppColor.primary.copy(alpha = 0.10f) else AppColor.surfaceContainer)
        .border(1.dp, if (selected) AppColor.primary else AppColor.outlineVariant, RoundShapes.medium)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)

/** Keep the full gallery's memory footprint small; the selected tree is rendered at full quality. */
@Composable
private fun TreeGuideThumbnail(index: Int) {
    val resources = LocalContext.current.resources
    val bitmap = remember(index, resources) {
        BitmapFactory.decodeResource(resources, treeArt[index], BitmapFactory.Options().apply {
            inSampleSize = 4
        }).asImageBitmap()
    }
    Image(bitmap, null, Modifier.height(72.dp).fillMaxWidth())
}
