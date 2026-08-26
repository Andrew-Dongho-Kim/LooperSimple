package com.pnd.android.loop.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.data.common.MAX_LOOPS_TOGETHER
import com.pnd.android.loop.data.common.MAX_WEEKLY_GOAL
import com.pnd.android.loop.data.common.NO_WEEKLY_GOAL
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.home.input.selector.ColorSelector
import com.pnd.android.loop.ui.home.input.selector.MIN_DIFF_MINUTES
import com.pnd.android.loop.ui.home.input.selector.StartEndTimeSelector
import com.pnd.android.loop.ui.home.input.selector.isLoopDurationTooShort
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.ui.theme.surfaceContainer
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// 루프 수정 — 액션 바의 연필 하나로 들어오는 전체 화면 편집기
// ─────────────────────────────────────────────────────────────────────────────

/** 편집기 카드 사이, 그리고 목록 위아래의 간격. */
private val EditorCardSpacing = 12.dp

/** 섹션 제목과 그 아래 조작 UI 사이의 간격. */
private val EditorTitleSpacing = 14.dp

/**
 * 편집 중인 값 한 벌. 저장을 누를 때까지 DB 에 닿지 않는다.
 *
 * 값을 한 곳에 모아 두는 이유는 화면의 두 층이 같은 초안을 봐야 하기 때문이다 — 앱바의 저장
 * 버튼은 초안이 유효한지 알아야 하고, 이름 칸은 지금 고른 색이 무엇인지 알아야 한다.
 */
@Stable
internal class LoopDraft(loop: LoopBase) {
    var title by mutableStateOf(loop.title)
    var color by mutableStateOf(loop.color)
    var isAnyTime by mutableStateOf(loop.isAnyTime)
    var startInDay by mutableStateOf(loop.startInDay)
    var endInDay by mutableStateOf(loop.endInDay)
    var activeDays by mutableStateOf(loop.activeDays)
    var weeklyGoal by mutableStateOf(loop.weeklyGoal)

    /**
     * 초안을 [loop] 에 얹은 결과. 초안이 손대지 않는 값(활성 여부 · 생성 시각)은 그대로 남는다.
     *
     * '언제든지'로 바꾸면 시각은 홈의 추가 UX와 같은 규칙으로 [ANY_TIME] 으로 비운다. 활동 요일을
     * 줄여 주간 목표가 채울 수 없는 값이 됐다면 목표도 함께 줄인다 — 주 3일만 활동하는 루프에
     * 주 5회 목표가 남아 있으면 영원히 달성되지 않는다.
     */
    fun applyTo(loop: LoopBase): LoopBase = loop.copyAs(
        title = title.trim(),
        color = color,
        isAnyTime = isAnyTime,
        startInDay = if (isAnyTime) ANY_TIME else startInDay,
        endInDay = if (isAnyTime) ANY_TIME else endInDay,
        activeDays = activeDays,
        weeklyGoal = weeklyGoal.coerceAtMost(activeDayCount(activeDays)),
    )
}

/**
 * 루프의 색 · 이름 · 시간 · 요일 · 주간 목표를 한 화면에서 고친다.
 *
 * 예전에는 이름은 요약 헤더의 연필, 색은 이름 옆 점, 시간·요일·목표는 스케줄 섹션의 '시간 수정'
 * 으로 고칠 곳이 세 군데에 흩어져 있었다. 고칠 자리를 찾는 일이 고치는 일보다 어려웠다. 지금은
 * 액션 바의 연필 하나가 이 화면으로 들어오는 유일한 입구다.
 *
 * 저장을 누를 때까지 값은 [LoopDraft] 안에만 머문다. 저장할 수 없는 상태는 앱바 바로 아래
 * 경고 줄로 이유를 말하고 저장 버튼을 잠근다 — 눌러 본 뒤에 알게 되는 일이 없도록.
 */
@Composable
internal fun LoopEditor(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onCountLoopsAtSameTime: suspend (LoopBase) -> Int,
    onSave: suspend (LoopBase) -> Unit,
    onClose: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val draft = remember(loop.loopId) { LoopDraft(loop) }
    val edited = draft.applyTo(loop)

    // 같은 시간대 과밀 여부는 DB 를 봐야 알 수 있어, 초안의 시간대가 바뀔 때마다 한 번씩 확인한다.
    var loopsAtSameTime by remember(loop.loopId) { mutableStateOf(0) }
    LaunchedEffect(draft.isAnyTime, draft.startInDay, draft.endInDay, draft.activeDays) {
        loopsAtSameTime = onCountLoopsAtSameTime(edited)
    }

    // 시스템 뒤로가기는 화면을 벗어나는 대신 편집기를 닫는다. 상세 화면으로 돌아가는 것이
    // 편집기를 열기 전 상태이므로, X 를 누른 것과 같은 결과여야 한다.
    BackHandler(onBack = onClose)

    val blocker = saveBlockerMessage(draft = draft, loopsAtSameTime = loopsAtSameTime)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColor.background),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                EditorAppBar(
                    saveEnabled = blocker == null,
                    onClose = onClose,
                    onSave = { coroutineScope.launch { onSave(edited) } },
                )
                // 저장을 막는 이유는 저장 버튼과 같은 눈높이에 둔다. 어느 칸이 잘못됐는지
                // 화면을 훑어 내려가며 찾지 않아도 된다.
                if (blocker != null) {
                    EditorWarning(
                        modifier = Modifier.padding(
                            start = Dimens.screenHorizontalPadding,
                            end = Dimens.screenHorizontalPadding,
                            bottom = EditorCardSpacing,
                        ),
                        text = blocker,
                    )
                }
            }
        },
    ) { contentPadding ->
        EditorForm(
            modifier = Modifier.padding(paddingValues = contentPadding),
            draft = draft,
        )
    }
}

/** 편집기의 앱바: [X 닫기] · 제목 · [저장]. 저장은 스크롤과 무관하게 늘 같은 자리에 있다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorAppBar(
    modifier: Modifier = Modifier,
    saveEnabled: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    TopAppBar(
        modifier = modifier.background(color = AppColor.surface),
        title = {
            Text(
                text = stringResource(id = R.string.detail_edit_loop),
                style = AppTypography.titleLarge.copy(color = AppColor.onSurface),
            )
        },
        navigationIcon = {
            AppBarIcon(
                imageVector = Icons.Outlined.Close,
                color = AppColor.onSurface,
                descriptionResId = R.string.cancel,
                onClick = onClose,
            )
        },
        actions = {
            TextActionButton(
                text = stringResource(id = R.string.save),
                enabled = saveEnabled,
                onClick = onSave,
            )
        },
    )
}

/**
 * 고칠 것들을 한 줄로 세운 본문. 순서는 "무엇인지(이름 · 색) → 언제인지(시간 · 요일) →
 * 얼마나 자주인지(주간 목표)" 로, 홈에서 루프를 처음 만들 때 묻는 순서와 같다.
 */
@Composable
private fun EditorForm(
    modifier: Modifier = Modifier,
    draft: LoopDraft,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .imePadding()
            .padding(
                horizontal = Dimens.screenHorizontalPadding,
                vertical = EditorCardSpacing,
            ),
        verticalArrangement = Arrangement.spacedBy(EditorCardSpacing),
    ) {
        EditorSection(title = stringResource(id = R.string.detail_loop_name)) {
            NameField(
                color = draft.color,
                value = draft.title,
                onValueChange = { draft.title = it },
            )
        }

        EditorSection(
            title = stringResource(id = R.string.detail_loop_color),
            // 색 선택기는 자체 위쪽 여백을 갖고 있어, 제목 간격을 또 두면 두 겹으로 벌어진다.
            contentPadding = PaddingValues(horizontal = CardPadding),
        ) {
            ColorSelector(
                selectedColor = draft.color,
                onColorSelected = { draft.color = it },
            )
        }

        EditorSection(
            title = stringResource(id = R.string.detail_schedule),
            // 시간 선택기도 자체 여백을 갖고 있다. 가로 여백이 두 겹으로 쌓이면 시·분 스테퍼와
            // 요일 칸이 눌러 맞추기 어려울 만큼 좁아진다.
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            StartEndTimeSelector(
                isAnyTimeChecked = draft.isAnyTime,
                onIsAnyTimeCheckChanged = { draft.isAnyTime = it },
                selectedStartTime = draft.startInDay,
                onStartTimeSelected = { draft.startInDay = it },
                selectedEndTime = draft.endInDay,
                onEndTimeSelected = { draft.endInDay = it },
                selectedDays = draft.activeDays,
                onSelectedDayChanged = { draft.activeDays = it },
            )
        }

        EditorSection(
            title = stringResource(id = R.string.detail_week_goal_label),
            hint = stringResource(id = R.string.detail_week_goal_hint),
        ) {
            WeeklyGoalChips(
                activeDays = draft.activeDays,
                weeklyGoal = draft.weeklyGoal,
                onWeeklyGoalChanged = { draft.weeklyGoal = it },
            )
        }
    }
}

/**
 * 편집기의 한 칸. 무엇을 고치는 칸인지 제목으로 밝히고, 그 아래에 조작 UI 하나만 둔다.
 *
 * 네 가지를 카드 한 장에 쌓지 않은 이유는 경계 때문이다. 시간 선택기와 목표 칩이 한 장 안에
 * 붙어 있으면 어디까지가 '시간'이고 어디부터가 '목표'인지 눈으로 구분되지 않는다.
 *
 * [contentPadding] 은 제목이 아니라 내용에만 걸린다. 제목은 어느 칸에서나 카드 여백에 맞춰
 * 같은 세로선에서 시작하고, 자체 여백을 가진 선택기(색 · 시간)만 내용 쪽 여백을 덜어 낸다.
 */
@Composable
private fun EditorSection(
    modifier: Modifier = Modifier,
    title: String,
    hint: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = CardPadding,
        end = CardPadding,
        top = EditorTitleSpacing,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    DetailCard(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = CardPadding),
    ) {
        Column(modifier = Modifier.padding(horizontal = CardPadding)) {
            Text(
                text = title,
                style = AppTypography.labelMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.5f),
                ),
            )
            if (hint != null) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = hint,
                    style = AppTypography.bodySmall.copy(
                        color = AppColor.onSurface.copy(alpha = 0.4f),
                    ),
                )
            }
        }
        Column(
            modifier = Modifier.padding(paddingValues = contentPadding),
            content = content,
        )
    }
}

/**
 * 이름 입력 칸. 왼쪽 점은 지금 고른 색을 그대로 비추므로, 아래에서 색을 바꾸면 그 결과가
 * 이름 옆에서 바로 보인다 — 상세 화면 앱바에서 보게 될 모습과 같은 조합이다.
 */
@Composable
private fun NameField(
    modifier: Modifier = Modifier,
    color: Int,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(LoopColorDotSize)
                .clip(CircleShape)
                .background(color.compositeOverOnSurface()),
        )
        BasicTextField(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(AppColor.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            textStyle = AppTypography.titleMedium.copy(color = AppColor.onSurface),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.detail_loop_name),
                        style = AppTypography.titleMedium.copy(
                            color = AppColor.onSurface.copy(alpha = 0.3f),
                        ),
                    )
                }
                innerTextField()
            },
        )
    }
}

/**
 * 주간 목표 칩. "요일대로"(목표 없음)와 주 1~7회 중 하나를 고른다.
 *
 * 활동 요일 수보다 큰 값은 채울 수 없으므로 고를 수 없게 흐려 둔다.
 */
@Composable
private fun WeeklyGoalChips(
    modifier: Modifier = Modifier,
    activeDays: Int,
    weeklyGoal: Int,
    onWeeklyGoalChanged: (Int) -> Unit,
) {
    val maxSelectable = activeDayCount(activeDays).coerceAtMost(MAX_WEEKLY_GOAL)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GoalChip(
            modifier = Modifier.weight(1.8f),
            label = stringResource(id = R.string.detail_week_goal_none_short),
            selected = weeklyGoal <= NO_WEEKLY_GOAL,
            enabled = true,
            onClick = { onWeeklyGoalChanged(NO_WEEKLY_GOAL) },
        )
        (1..MAX_WEEKLY_GOAL).forEach { times ->
            GoalChip(
                modifier = Modifier.weight(1f),
                label = "$times",
                selected = weeklyGoal == times,
                enabled = times <= maxSelectable,
                onClick = { onWeeklyGoalChanged(times) },
            )
        }
    }
}

@Composable
private fun GoalChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isSelected = selected
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(
                when {
                    selected -> AppColor.primary
                    enabled -> AppColor.surfaceContainer
                    else -> AppColor.surfaceContainer.copy(alpha = 0.4f)
                }
            )
            .clickable(enabled = enabled && !selected, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = isSelected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = AppTypography.labelMedium.copy(
                color = when {
                    selected -> AppColor.onPrimary
                    enabled -> AppColor.onSurface.copy(alpha = 0.7f)
                    else -> AppColor.onSurface.copy(alpha = 0.25f)
                },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

/**
 * 저장을 막고 있는 이유. 저장할 수 있는 상태라면 null.
 *
 * 시각 스테퍼는 너무 짧은 구간을 지나갈 수 있게 두고(자정을 넘기는 루프를 만들 수 있어야 한다),
 * 막는 것은 저장 버튼뿐이다.
 */
@Composable
private fun saveBlockerMessage(
    draft: LoopDraft,
    loopsAtSameTime: Int,
): String? = when {
    draft.title.isBlank() -> stringResource(id = R.string.detail_editor_name_required)

    !draft.isAnyTime && isLoopDurationTooShort(draft.startInDay, draft.endInDay) -> stringResource(
        id = R.string.warning_end_time_should_be_after_start_time,
        MIN_DIFF_MINUTES,
    )

    loopsAtSameTime > MAX_LOOPS_TOGETHER ->
        stringResource(id = R.string.warning_up_to_max_loops, MAX_LOOPS_TOGETHER)

    else -> null
}

/** 저장을 막고 있는 이유를 말해 주는 줄. */
@Composable
private fun EditorWarning(
    modifier: Modifier = Modifier,
    text: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(AppColor.error.copy(alpha = 0.10f))
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.WarningAmber,
            tint = AppColor.error,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text,
            style = AppTypography.bodySmall.copy(color = AppColor.error),
        )
    }
}
