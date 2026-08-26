package com.pnd.android.loop.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.toLocalDate
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

// ─────────────────────────────────────────────────────────────────────────────
// 기록 · 회고 섹션
// ─────────────────────────────────────────────────────────────────────────────

/** [LocalDate] 를 화면 회전·프로세스 복구 뒤에도 그대로 되살리기 위한 저장기. */
private val LocalDateSaver = Saver<LocalDate, Long>(
    save = { it.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) },
)

private val YearMonthSaver = Saver<YearMonth, Long>(
    save = { it.year * 12L + (it.monthValue - 1) },
    restore = { YearMonth.of((it / 12).toInt(), (it % 12).toInt() + 1) },
)

/**
 * 한 달 달력을 중심에 두고, 날짜를 누르면 그날의 상태와 회고 메모를 보고 **고칠 수 있는** 섹션.
 * 완료한 날은 잔디처럼 옅은 강조색으로 칠하고, 메모가 있는 날은 우상단에 점 마커를 얹는다.
 *
 * 예전에는 여기서 상태를 읽기만 할 수 있어, 어제 깜빡한 완료를 이 화면에서는 고칠 방법이 없었다.
 * 회고 메모도 저장 버튼을 누르지 않고 다른 날짜로 옮기면 조용히 사라졌다 — 지금은 옮기기 전에
 * 자동으로 저장하고 그 사실을 알린다.
 */
@Composable
internal fun JournalSection(
    modifier: Modifier = Modifier,
    stats: DetailStats,
    memos: List<LoopRetrospectVo>,
    accent: Color,
    feedback: DetailFeedback,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLoadMemo: suspend (LocalDate) -> String?,
    onSaveMemo: suspend (LocalDate, String) -> Unit,
    onSaveMemoInBackground: (LocalDate, String) -> Unit,
    onSetDoneState: suspend (LocalDate, Int) -> Unit,
) {
    val today = stats.today
    val createdDate = stats.createdDate

    var selectedDate by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(today) }
    var visibleMonth by rememberSaveable(stateSaver = YearMonthSaver) {
        mutableStateOf(YearMonth.from(today))
    }
    var showAllMemos by rememberSaveable { mutableStateOf(false) }

    // 메모 초안은 날짜별로 살아 있어야 한다. 회전해도 유지되도록 저장 가능한 상태로 둔다.
    var memo by rememberSaveable(selectedDate) { mutableStateOf("") }
    var savedMemo by rememberSaveable(selectedDate) { mutableStateOf<String?>(null) }
    val isLoaded = savedMemo != null
    val isDirty = isLoaded && memo != savedMemo

    LaunchedEffect(selectedDate) {
        if (savedMemo == null) {
            val loaded = onLoadMemo(selectedDate) ?: ""
            savedMemo = loaded
            memo = loaded
        }
    }

    // 날짜를 옮기기 전에 초안을 저장한다. 옮긴 뒤에는 되돌릴 방법이 없으므로, 묻기보다
    // 저장하고 알리는 쪽을 택했다.
    fun flushDraft() {
        if (isDirty) {
            onSaveMemoInBackground(selectedDate, memo)
            savedMemo = memo
            feedback.show(R.string.detail_saved_memo)
        }
    }

    // 섹션을 접거나 화면을 벗어날 때도 같은 규칙으로 초안을 지킨다.
    val latestDirty by rememberUpdatedState(isDirty)
    val latestMemo by rememberUpdatedState(memo)
    val latestDate by rememberUpdatedState(selectedDate)
    DisposableEffect(Unit) {
        onDispose {
            if (latestDirty) onSaveMemoInBackground(latestDate, latestMemo)
        }
    }

    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.CalendarMonth,
        title = stringResource(id = R.string.daily_record),
        summary = stringResource(id = R.string.detail_memo_count, memos.size),
        expanded = expanded,
        onExpandedChange = { next ->
            if (!next) flushDraft()
            onExpandedChange(next)
        },
    ) {
        MonthNavigator(
            visibleMonth = visibleMonth,
            canGoPrev = visibleMonth.isAfter(YearMonth.from(createdDate)),
            canGoNext = visibleMonth.isBefore(YearMonth.from(today)),
            onPrev = { visibleMonth = visibleMonth.minusMonths(1) },
            onNext = { visibleMonth = visibleMonth.plusMonths(1) },
        )

        MonthCalendar(
            modifier = Modifier.padding(top = 12.dp),
            visibleMonth = visibleMonth,
            doneStateByDate = stats.doneStateByDate,
            memoDates = stats.memoDates,
            createdDate = createdDate,
            today = today,
            selectedDate = selectedDate,
            accent = accent,
            onSelect = { date ->
                if (date != selectedDate) {
                    flushDraft()
                    selectedDate = date
                }
            },
        )

        SelectedDayPanel(
            modifier = Modifier.padding(top = 18.dp),
            selectedDate = selectedDate,
            doneState = stats.doneStateByDate[selectedDate],
            editable = !selectedDate.isAfter(today) && !selectedDate.isBefore(createdDate),
            memo = memo,
            onMemoChange = { memo = it },
            memoSaveEnabled = isLoaded && isDirty,
            accent = accent,
            feedback = feedback,
            onSaveMemo = {
                onSaveMemo(selectedDate, memo)
                savedMemo = memo
            },
            onSetDoneState = { state -> onSetDoneState(selectedDate, state) },
        )

        if (memos.isNotEmpty()) {
            MemoListToggle(
                modifier = Modifier.padding(top = 18.dp),
                count = memos.size,
                expanded = showAllMemos,
                onToggle = { showAllMemos = !showAllMemos },
            )
            AnimatedVisibility(visible = showAllMemos) {
                MemoList(
                    modifier = Modifier.padding(top = 8.dp),
                    memos = memos,
                    onSelect = { date ->
                        if (date != selectedDate) {
                            flushDraft()
                            selectedDate = date
                        }
                        visibleMonth = YearMonth.from(date)
                    },
                )
            }
        }
    }
}

/** 이전/다음 달로 이동하는 헤더. 범위(생성월 ~ 이번 달)를 벗어나는 화살표는 흐리게 비활성화한다. */
@Composable
private fun MonthNavigator(
    modifier: Modifier = Modifier,
    visibleMonth: YearMonth,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavArrowButton(
            icon = Icons.Outlined.ChevronLeft,
            contentDescription = stringResource(id = R.string.detail_prev_month),
            enabled = canGoPrev,
            onClick = onPrev,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = visibleMonth.atDay(1).formatYearMonth(),
            textAlign = TextAlign.Center,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        NavArrowButton(
            icon = Icons.Outlined.ChevronRight,
            contentDescription = stringResource(id = R.string.detail_next_month),
            enabled = canGoNext,
            onClick = onNext,
        )
    }
}

/** 일요일 시작 한 달 달력 그리드. 완료 히트 배경·오늘/선택 강조·메모 마커를 셀마다 그린다. */
@Composable
private fun MonthCalendar(
    modifier: Modifier = Modifier,
    visibleMonth: YearMonth,
    doneStateByDate: Map<LocalDate, Int>,
    memoDates: Set<LocalDate>,
    createdDate: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate,
    accent: Color,
    onSelect: (LocalDate) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 요일 헤더(일 ~ 토). 주말은 요일 색을 옅게 입힌다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 헤더 일곱 글자는 달력 격자를 읽기 위한 눈금이지, 따로 읽을 정보가 아니다.
                .clearAndSetSemantics { },
        ) {
            DAYS_WITH_3CHARS_SUNDAY_FIRST.forEachIndexed { index, dayResId ->
                val dayOfWeek = DayOfWeek.of(if (index == 0) 7 else index)
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = dayResId),
                    style = AppTypography.labelSmall.copy(
                        color = dayOfWeek.color().copy(alpha = 0.7f),
                    ),
                )
            }
        }

        // 1일이 놓일 위치까지의 빈 칸 수(일요일 시작 기준)와 필요한 주(행) 수를 계산한다.
        val leadingBlanks = visibleMonth.atDay(1).dayOfWeek.value % 7
        val lengthOfMonth = visibleMonth.lengthOfMonth()
        val rows = ceil((leadingBlanks + lengthOfMonth) / 7f).toInt()

        var cellIndex = 0
        Column(modifier = Modifier.padding(top = 8.dp)) {
            repeat(rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 칸 폭은 화면 너비에 묶여 있어 48dp 를 줄 수 없지만, 높이만은 지킨다.
                        .height(MinTouchTarget),
                ) {
                    repeat(7) {
                        val dayOfMonth = cellIndex - leadingBlanks + 1
                        if (dayOfMonth in 1..lengthOfMonth) {
                            val date = visibleMonth.atDay(dayOfMonth)
                            CalendarDayCell(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                date = date,
                                state = doneStateByDate[date],
                                hasMemo = date in memoDates,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                // 미래·생성 이전 날짜는 선택할 수 없다.
                                selectable = !date.isAfter(today) && !date.isBefore(createdDate),
                                accent = accent,
                                onSelect = onSelect,
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                        cellIndex++
                    }
                }
            }
        }
    }
}

/** 달력의 하루 칸: 완료 히트 배경 + 날짜 숫자 + (선택 시)외곽 링 + (메모 시)점 마커. */
@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    state: Int?,
    hasMemo: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    selectable: Boolean,
    accent: Color,
    onSelect: (LocalDate) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    // 완료일은 잔디처럼 옅은 강조색으로 칠하고, 건너뛴 날은 아주 옅은 회색으로 표시한다.
    // 다크 모드는 배경이 어두워 같은 알파라도 옅게 보이므로 조금 더 진하게 칠한다.
    val background = when (state) {
        DoneState.DONE -> accent.copy(alpha = if (isDark) 0.40f else 0.22f)
        DoneState.SKIP -> AppColor.onSurface.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val textColor = when {
        !selectable -> AppColor.onSurface.copy(alpha = 0.3f)
        state == DoneState.DONE -> accent
        else -> AppColor.onSurface
    }

    // 숫자만 읽히면 완료·건너뜀·메모가 색과 점으로만 남는다. 한 문장으로 풀어 준다.
    val dateLabel = date.formatMonthDateDay()
    val stateLabel = when (state) {
        DoneState.DONE -> stringResource(id = R.string.done)
        DoneState.SKIP -> stringResource(id = R.string.skip)
        else -> stringResource(id = R.string.detail_day_no_record)
    }
    val memoLabel = if (hasMemo) stringResource(id = R.string.detail_has_memo) else null
    val todayLabel = if (isToday) stringResource(id = R.string.detail_today) else null
    val cellDescription = listOfNotNull(dateLabel, todayLabel, stateLabel, memoLabel)
        .joinToString(", ")
    val isCellSelected = isSelected

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                // 선택된 날은 면이 아니라 외곽 링으로 표시해 히트 배경과 겹치지 않게 한다.
                if (isSelected) {
                    Modifier.border(width = 1.5.dp, color = accent, shape = CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = selectable) { onSelect(date) }
            .clearAndSetSemantics {
                if (selectable) {
                    this.contentDescription = cellDescription
                    this.role = Role.Button
                    this.selected = isCellSelected
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${date.dayOfMonth}",
            style = AppTypography.bodySmall.copy(
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            ),
        )

        if (hasMemo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(AppColor.onSurface.copy(alpha = 0.55f)),
            )
        }
    }
}

/**
 * 달력에서 고른 날짜의 상태와 그날의 회고 메모를 다루는 패널.
 *
 * 상태는 읽기만 하던 알약에서 고칠 수 있는 세 칸으로 바꿨다. 어제 깜빡한 완료를 고치려고
 * 홈으로 돌아갈 필요가 없어졌고, 실수로 누른 건너뜀도 여기서 되돌린다.
 */
@Composable
private fun SelectedDayPanel(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    doneState: Int?,
    editable: Boolean,
    memo: String,
    onMemoChange: (String) -> Unit,
    memoSaveEnabled: Boolean,
    accent: Color,
    feedback: DetailFeedback,
    onSaveMemo: suspend () -> Unit,
    onSetDoneState: suspend (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = selectedDate.formatMonthDateDay(),
            style = AppTypography.titleSmall.copy(color = AppColor.onSurface),
        )

        if (editable) {
            DayStateSelector(
                modifier = Modifier.padding(top = 10.dp),
                doneState = doneState,
                accent = accent,
                onSelect = { state ->
                    scope.launch {
                        onSetDoneState(state)
                        feedback.show(R.string.detail_saved_day_state)
                    }
                },
            )
        } else {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(id = R.string.detail_day_not_editable),
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }

        JournalMemoField(
            modifier = Modifier.padding(top = 14.dp),
            value = memo,
            onValueChange = onMemoChange,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.retrospect_char_count, memo.length),
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            PrimaryPillButton(
                enabled = memoSaveEnabled,
                text = stringResource(id = R.string.save),
                onClick = {
                    scope.launch {
                        onSaveMemo()
                        feedback.show(R.string.detail_saved_memo)
                    }
                },
            )
        }
    }
}

/**
 * 선택한 날의 상태를 고르는 세 칸(완료 · 건너뜀 · 기록 없음).
 * 이미 골라 둔 칸을 다시 눌러도 같은 값을 저장하지 않는다.
 */
@Composable
private fun DayStateSelector(
    modifier: Modifier = Modifier,
    doneState: Int?,
    accent: Color,
    onSelect: (Int) -> Unit,
) {
    val current = doneState ?: DoneState.NO_RESPONSE

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DayStateChip(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.done),
            selected = current == DoneState.DONE,
            selectedColor = accent,
            onClick = { onSelect(DoneState.DONE) },
        )
        DayStateChip(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.skip),
            selected = current == DoneState.SKIP,
            selectedColor = AppColor.onSurface.copy(alpha = 0.6f),
            onClick = { onSelect(DoneState.SKIP) },
        )
        DayStateChip(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.detail_day_no_record),
            selected = current == DoneState.NO_RESPONSE,
            selectedColor = AppColor.onSurface.copy(alpha = 0.45f),
            onClick = { onSelect(DoneState.NO_RESPONSE) },
        )
    }
}

@Composable
private fun DayStateChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    val isSelected = selected
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundShapes.medium)
            .background(
                if (selected) selectedColor.copy(alpha = 0.16f) else AppColor.surfaceContainer
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        color = selectedColor.copy(alpha = 0.55f),
                        shape = RoundShapes.medium,
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !selected, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = isSelected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = AppTypography.labelMedium.copy(
                color = if (selected) selectedColor else AppColor.onSurface.copy(alpha = 0.55f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

/** 회고 입력창. 홈 화면의 회고 입력과 같은 톤이되, 스크롤 중 키보드가 튀지 않도록 자동 포커스는 두지 않는다. */
@Composable
private fun JournalMemoField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.12f),
                shape = RoundShapes.medium,
            )
            .padding(all = 14.dp),
        value = value,
        onValueChange = onValueChange,
        cursorBrush = SolidColor(AppColor.primary),
        textStyle = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface,
            lineHeight = 20.sp,
        ),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.retrospect_hint),
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.4f),
                    ),
                )
            }
            innerTextField()
        },
    )
}

/**
 * 남긴 메모를 한자리에서 훑는 목록의 여닫이.
 *
 * 접힌 행이 "메모 12개"라고 세어 주면서도 그 12개를 한 번에 볼 방법이 없어, 날짜를 하나씩
 * 눌러 가며 찾아야 했다.
 */
@Composable
private fun MemoListToggle(
    modifier: Modifier = Modifier,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    TextActionButton(
        modifier = modifier,
        text = if (expanded) {
            stringResource(id = R.string.detail_hide_all_memos)
        } else {
            stringResource(id = R.string.detail_show_all_memos, count)
        },
        onClick = onToggle,
    )
}

/** 메모를 남긴 날짜와 본문을 최신 순으로. 한 줄을 누르면 그 날짜가 달력에서 선택된다. */
@Composable
private fun MemoList(
    modifier: Modifier = Modifier,
    memos: List<LoopRetrospectVo>,
    onSelect: (LocalDate) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        memos.forEach { memo ->
            val date = memo.date.toLocalDate()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundShapes.medium)
                    .clickable(role = Role.Button) { onSelect(date) }
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            ) {
                Text(
                    text = date.formatMonthDateDay(),
                    style = AppTypography.labelMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.5f),
                    ),
                )
                Text(
                    modifier = Modifier.padding(top = 3.dp),
                    text = memo.text.orEmpty(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
                )
            }
        }
    }
}
