package com.pnd.android.loop.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.common.MAX_LOOPS_TOGETHER
import com.pnd.android.loop.ui.home.input.selector.MIN_DIFF_MINUTES
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 상세 화면과 같은 배경·여백을 쓰며, 입력 본문과 고정 저장 영역을 분리한다. */
@Composable
internal fun LoopEditor(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onCountLoopsAtSameTime: suspend (LoopBase) -> Int,
    onSave: suspend (LoopBase) -> Unit,
    onClose: () -> Unit,
) {
    var draft by rememberSaveable(loop.loopId, stateSaver = LoopEditorDraft.Saver) {
        mutableStateOf(LoopEditorDraft.from(loop))
    }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf<Pair<ScheduleDraft, Int>?>(null) }
    var checkFailed by remember { mutableStateOf<ScheduleDraft?>(null) }
    var retry by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val schedule = draft.schedule

    // 이름·색 편집에는 조회하지 않는다. 응답은 확인한 일정과 묶어 이전 결과로 저장되지 않게 한다.
    LaunchedEffect(schedule, retry) {
        checked = null
        checkFailed = null
        if (schedule.validDays && schedule.validTime) {
            try {
                checked = schedule to onCountLoopsAtSameTime(schedule.applyTo(loop))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                checkFailed = schedule
            }
        }
    }
    BackHandler { if (!saving) onClose() }

    val overlapCount = checked?.takeIf { it.first == schedule }?.second
    val blocker = editorBlocker(draft, checkFailed == schedule, overlapCount)
    val canSave = !saving && draft.hasChanges(loop) && blocker == null && overlapCount != null
    val status = when {
        saveFailed -> stringResource(R.string.detail_journal_save_failed)
        blocker != null -> blocker
        overlapCount == null -> stringResource(R.string.detail_schedule_checking)
        else -> null
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = AppColor.background,
        topBar = { EditorAppBar(enabled = !saving, onClose = onClose) },
        bottomBar = {
            EditorActions(
                saving = saving,
                canSave = canSave,
                status = status,
                isError = saveFailed || blocker != null,
                retryVisible = checkFailed == schedule,
                onRetry = { retry++ },
                onClose = onClose,
                onSave = {
                    if (canSave && !saving) {
                        saving = true
                        saveFailed = false
                        val snapshot = draft
                        scope.launch {
                            try {
                                val edited = snapshot.applyTo(loop)
                                // 편집 중 다른 루프가 바뀌었을 수 있으므로 저장 직전에 다시 확인한다.
                                val count = onCountLoopsAtSameTime(edited)
                                checked = snapshot.schedule to count
                                if (count <= MAX_LOOPS_TOGETHER) onSave(edited)
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
        },
    ) { contentPadding ->
        EditorForm(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            draft = draft,
            enabled = !saving,
            onChange = {
                draft = it
                saveFailed = false
            },
        )
    }
}

@Composable
private fun editorBlocker(draft: LoopEditorDraft, checkFailed: Boolean, overlapCount: Int?): String? = when {
    draft.title.isBlank() -> stringResource(R.string.detail_editor_name_required)
    !draft.schedule.validDays -> stringResource(R.string.warning_choose_at_least_one_day_of_the_week)
    !draft.schedule.validTime ->
        stringResource(R.string.warning_end_time_should_be_after_start_time, MIN_DIFF_MINUTES)
    checkFailed -> stringResource(R.string.detail_schedule_check_failed)
    overlapCount != null && overlapCount > MAX_LOOPS_TOGETHER ->
        stringResource(R.string.warning_up_to_max_loops, MAX_LOOPS_TOGETHER)
    else -> null
}

/** 시스템 인셋은 TopAppBar가 처리한다. 상세 앱바와 동일한 색과 타이포를 유지한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorAppBar(enabled: Boolean, onClose: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColor.surface,
            scrolledContainerColor = AppColor.surface,
        ),
        title = {
            Text(stringResource(R.string.detail_edit_loop),
                style = AppTypography.titleLarge.copy(color = AppColor.onSurface))
        },
        navigationIcon = {
            IconButton(enabled = enabled, onClick = onClose) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = AppColor.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
                )
            }
        },
    )
}

/** 저장 여부와 오류를 같은 위치에 표시한다. 키보드가 열려도 버튼은 입력창 아래에 남는다. */
@Composable
private fun EditorActions(
    saving: Boolean,
    canSave: Boolean,
    status: String?,
    isError: Boolean,
    retryVisible: Boolean,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(color = AppColor.background) {
        Column(Modifier.navigationBarsPadding()) {
            HairlineDivider()
            Column(
                modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding)
                    .padding(vertical = DetailSpacing.headerToContent),
                verticalArrangement = Arrangement.spacedBy(DetailSpacing.related),
            ) {
                if (status != null) {
                    Text(
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        text = status,
                        style = AppTypography.bodySmall.copy(
                            color = if (isError) AppColor.error else AppColor.onSurface.copy(alpha = 0.6f),
                        ),
                    )
                }
                if (retryVisible) {
                    TextActionButton(
                        text = stringResource(R.string.detail_journal_retry),
                        enabled = !saving,
                        onClick = onRetry,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DetailSpacing.related),
                ) {
                    ScheduleSecondaryButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        enabled = !saving,
                        onClick = onClose,
                    )
                    PrimaryPillButton(
                        modifier = Modifier.weight(2f),
                        text = stringResource(if (saving) R.string.detail_journal_saving else R.string.save),
                        enabled = canSave,
                        onClick = onSave,
                    )
                }
            }
        }
    }
}
