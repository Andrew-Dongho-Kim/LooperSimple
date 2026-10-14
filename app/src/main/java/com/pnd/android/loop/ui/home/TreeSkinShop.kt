package com.pnd.android.loop.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TreeShopPage(vitality: TreeVitality, onNavigateUp: () -> Unit) {
    val store = rememberTreeSkinStore()
    val inventory = rememberTreeSkinInventory()
    var previewId by rememberSaveable { mutableStateOf(inventory.selected.packId) }
    var previewStage by rememberSaveable { mutableIntStateOf(10) }
    var previewWilt by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var effects by rememberSaveable { mutableIntStateOf(inventory.effectsFor(inventory.selected)) }
    val preview = TreeSkin.fromId(previewId)
    val previewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val owned = preview in inventory.unlocked
    val changed = inventory.selected != preview || inventory.effectsFor(preview) != effects
    val names = treeSkinRewardNames(preview)
    LaunchedEffect(vitality.growthPoints) { store.earn(vitality.growthPoints) }

    Column(Modifier.fillMaxSize().background(AppColor.background)) {
        SimpleAppBar(title = stringResource(R.string.tree_skin_shop), onNavigateUp = onNavigateUp)
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tree_shop_trees_tab)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tree_shop_effects_tab)) })
        }
        if (tab == 0) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundShapes.medium).background(AppColor.surfaceContainer).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.tree_skin_points, vitality.growthPoints),
                        style = AppTypography.titleMedium.copy(color = AppColor.onSurface))
                    Text(stringResource(R.string.tree_skin_unlock_help),
                        style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.tree_shop_all), style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
                    Text(stringResource(R.string.tree_shop_count, TreeSkin.entries.size),
                        style = AppTypography.labelMedium.copy(color = AppColor.onSurfaceVariant))
                }
                for (pair in TreeSkin.entries.chunked(2)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (skin in pair) TreeShopCard(skin, skin in inventory.unlocked, inventory.selected == skin,
                            preview == skin, Modifier.weight(1f)) {
                            previewId = skin.packId
                            effects = inventory.effectsFor(skin)
                            scope.launch { previewRequester.bringIntoView() }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Column(Modifier.fillMaxWidth().bringIntoViewRequester(previewRequester)
                    .clip(RoundShapes.medium).background(AppColor.surfaceContainer).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(preview.title), style = AppTypography.titleMedium.copy(color = AppColor.onSurface))
                    Text(stringResource(preview.description), style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                    LoopTreeScene(previewStage, 5, stringResource(R.string.tree_shop_preview_stage,
                        stringResource(preview.title), previewStage), Modifier.fillMaxWidth().height(220.dp), previewWilt, preview, effects)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.tree_shop_growth), style = AppTypography.labelMedium.copy(color = AppColor.onSurface))
                        Text(stringResource(R.string.tree_shop_stage, previewStage),
                            style = AppTypography.labelMedium.copy(color = AppColor.onSurfaceVariant))
                    }
                    val growthLabel = stringResource(R.string.tree_shop_growth)
                    Slider(value = previewStage.toFloat(), onValueChange = { previewStage = it.toInt().coerceIn(1, 10) },
                        valueRange = 1f..10f, steps = 8,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = growthLabel })
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (wilt in 0..3) FilterChip(selected = previewWilt == wilt, onClick = { previewWilt = wilt }, label = {
                            Text(if (wilt == 0) stringResource(R.string.garden_health_0)
                                else stringResource(R.string.garden_health_step, wilt))
                        })
                    }
                    TextButton(onClick = { tab = 1 }) { Text(stringResource(R.string.tree_shop_customize_effects)) }
                    Text(stringResource(R.string.tree_skin_preview_help),
                        style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                }
            }
        } else {
            // The preview stays visible while independently scrolling the effect controls.
            Column(Modifier.weight(1f)) {
                Text(stringResource(preview.title), Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = AppTypography.titleMedium.copy(color = AppColor.onSurface))
                LoopTreeScene(previewStage, 5, stringResource(R.string.tree_shop_preview_stage,
                    stringResource(preview.title), previewStage), Modifier.fillMaxWidth().height(190.dp), previewWilt, preview, effects)
                if (previewWilt > 0) TextButton(onClick = { previewWilt = 0 }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.tree_shop_effects_healthy))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tree_shop_effects_help),
                        style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                    for (effect in TreeEffect.entries) {
                        val name = stringResource(names[effect.level - 1])
                        val reward = treeRewards[effect.level - 1]
                        Row(Modifier.fillMaxWidth().clip(RoundShapes.medium).background(AppColor.surfaceContainer).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(name, style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
                                Text(if (effect.level <= vitality.rewardLevel) stringResource(R.string.tree_skin_unlocked)
                                    else stringResource(R.string.tree_shop_effect_unlock, reward.minimumPercent, reward.days),
                                    style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                            }
                            val toggleLabel = stringResource(R.string.tree_shop_effect_toggle, name)
                            Switch(checked = effect.enabled(effects), onCheckedChange = { enabled ->
                                effects = if (enabled) effects or effect.bit else effects and effect.bit.inv()
                            }, modifier = Modifier.semantics { contentDescription = toggleLabel })
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val applyLabel = stringResource(R.string.tree_skin_apply_button, stringResource(preview.title))
            Button(onClick = { store.apply(preview, effects) }, enabled = owned && changed,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = applyLabel }) {
                Text(if (!owned) stringResource(R.string.tree_shop_remaining, (preview.unlockPoints - vitality.growthPoints).coerceAtLeast(0))
                    else if (!changed) stringResource(R.string.tree_shop_applied)
                    else stringResource(R.string.tree_shop_apply_settings))
            }
            Text(stringResource(R.string.tree_shop_effects_apply_help),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
        }
    }
}

@Composable
private fun TreeShopCard(skin: TreeSkin, owned: Boolean, equipped: Boolean, previewing: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pack = rememberTreeVisualPack(skin)
    val image = rememberTreePackBitmap(pack, pack.treeImageKey(10, 0), sampleSize = 4)
    val previewLabel = stringResource(R.string.tree_skin_preview_button, stringResource(skin.title))
    Surface(onClick = onClick, modifier = modifier.semantics { contentDescription = previewLabel; selected = previewing },
        shape = RoundShapes.medium, color = AppColor.surfaceContainer,
        border = if (previewing) BorderStroke(1.dp, AppColor.primary) else null) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(image, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(124.dp))
            Text(stringResource(if (equipped) R.string.tree_skin_equipped else if (owned) R.string.tree_skin_unlocked
                else R.string.tree_skin_requires_points, skin.unlockPoints),
                style = AppTypography.labelSmall.copy(color = if (equipped) AppColor.primary else AppColor.onSurfaceVariant))
            Text(stringResource(skin.title), style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
            Text(if (skin.unlockPoints == 0) stringResource(R.string.tree_shop_default)
                else stringResource(R.string.tree_shop_milestone, skin.unlockPoints),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
        }
    }
}
