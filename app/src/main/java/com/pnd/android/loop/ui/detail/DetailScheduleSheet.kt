package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDay.Companion.toggle
import com.pnd.android.loop.data.common.MAX_LOOPS_TOGETHER
import com.pnd.android.loop.ui.home.input.selector.MIN_DIFF_MINUTES
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.formatHourMinute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 초안은 화면 회전에도 유지되며 닫기·취소로 폐기한다. 저장 중에는 중복 조작과 닫기를 막는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleEditorSheet(
    loop: LoopBase,
    onCountLoopsAtSameTime: suspend (LoopBase) -> Int,
    onSave: suspend (LoopBase) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable(loop.loopId, stateSaver = ScheduleDraft.Saver) {
        mutableStateOf(ScheduleDraft.from(loop))
    }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf<Pair<ScheduleDraft, Int>?>(null) }
    var checkFailed by remember { mutableStateOf<ScheduleDraft?>(null) }
    var retry by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !saving },
    )
    LaunchedEffect(draft, retry) {
        checked = null
        checkFailed = null
        saveFailed = false
        if (draft.validDays && draft.validTime) {
            try {
                checked = draft to onCountLoopsAtSameTime(draft.applyTo(loop))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                checkFailed = draft
            }
        }
    }
    val overlapCount = checked?.takeIf { it.first == draft }?.second
    val blocker = when {
        !draft.validDays -> stringResource(R.string.warning_choose_at_least_one_day_of_the_week)
        !draft.validTime -> stringResource(R.string.warning_end_time_should_be_after_start_time, MIN_DIFF_MINUTES)
        checkFailed == draft -> stringResource(R.string.detail_schedule_check_failed)
        overlapCount != null && overlapCount > MAX_LOOPS_TOGETHER ->
            stringResource(R.string.warning_up_to_max_loops, MAX_LOOPS_TOGETHER)
        else -> null
    }
    val canSave = !saving && draft.hasChanges(loop) && blocker == null && overlapCount != null

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = AppColor.surfaceElevated,
        contentColor = AppColor.onSurface,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding()
                .padding(horizontal = Dimens.screenHorizontalPadding)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.detail_schedule_edit),
                    style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
                )
                IconButton(enabled = !saving, onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.detail_schedule_close), tint = AppColor.onSurface)
                }
            }
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(top = DetailSpacing.headerToContent),
                verticalArrangement = Arrangement.spacedBy(DetailSpacing.group),
            ) {
                ScheduleFields(draft = draft, enabled = !saving, onChange = { draft = it })
                if (blocker != null || saveFailed) {
                    Text(
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        text = blocker ?: stringResource(R.string.detail_schedule_save_failed),
                        style = AppTypography.bodySmall.copy(color = AppColor.error),
                    )
                } else if (overlapCount == null) {
                    Text(
                        text = stringResource(R.string.detail_schedule_checking),
                        style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
                    )
                }
                if (checkFailed == draft) {
                    TextActionButton(
                        text = stringResource(R.string.detail_journal_retry),
                        enabled = !saving,
                        onClick = { retry++ },
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = DetailSpacing.actions),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScheduleSecondaryButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.cancel),
                    enabled = !saving,
                    onClick = onDismiss,
                )
                PrimaryPillButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(if (saving) R.string.detail_schedule_saving else R.string.save),
                    enabled = canSave,
                    onClick = {
                        if (!saving) {
                            saving = true
                            saveFailed = false
                            val snapshot = draft
                            scope.launch {
                                try {
                                    val edited = snapshot.applyTo(loop)
                                    // 화면을 열어 둔 동안 다른 루프가 바뀌었을 수 있어 저장 직전에 재확인한다.
                                    val count = onCountLoopsAtSameTime(edited)
                                    checked = snapshot to count
                                    if (count <= MAX_LOOPS_TOGETHER) {
                                        onSave(edited)
                                        onDismiss()
                                    }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    saveFailed = true
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
internal fun ScheduleFields(draft: ScheduleDraft, enabled: Boolean, onChange: (ScheduleDraft) -> Unit) {
    var timeTarget by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var goalMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.group)) {
        Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.item)) {
            ScheduleFieldTitle(stringResource(R.string.detail_schedule_repeat))
            ScheduleDayChips(draft.activeDays, enabled) { onChange(draft.withDays(it)) }
        }
        Column {
            ScheduleFieldTitle(stringResource(R.string.detail_schedule_time))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = DetailSpacing.related)
                    .toggleable(draft.isAnyTime, enabled, Role.Checkbox) { onChange(draft.copy(isAnyTime = it)) }
                    .sizeIn(minHeight = MinTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = draft.isAnyTime,
                    onCheckedChange = null,
                    enabled = enabled,
                    colors = CheckboxDefaults.colors(checkedColor = AppColor.primary, checkmarkColor = AppColor.onPrimary),
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.anytime),
                    style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
                )
            }
            if (!draft.isAnyTime) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(true, false).forEach { start ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val label = stringResource(if (start) R.string.start else R.string.end)
                            Text(label, style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)))
                            ScheduleSecondaryButton(
                                modifier = Modifier.fillMaxWidth().semantics {
                                    contentDescription = label
                                },
                                text = (if (start) draft.startInDay else draft.endInDay).formatHourMinute(),
                                enabled = enabled,
                                onClick = { timeTarget = start },
                            )
                        }
                    }
                }
                if (draft.endInDay < draft.startInDay) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(R.string.detail_schedule_overnight),
                        style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.item)) {
            ScheduleFieldTitle(stringResource(R.string.detail_week_goal_label))
            Box {
                ScheduleSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = (if (draft.weeklyGoal == 0) stringResource(R.string.detail_week_goal_none)
                    else stringResource(R.string.detail_week_goal_times, draft.weeklyGoal)) + "  ▾",
                    enabled = enabled,
                    onClick = { goalMenu = true },
                )
                DropdownMenu(expanded = goalMenu, onDismissRequest = { goalMenu = false }) {
                    (0..activeDayCount(draft.activeDays)).forEach { goal ->
                        DropdownMenuItem(
                            text = {
                                Text(if (goal == 0) stringResource(R.string.detail_week_goal_none)
                                else stringResource(R.string.detail_week_goal_times, goal))
                            },
                            onClick = { onChange(draft.copy(weeklyGoal = goal)); goalMenu = false },
                        )
                    }
                }
            }
        }
    }
    timeTarget?.let { start ->
        key(start) {
            ScheduleTimeDialog(
                label = stringResource(if (start) R.string.start else R.string.end),
                initial = if (start) draft.startInDay else draft.endInDay,
                onDismiss = { timeTarget = null },
                onConfirm = { time ->
                    onChange(if (start) draft.copy(startInDay = time) else draft.copy(endInDay = time))
                    timeTarget = null
                },
            )
        }
    }
}

@Composable
private fun ScheduleFieldTitle(text: String) {
    Text(text, style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleDayChips(activeDays: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // 좁은 화면에서도 터치 영역은 유지하고, 7개가 들어가지 않으면 4개씩 줄을 바꾼다.
        val columns = if (maxWidth >= 48.dp * 7 + 6.dp * 6) 7 else 4
        FlowRow(
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LoopDay.ALL.forEachIndexed { index, day ->
                val selected = activeDays.isOn(day)
                val label = stringResource(DAYS_WITH_3CHARS_SUNDAY_FIRST[index])
                Box(
                    modifier = Modifier.size(MinTouchTarget).clip(RoundShapes.medium)
                        .background(if (selected) AppColor.primary.copy(alpha = 0.14f) else AppColor.surfaceContainer)
                        .toggleable(selected, enabled, Role.Checkbox) { onChange(activeDays.toggle(day)) }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(ABB_DAYS[index]),
                        style = AppTypography.bodyMedium.copy(
                            color = if (selected) AppColor.primary else AppColor.onSurface.copy(alpha = 0.6f),
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimeDialog(label: String, initial: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val minutes = (initial / MS_1MIN).toInt()
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColor.surfaceElevated,
        titleContentColor = AppColor.onSurface,
        title = { Text(label) },
        text = { TimeInput(state = state) },
        dismissButton = { TextActionButton(text = stringResource(R.string.cancel), onClick = onDismiss) },
        confirmButton = {
            TextActionButton(text = stringResource(R.string.save)) {
                onConfirm((state.hour * 60 + state.minute) * MS_1MIN)
            }
        },
    )
}
