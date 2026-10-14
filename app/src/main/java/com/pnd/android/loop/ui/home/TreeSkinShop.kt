package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppDialog
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.surfaceContainer
import kotlinx.coroutines.launch

@Composable
internal fun TreeSkinShop(vitality: TreeVitality, onDismiss: () -> Unit) {
    val store = rememberTreeSkinStore()
    val inventory = rememberTreeSkinInventory()
    var previewId by rememberSaveable { mutableStateOf(inventory.selected.packId) }
    var previewWilt by rememberSaveable { mutableIntStateOf(0) }
    val preview = TreeSkin.fromId(previewId)
    val height = LocalConfiguration.current.screenHeightDp
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(vitality.growthPoints) { store.earn(vitality.growthPoints) }
    AppDialog(onDismiss = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 560.dp).heightIn(max = (height * 0.9f).dp)) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.tree_skin_shop), style = AppTypography.titleLarge.copy(color = AppColor.onSurface))
            Text(stringResource(R.string.tree_skin_points, vitality.growthPoints),
                style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
            Text(stringResource(R.string.tree_skin_unlock_help),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
            Column(Modifier.fillMaxWidth().clip(RoundShapes.medium).background(AppColor.surfaceContainer).padding(12.dp)) {
                Text(stringResource(R.string.tree_skin_preview, stringResource(preview.title)),
                    style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
                LoopTreeScene(10, 5, stringResource(preview.title), Modifier.fillMaxWidth().height(220.dp), previewWilt, preview)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (wilt in 0..3) TextButton(onClick = { previewWilt = wilt }) {
                        Text(if (wilt == 0) stringResource(R.string.garden_health_0)
                            else stringResource(R.string.garden_health_step, wilt))
                    }
                }
                Text(stringResource(R.string.tree_skin_preview_help),
                    style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
            }
            for (skin in TreeSkin.entries) {
                val owned = skin in inventory.unlocked
                val previewLabel = stringResource(R.string.tree_skin_preview_button, stringResource(skin.title))
                val applyLabel = stringResource(R.string.tree_skin_apply_button, stringResource(skin.title))
                Column(Modifier.fillMaxWidth().clip(RoundShapes.medium).background(AppColor.surfaceContainer).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(skin.title), style = AppTypography.titleMedium.copy(color = AppColor.onSurface))
                    Text(stringResource(skin.description), style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant))
                    Text(stringResource(if (owned) R.string.tree_skin_unlocked else R.string.tree_skin_requires_points,
                        skin.unlockPoints), style = AppTypography.labelMedium.copy(color = AppColor.onSurfaceVariant))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { previewId = skin.packId; scope.launch { scroll.animateScrollTo(0) } },
                            modifier = Modifier.semantics { contentDescription = previewLabel }) {
                            Text(stringResource(R.string.tree_skin_preview_action))
                        }
                        Button(onClick = { store.select(skin); previewId = skin.packId; scope.launch { scroll.animateScrollTo(0) } },
                            modifier = Modifier.semantics { contentDescription = applyLabel }, enabled = owned && inventory.selected != skin) {
                            Text(stringResource(if (inventory.selected == skin) R.string.tree_skin_equipped
                                else if (owned) R.string.tree_skin_equip else R.string.tree_skin_locked))
                        }
                    }
                }
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tree_skin_close)) }
    }
}
