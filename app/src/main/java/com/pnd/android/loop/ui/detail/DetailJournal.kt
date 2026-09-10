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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
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
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.toLocalDate
import kotlinx.coroutines.CancellationException
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
 * 월간 달력과 메모 목록은 기록을 찾는 영역, 하단 편집창은 선택한 하루를 고치는 영역이다.
 * 날짜 선택·오늘 기록·메모 선택이 모두 같은 편집창으로 연결되고, 닫을 때도 초안을 보존한다.
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
    var showDaySheet by rememberSaveable { mutableStateOf(false) }
    var memoLoadFailed by rememberSaveable(selectedDate) { mutableStateOf(false) }
    var memoLoadAttempt by rememberSaveable { mutableIntStateOf(0) }

    // 메모 초안은 날짜별로 살아 있어야 한다. 회전해도 유지되도록 저장 가능한 상태로 둔다.
    var memo by rememberSaveable(selectedDate) { mutableStateOf("") }
    var savedMemo by rememberSaveable(selectedDate) { mutableStateOf<String?>(null) }
    val isLoaded = savedMemo != null
    val isDirty = isLoaded && memo != savedMemo

    LaunchedEffect(selectedDate, showDaySheet, memoLoadAttempt) {
        if (showDaySheet && savedMemo == null) {
            memoLoadFailed = false
            try {
                val loaded = onLoadMemo(selectedDate) ?: ""
                savedMemo = loaded
                memo = loaded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                memoLoadFailed = true
            }
        }
    }

    // 편집창 닫기·날짜 이동·섹션 접기에서 기존 자동 저장 동작을 유지한다.
    fun flushDraft() {
        if (isDirty) {
            onSaveMemoInBackground(selectedDate, memo)
            savedMemo = memo
            feedback.show(R.string.detail_saved_memo)
        }
    }

    fun openDay(date: LocalDate) {
        if (date.isBefore(createdDate) || date.isAfter(today)) return
        if (date != selectedDate) {
            flushDraft()
            selectedDate = date
        }
        visibleMonth = YearMonth.from(date)
        showDaySheet = true
    }

    fun closeDay() {
        flushDraft()
        showDaySheet = false
    }

    // 섹션을 접거나 화면을 벗어날 때도 같은 규칙으로 초안을 지킨다.
    val latestDirty by rememberUpdatedState(isDirty)
    val latestMemo by rememberUpdatedState(memo)
    val latestDate by rememberUpdatedState(selectedDate)
    val latestSaveInBackground by rememberUpdatedState(onSaveMemoInBackground)
    DisposableEffect(Unit) {
        onDispose {
            if (latestDirty) latestSaveInBackground(latestDate, latestMemo)
        }
    }

    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.CalendarMonth,
        title = stringResource(id = R.string.detail_records_and_memos),
        summary = stringResource(id = R.string.detail_memo_count, memos.size),
        expanded = expanded,
        onExpandedChange = { next ->
            if (!next) closeDay()
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
            modifier = Modifier.padding(top = DetailSpacing.headerToContent),
            visibleMonth = visibleMonth,
            doneStateByDate = stats.doneStateByDate,
            memoDates = stats.memoDates,
            createdDate = createdDate,
            today = today,
            selectedDate = selectedDate,
            accent = accent,
            onSelect = ::openDay,
        )

        CalendarLegend(modifier = Modifier.padding(top = DetailSpacing.item))
        TodayJournalRow(
            modifier = Modifier.padding(top = DetailSpacing.group),
            today = today,
            state = stats.doneStateByDate[today],
            enabled = !today.isBefore(createdDate),
            onClick = { openDay(today) },
        )
        Text(
            modifier = Modifier.fillMaxWidth().padding(top = DetailSpacing.related),
            text = stringResource(R.string.detail_calendar_edit_hint),
            textAlign = TextAlign.Center,
            style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
        )

        if (memos.isNotEmpty()) {
            MemoListToggle(
                modifier = Modifier.padding(top = DetailSpacing.group),
                count = memos.size,
                expanded = showAllMemos,
                onToggle = { showAllMemos = !showAllMemos },
            )
            AnimatedVisibility(visible = showAllMemos) {
                MemoList(
                    modifier = Modifier.padding(top = DetailSpacing.item),
                    memos = memos,
                    onSelect = ::openDay,
                )
            }
        }
    }

    if (showDaySheet && expanded) {
        JournalDaySheet(
            date = selectedDate,
            doneState = stats.doneStateByDate[selectedDate],
            editable = !selectedDate.isAfter(today) && !selectedDate.isBefore(createdDate),
            memo = memo,
            memoLoaded = isLoaded,
            memoLoadFailed = memoLoadFailed,
            memoDirty = isDirty,
            accent = accent,
            onRetryLoad = { memoLoadAttempt++ },
            onMemoChange = { memo = it },
            onSaveMemo = {
                // 저장 중 날짜나 초안이 바뀌더라도 실제로 저장한 값만 반영한다.
                val dateToSave = selectedDate
                val textToSave = memo
                onSaveMemo(dateToSave, textToSave)
                if (selectedDate == dateToSave) savedMemo = textToSave
            },
            onSetDoneState = { state -> onSetDoneState(selectedDate, state) },
            onDismiss = ::closeDay,
        )
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
        if (state == DoneState.DONE || state == DoneState.SKIP) {
            Text(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .clearAndSetSemantics { },
                text = if (state == DoneState.DONE) "✓" else "−",
                style = AppTypography.labelSmall.copy(
                    color = textColor,
                    fontSize = 10.sp,
                    letterSpacing = 0.sp,
                ),
            )
        }
    }
}

/** 달력 기호와 의미를 함께 보여 준다. */
@Composable
private fun CalendarLegend(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = stringResource(R.string.detail_calendar_legend),
        textAlign = TextAlign.Center,
        style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
    )
}

@Composable
private fun TodayJournalRow(
    modifier: Modifier = Modifier,
    today: LocalDate,
    state: Int?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .clip(RoundShapes.large)
            .background(AppColor.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                text = stringResource(R.string.detail_today) + " · " + today.formatMonthDateDay(),
                style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
            )
            Text(
                text = stringResource(when (state) {
                    DoneState.DONE -> R.string.done
                    DoneState.SKIP -> R.string.skip
                    else -> R.string.detail_day_no_record
                }),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.65f)),
            )
        }
        TextActionButton(
            text = stringResource(R.string.detail_record_today),
            enabled = enabled,
            onClick = onClick,
        )
    }
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

/** 메모를 최신 순으로 보여 준다. 한 줄을 누르면 그 날짜의 편집창을 연다. */
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
