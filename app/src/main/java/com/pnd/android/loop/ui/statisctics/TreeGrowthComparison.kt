package com.pnd.android.loop.ui.statisctics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.home.NativeTreeScene
import com.pnd.android.loop.ui.home.TreeGrowthChange
import com.pnd.android.loop.ui.home.TreeGrowthObservation
import com.pnd.android.loop.ui.home.gardenHealthNames
import com.pnd.android.loop.ui.home.rememberTreeVisualPack
import com.pnd.android.loop.ui.home.treeCardColor
import com.pnd.android.loop.ui.home.treeGrowthEvents
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import java.time.LocalDate

internal fun treeComparisonDates(
    history: List<TreeGrowthObservation>, beforeEpoch: Long?, afterEpoch: Long?,
): Pair<TreeGrowthObservation, TreeGrowthObservation>? {
    if (history.isEmpty()) return null
    val after = history.firstOrNull { it.date.toEpochDay() == afterEpoch } ?: history.last()
    val before = history.firstOrNull { it.date.toEpochDay() == beforeEpoch && it.date < after.date }
        ?: history.firstOrNull { it.date >= after.date.minusDays(30) && it.date < after.date }
        ?: history.first()
    return before to after
}

/** Two dates share one skin, camera and scale so only actual growth/condition differs. */
@Composable
internal fun TreeGrowthComparison(history: List<TreeGrowthObservation>, modifier: Modifier = Modifier) {
    var beforeEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var afterEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAllEvents by rememberSaveable(beforeEpoch, afterEpoch) { mutableStateOf(false) }
    val selection = remember(history, beforeEpoch, afterEpoch) {
        treeComparisonDates(history, beforeEpoch, afterEpoch)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
        Text(stringResource(R.string.tree_compare_title), style = AppTypography.titleMedium)
        Text(stringResource(R.string.tree_compare_scope), style = AppTypography.bodySmall,
            color = AppColor.onSurfaceVariant)
        if (selection == null) {
            AppCard {
                Text(stringResource(R.string.tree_compare_empty_title), style = AppTypography.titleSmall)
                Text(stringResource(R.string.tree_compare_empty_description),
                    style = AppTypography.bodySmall, color = AppColor.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
            return@Column
        }
        val (before, after) = selection
        val sameDay = before.date == after.date
        val events = remember(history, before.date, after.date) {
            treeGrowthEvents(history).filter { it.observation.date > before.date && it.observation.date <= after.date }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TreeObservationPicker(stringResource(R.string.tree_compare_before), before,
                history.filter { it.date < after.date }, Modifier.weight(1f)) { beforeEpoch = it.date.toEpochDay() }
            TreeObservationPicker(stringResource(R.string.tree_compare_after), after,
                history.filter { it.date > before.date }, Modifier.weight(1f)) { afterEpoch = it.date.toEpochDay() }
        }
        AppCard(color = treeCardColor()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TreeComparisonPortrait(before, Modifier.weight(1f))
                TreeComparisonPortrait(after, Modifier.weight(1f))
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                val stages = after.vitality.earnedStage - before.vitality.earnedStage
                Text(if (stages > 0) stringResource(R.string.tree_compare_grew, stages)
                    else stringResource(R.string.tree_compare_stage_unchanged), style = AppTypography.titleSmall)
                Text(stringResource(R.string.tree_compare_same_skin), style = AppTypography.bodySmall,
                    color = AppColor.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (sameDay) {
            Text(stringResource(R.string.tree_compare_one_record), style = AppTypography.bodySmall,
                color = AppColor.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.tree_compare_changes), style = AppTypography.titleSmall,
                modifier = Modifier.padding(top = 8.dp))
            ComparisonValue(stringResource(R.string.tree_compare_stage),
                stringResource(R.string.tree_compare_stage_value, before.vitality.earnedStage, after.vitality.earnedStage))
            ComparisonValue(stringResource(R.string.tree_compare_points),
                stringResource(R.string.tree_compare_points_value, before.vitality.growthPoints, after.vitality.growthPoints))
            ComparisonValue(stringResource(R.string.tree_compare_health),
                stringResource(R.string.tree_compare_value,
                    stringResource(gardenHealthNames[before.vitality.wiltLevel]),
                    stringResource(gardenHealthNames[after.vitality.wiltLevel])))
            Text(stringResource(R.string.tree_compare_events), style = AppTypography.titleSmall,
                modifier = Modifier.padding(top = 12.dp))
            if (events.isEmpty()) {
                Text(stringResource(R.string.tree_compare_no_events), style = AppTypography.bodySmall,
                    color = AppColor.onSurfaceVariant)
            } else {
                val shown = if (showAllEvents) events else events.takeLast(4)
                shown.forEach { event ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(comparisonDate(event.observation.date), style = AppTypography.labelSmall,
                            color = AppColor.onSurfaceVariant)
                        Text(when (event.change) {
                            TreeGrowthChange.GROWTH -> stringResource(R.string.tree_compare_event_growth, event.observation.vitality.earnedStage)
                            TreeGrowthChange.WILT -> stringResource(R.string.tree_compare_event_wilt)
                            TreeGrowthChange.RECOVERY -> stringResource(R.string.tree_compare_event_recovery)
                            TreeGrowthChange.REWARD -> stringResource(R.string.tree_compare_event_reward)
                        }, style = AppTypography.bodyMedium)
                        if (event.observation.reconstructed) Text(stringResource(R.string.tree_compare_reconstructed),
                            style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant)
                    }
                }
                if (events.size > 4) TextButton(onClick = { showAllEvents = !showAllEvents }) {
                    Text(stringResource(if (showAllEvents) R.string.tree_compare_less else R.string.tree_compare_more))
                }
            }
        }
        Text(stringResource(R.string.tree_compare_history_note), style = AppTypography.bodySmall,
            color = AppColor.onSurfaceVariant)
    }
}

@Composable
private fun TreeComparisonPortrait(observation: TreeGrowthObservation, modifier: Modifier) {
    val vitality = observation.vitality
    val pack = rememberTreeVisualPack()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        NativeTreeScene(vitality.earnedStage, vitality.rewardLevel,
            stringResource(R.string.tree_image_description, vitality.earnedStage),
            Modifier.fillMaxWidth().height(140.dp), vitality.wiltLevel, pack, animationsRunning = false)
        Text(comparisonDate(observation.date), style = AppTypography.labelMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.tree_stage, vitality.earnedStage), style = AppTypography.titleSmall,
            modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(if (observation.reconstructed) R.string.tree_compare_reconstructed else R.string.tree_compare_saved),
            style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TreeObservationPicker(
    label: String, selected: TreeGrowthObservation, choices: List<TreeGrowthObservation>,
    modifier: Modifier, onSelected: (TreeGrowthObservation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = choices.size > 1,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(comparisonDate(selected.date), modifier = Modifier.weight(1f),
                    style = AppTypography.labelMedium)
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
            }
            if (expanded && choices.isNotEmpty()) {
                val allowed = remember(choices) { choices.associateBy { it.date.toEpochDay() } }
                val selectable = remember(allowed) {
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                            Math.floorDiv(utcTimeMillis, 86_400_000L) in allowed

                        override fun isSelectableYear(year: Int): Boolean =
                            choices.any { it.date.year == year }
                    }
                }
                val picker = rememberDatePickerState(
                    initialSelectedDateMillis = selected.date.toEpochDay() * 86_400_000L,
                    yearRange = choices.first().date.year..choices.last().date.year,
                    selectableDates = selectable,
                )
                DatePickerDialog(onDismissRequest = { expanded = false }, confirmButton = {
                    TextButton(onClick = {
                        picker.selectedDateMillis?.let { millis ->
                            allowed[Math.floorDiv(millis, 86_400_000L)]?.let(onSelected)
                        }
                        expanded = false
                    }, enabled = picker.selectedDateMillis != null) { Text(stringResource(android.R.string.ok)) }
                }, dismissButton = {
                    TextButton(onClick = { expanded = false }) { Text(stringResource(android.R.string.cancel)) }
                }) {
                    DatePicker(state = picker, showModeToggle = false)
                }
            }
        }
    }
}

@Composable
private fun ComparisonValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(1f), style = AppTypography.bodyMedium, color = AppColor.onSurfaceVariant)
        Text(value, Modifier.weight(1.4f), style = AppTypography.bodyMedium,
            color = AppColor.onSurface, textAlign = TextAlign.End)
    }
    HorizontalDivider()
}

@Composable
private fun comparisonDate(date: LocalDate): String =
    stringResource(R.string.tree_compare_date, date.year, date.monthValue, date.dayOfMonth)
