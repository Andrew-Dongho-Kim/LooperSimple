package com.pnd.android.loop.ui.detail

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.data.common.MAX_LOOPS_TOGETHER
import com.pnd.android.loop.data.common.NO_REPEAT
import com.pnd.android.loop.ui.common.SimpleAd
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.common.rememberScrollCollapseProgress
import com.pnd.android.loop.ui.home.DeleteLoopDialog
import com.pnd.android.loop.ui.home.input.selector.MIN_DIFF_MINUTES
import com.pnd.android.loop.ui.home.input.selector.StartEndTimeSelector
import com.pnd.android.loop.ui.home.input.selector.isLoopDurationTooShort
import com.pnd.android.loop.ui.statisctics.computeStreak
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
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.toLocalDate
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.roundToInt

private val adId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-2341430172816266/5981213088"
}

/** Inner padding shared by every card on the detail screen. */
private val CardPadding = 20.dp

/** Vertical gap between rows inside a card (info rows, header → content). */
private val CardInnerSpacing = 16.dp

/** Vertical padding of one collapsible section row. */
private val SectionRowPadding = 15.dp

/** Gap between two stat tiles, horizontally and vertically. */
private val TileSpacing = 10.dp

/**
 * 앱바가 요약 헤더로부터 제목을 이어받는 스크롤 거리. 헤더 제목 줄 한 줄 높이(titleLarge + 여백)에
 * 맞춰 두어, 제목이 앱바 뒤로 사라질 무렵 앱바 타이틀이 완전히 드러난다.
 */
private val TitleHandoverDistance = 44.dp


@Composable
fun DetailPage(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val loop by detailViewModel.loop.collectAsState(LoopVo.create())
    // 시간 저장 실패(너무 짧은 구간 / 같은 시간대 과밀)를 알리는 스낵바. 홈의 추가 UX와 같은 문구를 쓴다.
    val snackBarHostState = remember { SnackbarHostState() }

    // 앱바 타이틀과 요약 헤더의 큰 제목이 같은 이름이라, 진입 화면에서 이름이 두 번 보였다.
    // 앱바 타이틀은 숨겨 두고, 헤더 제목이 스크롤로 앱바 뒤로 사라지는 만큼만 나타나게 한다.
    val scrollState = rememberScrollState()
    val titleAlpha by rememberScrollCollapseProgress(
        scrollState = scrollState,
        collapseDistance = TitleHandoverDistance,
    )

    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.background),
        topBar = {
            // 활성화 토글은 앱바에서 내려 본문 최상단 요약 헤더로 옮겼다. 앱바에서 가장 누르기 쉬운
            // 자리에 있던 탓에, 기록을 멈추는 되돌리기 어려운 동작이 실수로 눌리기 쉬웠다.
            SimpleAppBar(
                modifier = Modifier
                    .statusBarsPadding(),
                title = loop.title,
                onNavigateUp = onNavigateUp,
                titleAlpha = titleAlpha,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    )
    { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            DetailPageContent(
                detailViewModel = detailViewModel,
                loop = loop,
                scrollState = scrollState,
                snackBarHostState = snackBarHostState,
                onLoopDeleted = onNavigateUp,
            )
        }
    }
}

/**
 * 상세 화면 본문. 화면은 두 층으로만 나뉜다.
 *
 * 1. [SummaryHeader] — 카드 테두리 없이 배경 위에 바로 놓이는 요약. 이름·활성화 상태와 핵심 지표
 *    셋(완료율·연속·이번 주), 최근 7일 흐름까지가 여기에 들어가 진입 즉시 스크롤 없이 읽힌다.
 * 2. [SectionList] — 스케줄 / 통계 / 기록 / 삭제를 한 장의 카드 안에 접이식 행으로 담는다.
 *    접힌 행에도 오른쪽에 값이 남아 있어 펼치지 않아도 정보가 사라지지 않는다.
 *
 * 이전에는 같은 정보를 카드 8장에 나눠 담았고, 그중 넷이 "완료율이 오르는가"를 서로 다른 방식으로
 * 반복해 스크롤만 길어졌다. 반복되던 추세 카드 셋은 통계 섹션 안의 타일 넷으로 합쳤다.
 */
@Composable
private fun DetailPageContent(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    scrollState: ScrollState,
    snackBarHostState: SnackbarHostState,
    onLoopDeleted: () -> Unit,
) {
    // 요약 헤더와 통계·기록 섹션이 같은 응답 기록을 공유하므로 한 번만 구독해 아래로 내려준다.
    val responses by detailViewModel.allResponses.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .padding(horizontal = Dimens.screenHorizontalPadding)
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        verticalArrangement = Arrangement.spacedBy(Dimens.contentPadding),
    ) {
        SummaryHeader(
            detailViewModel = detailViewModel,
            loop = loop,
            responses = responses,
        )

        SectionList(
            detailViewModel = detailViewModel,
            loop = loop,
            responses = responses,
            snackBarHostState = snackBarHostState,
            onLoopDeleted = onLoopDeleted,
        )

        SimpleAd(adId = adId)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. 요약 헤더 — 진입 즉시 보이는 것
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 화면 맨 위 요약. 카드로 감싸지 않아 "이 화면 전체의 머리말"로 읽히고, 아래 섹션 카드와 층이 갈린다.
 *
 * 이름은 연필을 눌러 그 자리에서 고치고, 활성화 토글은 이름과 같은 줄에 둔다. 꺼져 있을 때만
 * "기록하지 않음" 설명을 덧붙인다 — 켜져 있을 때는 스위치 모양만으로 충분하고, 한 줄이 절약된다.
 */
@Composable
private fun SummaryHeader(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    responses: List<LoopDoneVo>,
) {
    val accent = Color(loop.color).compositeOverOnSurface()
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }

    val total by detailViewModel.allEnabledCount.collectAsState(initial = 0)
    val doneCount by detailViewModel.doneCount.collectAsState(initial = 0)
    val donePercent = if (total == 0) 0 else (doneCount.toFloat() / total * 100).roundToInt()

    // 완료 상태를 날짜로 인덱싱해 두면 스트릭·주간 요약이 모두 빠르게 조회할 수 있다.
    val doneStateByDate = remember(responses) {
        responses.associate { it.date.toLocalDate() to it.done }
    }
    val streak = remember(responses) {
        computeStreak(doneDates = responses.filter { it.isDone() }.map { it.date.toLocalDate() })
    }
    val weekly = remember(doneStateByDate, loop.activeDays, createdDate) {
        computeWeeklyProgress(
            doneStateByDate = doneStateByDate,
            activeDays = loop.activeDays,
            createdDate = createdDate,
        )
    }

    var isEditing by remember(loop.loopId) { mutableStateOf(false) }
    // 편집 중 저장된 값이 되돌아오면 초안도 함께 갱신된다(저장 직후 편집 모드는 어차피 닫힌다).
    var draftTitle by remember(loop.loopId, loop.title) { mutableStateOf(loop.title) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isEditing) {
            TitleEditor(
                accent = accent,
                value = draftTitle,
                onValueChange = { draftTitle = it },
                onCancel = {
                    draftTitle = loop.title
                    isEditing = false
                },
                onSave = {
                    onTitleSaved(detailViewModel = detailViewModel, loop = loop, title = draftTitle)
                    isEditing = false
                },
            )
        } else {
            TitleRow(
                loop = loop,
                accent = accent,
                onEdit = { isEditing = true },
                onEnabledChanged = { enabled -> detailViewModel.enableLoop(loop, enabled) },
            )
        }

        KpiRow(
            modifier = Modifier.padding(top = 20.dp),
            donePercent = donePercent,
            streakDays = streak.current,
            bestStreakDays = streak.longest,
            weekly = weekly,
        )

        // 최근 7일 흐름(왼→오: 6일 전 → 오늘)과 지난주 대비 한 줄 피드백.
        WeekDotsRow(
            modifier = Modifier.padding(top = 20.dp),
            doneStateByDate = doneStateByDate,
            createdDate = createdDate,
            activeDays = loop.activeDays,
            accent = accent,
        )
        WeeklyTrendCaption(
            modifier = Modifier.padding(top = 8.dp),
            trend = weekly.trend,
        )
    }
}

private fun onTitleSaved(
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    title: String,
) {
    detailViewModel.updateLoop(loop.copyAs(title = title.trim()))
}

/** 색 점 · 이름(누르면 인라인 편집) · 활성화 토글을 한 줄에 담는 헤더 첫 줄. */
@Composable
private fun TitleRow(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    accent: Color,
    onEdit: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundShapes.medium)
                    .clickable(onClick = onEdit)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f, fill = false),
                    text = loop.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.titleLarge.copy(
                        color = AppColor.onSurface,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Icon(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(16.dp),
                    imageVector = Icons.Outlined.ModeEdit,
                    tint = AppColor.primary,
                    contentDescription = stringResource(id = R.string.detail_edit_name),
                )
            }
        }

        ActiveStateToggle(
            modifier = Modifier.padding(top = 14.dp),
            enabled = loop.enabled,
            onEnabledChanged = onEnabledChanged,
        )

        // 꺼져 있을 때만 결과를 설명한다. 켜져 있을 때의 "정상 동작"은 굳이 한 줄을 쓰지 않는다.
        if (!loop.enabled) {
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = stringResource(id = R.string.detail_loop_active_off),
                style = AppTypography.bodySmall.copy(
                    color = AppColor.error.copy(alpha = 0.8f),
                ),
            )
        }
    }
}

/**
 * 활성 / 비활성 2분할 토글. 라벨 없는 스위치를 대신한다.
 *
 * 스위치는 켜짐·꺼짐이 트랙 색만으로 구분되는데, 이 앱의 스위치 색은 두 상태가 모두 무채색이라
 * 상세 화면에서 "지금 어느 쪽인지"가 한눈에 읽히지 않았다. 두 칸 중 하나가 채워지는 형태로 바꾸면
 * 현재 상태(채워진 칸)와 바꾸는 방법(반대 칸을 누른다)이 글자로 함께 드러난다.
 * 이미 선택된 칸은 눌러도 아무 일이 없어(clickable 비활성) 같은 값을 다시 저장하지 않는다.
 */
@Composable
private fun ActiveStateToggle(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.large)
            .background(AppColor.surfaceContainer)
            .padding(all = 3.dp),
    ) {
        ActiveStateSegment(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Check,
            label = stringResource(id = R.string.detail_loop_state_active),
            selected = enabled,
            selectedColor = AppColor.primary,
            onClick = { onEnabledChanged(true) },
        )
        ActiveStateSegment(
            modifier = Modifier.weight(1f),
            // 활성 ✓ / 비활성 ✕ 짝. 홈 카드 스위치의 손잡이 아이콘과 같은 짝이라 두 화면이 어긋나지 않는다.
            icon = Icons.Outlined.Close,
            label = stringResource(id = R.string.detail_loop_state_inactive),
            selected = !enabled,
            selectedColor = AppColor.error,
            onClick = { onEnabledChanged(false) },
        )
    }
}

/** [ActiveStateToggle]의 한 칸. 선택된 칸만 표면을 한 층 띄우고 색·굵기를 입힌다. */
@Composable
private fun ActiveStateSegment(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) selectedColor else AppColor.onSurface.copy(alpha = 0.45f)
    Row(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(if (selected) AppColor.surfaceElevated else Color.Transparent)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 0.5.dp,
                        color = selectedColor.copy(alpha = 0.35f),
                        shape = RoundShapes.medium,
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !selected, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            tint = contentColor,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = label,
            style = AppTypography.labelLarge.copy(
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

/**
 * 핵심 지표 세 칸: 완료율 · 연속 · 이번 주. 세로 실선으로만 나눠 카드 없이도 한 묶음으로 읽힌다.
 * 최고 기록은 지금 기록을 넘어설 때만 연속 칸 아래에 작게 붙여, 평소에는 세 줄을 넘지 않는다.
 */
@Composable
private fun KpiRow(
    modifier: Modifier = Modifier,
    donePercent: Int,
    streakDays: Int,
    bestStreakDays: Int,
    weekly: WeeklyProgress,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KpiCell(
            modifier = Modifier.weight(1f),
            value = "$donePercent%",
            label = stringResource(id = R.string.detail_kpi_done_rate),
        )
        KpiDivider()
        KpiCell(
            modifier = Modifier.weight(1f),
            value = stringResource(id = R.string.detail_kpi_streak_days, streakDays),
            label = stringResource(id = R.string.detail_kpi_streak),
            caption = if (bestStreakDays > streakDays) {
                stringResource(id = R.string.detail_streak_best, bestStreakDays)
            } else {
                null
            },
        )
        KpiDivider()
        KpiCell(
            modifier = Modifier.weight(1f),
            value = stringResource(
                id = R.string.detail_kpi_week_ratio,
                weekly.doneThisWeek,
                weekly.activeThisWeek,
            ),
            label = stringResource(id = R.string.detail_kpi_this_week),
        )
    }
}

@Composable
private fun KpiCell(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    caption: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            maxLines = 1,
            style = AppTypography.headlineSmall.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            modifier = Modifier.padding(top = 3.dp),
            text = label,
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
        if (caption != null) {
            Text(
                modifier = Modifier.padding(top = 1.dp),
                text = caption,
                style = AppTypography.labelSmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.35f),
                ),
            )
        }
    }
}

@Composable
private fun KpiDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(0.5.dp)
            .background(AppColor.onSurface.copy(alpha = 0.10f)),
    )
}

/** 최근 7일을 작은 알약으로 늘어놓아, 완료(강조색)·건너뜀(옅음)·그 외를 색으로 구분한다. */
@Composable
private fun WeekDotsRow(
    modifier: Modifier = Modifier,
    doneStateByDate: Map<LocalDate, Int>,
    createdDate: LocalDate,
    activeDays: Int,
    accent: Color,
) {
    val today = LocalDate.now()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        (6 downTo 0).forEach { offset ->
            val date = today.minusDays(offset.toLong())
            val fill = when (doneStateByDate[date]) {
                DoneState.DONE -> accent
                DoneState.SKIP -> AppColor.onSurface.copy(alpha = 0.25f)
                else -> AppColor.surfaceContainer
            }
            // 생성 이전이거나 비활성 요일은 흐리게 처리해 "해당 없음"을 구분한다.
            val isActive = !date.isBefore(createdDate) && activeDays.isOn(dayForLoop(date))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .alpha(if (isActive) 1f else 0.4f),
            )
        }
    }
}

/** 7일 스트립 바로 아래 붙는, 지난주 대비 한 줄 피드백. */
@Composable
private fun WeeklyTrendCaption(
    modifier: Modifier = Modifier,
    trend: Int,
) {
    val trendRes = when {
        trend > 0 -> R.string.detail_week_trend_up
        trend < 0 -> R.string.detail_week_trend_down
        else -> R.string.detail_week_trend_same
    }
    Text(
        modifier = modifier,
        text = stringResource(id = trendRes),
        style = AppTypography.bodySmall.copy(
            color = AppColor.onSurface.copy(alpha = 0.5f),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. 접이식 섹션 목록
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 스케줄 / 통계 / 기록 / 삭제를 담는 한 장의 카드.
 *
 * 순서는 "이 루프가 무엇인지 정하는 것 → 잘 지키고 있는지 → 되돌아보기 → 되돌릴 수 없는 것" 이다.
 * 스케줄이 맨 위인 이유는 루프의 정체성이자 가장 자주 고치는 값이기 때문이고, 삭제가 맨 아래인
 * 이유는 되돌릴 수 없기 때문이다.
 *
 * 기본으로 펼쳐 두는 것은 통계 하나뿐이다. 나머지는 접힌 채로도 오른쪽 요약에 값이 남는다.
 */
@Composable
private fun SectionList(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    responses: List<LoopDoneVo>,
    snackBarHostState: SnackbarHostState,
    onLoopDeleted: () -> Unit,
) {
    // 메모가 있는 날짜 집합. 기록 섹션의 달력 마커와 접힌 행의 "메모 N개" 요약이 함께 쓴다.
    val retrospects by detailViewModel.retrospects.collectAsState(initial = emptyList())
    val memoDates = remember(retrospects) {
        retrospects.filter { !it.text.isNullOrBlank() }.map { it.date.toLocalDate() }.toSet()
    }

    var scheduleExpanded by rememberSaveable(loop.loopId) { mutableStateOf(false) }
    var statsExpanded by rememberSaveable(loop.loopId) { mutableStateOf(true) }
    var journalExpanded by rememberSaveable(loop.loopId) { mutableStateOf(false) }

    DetailCard(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        ScheduleSection(
            detailViewModel = detailViewModel,
            loop = loop,
            snackBarHostState = snackBarHostState,
            expanded = scheduleExpanded,
            onExpandedChange = { scheduleExpanded = it },
        )

        SectionSeparator()

        StatsSection(
            responses = responses,
            loop = loop,
            detailViewModel = detailViewModel,
            expanded = statsExpanded,
            onExpandedChange = { statsExpanded = it },
        )

        SectionSeparator()

        JournalSection(
            detailViewModel = detailViewModel,
            responses = responses,
            memoDates = memoDates,
            loop = loop,
            expanded = journalExpanded,
            onExpandedChange = { journalExpanded = it },
        )

        SectionSeparator()

        DeleteRow(
            loop = loop,
            onDelete = {
                detailViewModel.deleteLoop(loop)
                onLoopDeleted()
            },
        )
    }
}

/**
 * 접이식 섹션 하나. 접힌 상태에서도 [summary] 가 오른쪽에 남아, 펼치지 않고도 값을 읽을 수 있다.
 * "펼쳐야만 알 수 있는 정보"를 만들지 않는 것이 이 목록의 전제다.
 */
@Composable
private fun ExpandableSection(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    summary: String?,
    summaryColor: Color = AppColor.onSurface.copy(alpha = 0.55f),
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(
        start = CardPadding,
        end = CardPadding,
        bottom = CardPadding,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "sectionChevron",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = CardPadding, vertical = SectionRowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = icon,
                tint = AppColor.onSurface.copy(alpha = 0.5f),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = title,
                style = AppTypography.bodyLarge.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (summary != null) {
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                    text = summary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.bodySmall.copy(color = summaryColor),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Icon(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
                    .rotate(chevronRotation),
                imageVector = Icons.Outlined.ExpandMore,
                tint = AppColor.onSurface.copy(alpha = 0.4f),
                contentDescription = stringResource(
                    id = if (expanded) {
                        R.string.detail_collapse_section
                    } else {
                        R.string.detail_expand_section
                    }
                ),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(paddingValues = contentPadding),
                content = content,
            )
        }
    }
}

/** 섹션 사이를 가르는 선. 아이콘 열과 시작점을 맞춰 목록이 한 덩어리로 읽히게 한다. */
@Composable
private fun SectionSeparator() {
    HairlineDivider(modifier = Modifier.padding(start = CardPadding))
}

// ─────────────────────────────────────────────────────────────────────────────
// 2-1. 스케줄 섹션
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 스케줄 섹션. 접힌 상태의 요약이 "오전 7:00 – 오전 7:30 · 주중"처럼 그 자체로 답이 되게 했다.
 * 펼치면 하루 타임라인 · 활동 요일 · 반복 · 생성일이 나오고, 편집은 카드 안에서 그대로 이뤄진다.
 */
@Composable
private fun ScheduleSection(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    snackBarHostState: SnackbarHostState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    var isEditing by remember(loop.loopId) { mutableStateOf(false) }
    val daysLabel = activeDaysLabel(activeDays = loop.activeDays)
    val summary = if (loop.isAnyTime) {
        "${stringResource(id = R.string.anytime)} · $daysLabel"
    } else {
        "${loop.startInDay.formatHourMinute()} – ${loop.endInDay.formatHourMinute()} · $daysLabel"
    }

    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.Schedule,
        title = stringResource(id = R.string.detail_schedule),
        summary = summary,
        expanded = expanded,
        onExpandedChange = {
            // 접을 때는 편집 중이던 초안을 버리고 읽기 모드로 돌아간다.
            if (!it) isEditing = false
            onExpandedChange(it)
        },
        // 시간 선택기는 자체 여백을 갖고 있어, 가로 여백이 두 겹으로 쌓이면 조작 영역이 좁아진다.
        contentPadding = if (isEditing) {
            PaddingValues(start = 8.dp, end = 8.dp, bottom = CardPadding)
        } else {
            PaddingValues(start = CardPadding, end = CardPadding, bottom = CardPadding)
        },
    ) {
        if (isEditing) {
            ScheduleEditor(
                detailViewModel = detailViewModel,
                loop = loop,
                snackBarHostState = snackBarHostState,
                onClose = { isEditing = false },
            )
        } else {
            ScheduleDetails(
                loop = loop,
                onEdit = { isEditing = true },
            )
        }
    }
}

/** 활동 요일을 한 낱말로 줄인다. 매일/주중/주말이 아니면 "주 N일"로 센다. */
@Composable
private fun activeDaysLabel(activeDays: Int): String = when (activeDays) {
    LoopDay.EVERYDAY -> stringResource(id = R.string.everyday)
    LoopDay.WEEKDAYS -> stringResource(id = R.string.weekdays)
    LoopDay.WEEKENDS -> stringResource(id = R.string.weekends)
    else -> stringResource(
        id = R.string.detail_days_per_week,
        LoopDay.ALL.count { activeDays.isOn(it) },
    )
}

/**
 * 스케줄 섹션의 읽기 모드 본문.
 * 하루 중 활동 구간을 24시간 막대 위에 강조해 "언제 하는 습관인지"를 공간적으로 보여준다.
 * '언제든지' 루프는 고정 구간이 없으므로 막대를 그리지 않는다.
 */
@Composable
private fun ScheduleDetails(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onEdit: () -> Unit,
) {
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
    val dayCount = LocalDate.now().toEpochDay() - createdDate.toEpochDay() + 1
    val accent = Color(loop.color).compositeOverOnSurface()

    Column(modifier = modifier.fillMaxWidth()) {
        if (!loop.isAnyTime) {
            val durationMinutes =
                ((loop.endInDay - loop.startInDay) / MS_1MIN).toInt().coerceAtLeast(0)
            val durationText = if (durationMinutes >= 60) {
                stringResource(
                    id = R.string.stat_duration_hm,
                    durationMinutes / 60,
                    durationMinutes % 60,
                )
            } else {
                stringResource(id = R.string.stat_duration_m, durationMinutes)
            }
            Text(
                text = durationText,
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.6f),
                ),
            )

            DayTimeline(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                startFraction = loop.startInDay.toFloat() / MS_1DAY,
                endFraction = loop.endInDay.toFloat() / MS_1DAY,
                accent = accent,
            )
            HourTicks(modifier = Modifier.padding(top = 6.dp))
        }

        ActiveDaysRow(
            modifier = Modifier.padding(top = if (loop.isAnyTime) 0.dp else 18.dp),
            activeDays = loop.activeDays,
        )

        Column(
            modifier = Modifier.padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (loop.interval != NO_REPEAT) {
                InfoRow(
                    icon = Icons.Outlined.Repeat,
                    label = stringResource(id = R.string.detail_repeat),
                    value = intervalString(loop.interval),
                )
            }
            InfoRow(
                icon = Icons.Outlined.DateRange,
                label = stringResource(id = R.string.created_date),
                value = createdDate.formatYearMonthDateDays(),
                trailing = stringResource(id = R.string.n_days, dayCount),
            )
        }

        TextActionButton(
            modifier = Modifier
                .padding(top = 14.dp)
                .align(Alignment.End),
            text = stringResource(id = R.string.detail_edit_schedule),
            onClick = onEdit,
        )
    }
}

/**
 * 스케줄 섹션의 편집 모드. 루프 추가 UX와 같은 [StartEndTimeSelector] 를 섹션 안에 그대로 얹어,
 * 시각·요일·'언제든지'를 홈에서와 동일한 조작으로 고친다.
 *
 * 초안은 저장하기 전까지 DB 에 닿지 않는다. 저장 시점에만 홈의 추가 UX와 같은 규칙(최소 지속 시간,
 * 같은 시간대 최대 개수)으로 검사하고, 걸리면 스낵바로 알리고 편집 모드에 머문다.
 */
@Composable
private fun ScheduleEditor(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    snackBarHostState: SnackbarHostState,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAnyTime by remember(loop) { mutableStateOf(loop.isAnyTime) }
    var startInDay by remember(loop) { mutableStateOf(loop.startInDay) }
    var endInDay by remember(loop) { mutableStateOf(loop.endInDay) }
    var activeDays by remember(loop) { mutableStateOf(loop.activeDays) }

    Column(modifier = modifier.fillMaxWidth()) {
        StartEndTimeSelector(
            isAnyTimeChecked = isAnyTime,
            onIsAnyTimeCheckChanged = { isAnyTime = it },
            selectedStartTime = startInDay,
            onStartTimeSelected = { startInDay = it },
            selectedEndTime = endInDay,
            onEndTimeSelected = { endInDay = it },
            selectedDays = activeDays,
            onSelectedDayChanged = { activeDays = it },
        )

        EditActionRow(
            modifier = Modifier.padding(top = 4.dp, end = CardPadding - 8.dp),
            saveEnabled = true,
            onCancel = onClose,
            onSave = {
                // '언제든지'로 바꾸면 시각은 홈의 추가 UX와 같은 규칙으로 ANY_TIME 으로 비운다.
                val updated = loop.copyAs(
                    isAnyTime = isAnyTime,
                    startInDay = if (isAnyTime) ANY_TIME else startInDay,
                    endInDay = if (isAnyTime) ANY_TIME else endInDay,
                    activeDays = activeDays,
                )
                coroutineScope.launch {
                    if (!ensureSchedule(
                            context = context,
                            detailViewModel = detailViewModel,
                            loop = updated,
                            hostState = snackBarHostState,
                        )
                    ) {
                        return@launch
                    }
                    detailViewModel.updateLoop(updated)
                    onClose()
                }
            },
        )
    }
}

/**
 * 바뀐 시간대를 저장해도 되는지 확인한다. 홈의 추가 UX(ensureLoop)와 같은 규칙·같은 문구를 쓴다.
 * 시각 스테퍼는 너무 짧은 구간을 지나갈 수 있게 두고(자정을 넘기는 루프를 만들 수 있어야 한다),
 * 막는 것은 저장 시점인 여기뿐이다.
 */
private suspend fun ensureSchedule(
    context: Context,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    hostState: SnackbarHostState,
): Boolean {
    if (!loop.isAnyTime && isLoopDurationTooShort(loop.startInDay, loop.endInDay)) {
        hostState.showSnackbar(
            message = context.getString(
                R.string.warning_end_time_should_be_after_start_time,
                MIN_DIFF_MINUTES,
            )
        )
        return false
    }

    if (detailViewModel.numberOfLoopsAtTheSameTime(loop) > MAX_LOOPS_TOGETHER) {
        hostState.showSnackbar(
            message = context.getString(R.string.warning_up_to_max_loops, MAX_LOOPS_TOGETHER)
        )
        return false
    }

    return true
}

// ─────────────────────────────────────────────────────────────────────────────
// 2-2. 통계 섹션 — 한 줄 인사이트 + 2×2 타일
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 통계 섹션. 예전에는 카드 넷(성취 요약 · 응답 요약 · 일별 · 월별 · 요일별)이 세로로 늘어서
 * 화면 두 개 분량을 썼다. 지금은 한 줄 인사이트 아래 반폭 타일 넷으로 접혀 한 화면에 들어간다.
 *
 * 데이터가 없는 타일도 자리를 비우지 않고 "—"로 남긴다. 격자가 들쭉날쭉해지면 오히려 읽기 어렵고,
 * "아직 기록이 없다"는 것도 정보이기 때문이다.
 */
@Composable
private fun StatsSection(
    modifier: Modifier = Modifier,
    responses: List<LoopDoneVo>,
    loop: LoopBase,
    detailViewModel: LoopDetailViewModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
    val dailyTrend = remember(responses, createdDate) {
        computeDailyTrend(responses = responses, createdDate = createdDate)
    }

    val trendTint = when {
        dailyTrend == null -> AppColor.onSurface.copy(alpha = 0.55f)
        dailyTrend.deltaPercent > 0 -> AppColor.primary
        dailyTrend.deltaPercent < 0 -> AppColor.error
        else -> AppColor.onSurface.copy(alpha = 0.55f)
    }

    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.Insights,
        title = stringResource(id = R.string.detail_section_stats),
        summary = dailyTrend?.let { formatDeltaPercent(it.deltaPercent) },
        summaryColor = trendTint,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        if (dailyTrend != null) {
            InsightRow(
                icon = when {
                    dailyTrend.deltaPercent > 0 -> Icons.AutoMirrored.Outlined.TrendingUp
                    dailyTrend.deltaPercent < 0 -> Icons.AutoMirrored.Outlined.TrendingDown
                    else -> Icons.AutoMirrored.Outlined.TrendingFlat
                },
                iconTint = trendTint,
                text = stringResource(
                    id = when {
                        dailyTrend.deltaPercent > 0 -> R.string.detail_trend_daily_up
                        dailyTrend.deltaPercent < 0 -> R.string.detail_trend_daily_down
                        else -> R.string.detail_trend_daily_steady
                    }
                ),
            )
        }

        StatTiles(
            modifier = Modifier.padding(top = if (dailyTrend == null) 0.dp else CardInnerSpacing),
            detailViewModel = detailViewModel,
            responses = responses,
            loop = loop,
            dailyTrend = dailyTrend,
        )
    }
}

/** 통계 섹션 맨 위의 "아이콘 · 한 줄 결론". 타일이 답할 수 없는 "그래서 어떤데"를 문장으로 말한다. */
@Composable
private fun InsightRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    text: String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = iconTint,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** 반폭 타일 2×2. 같은 줄의 두 타일은 [IntrinsicSize.Min] 으로 높이를 맞춘다. */
@Composable
private fun StatTiles(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    responses: List<LoopDoneVo>,
    loop: LoopBase,
    dailyTrend: DailyTrend?,
) {
    val accent = Color(loop.color).compositeOverOnSurface()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TileSpacing),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(TileSpacing),
        ) {
            TrendTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                trend = dailyTrend,
                accent = accent,
            )
            WeekdayTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                responses = responses,
                accent = accent,
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(TileSpacing),
        ) {
            MonthlyTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                responses = responses,
                accent = accent,
            )
            CompositionTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                detailViewModel = detailViewModel,
                accent = accent,
            )
        }
    }
}

/** 타일 한 장의 껍데기: 라벨 → 시각화 → 값. 세 줄 구조를 넷이 공유해 격자가 정돈돼 보인다. */
@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.labelSmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
        content()
    }
}

/** 타일 맨 아랫줄: 큰 값 + (있으면) 변화 배지. */
@Composable
private fun TileValueRow(
    modifier: Modifier = Modifier,
    value: String,
    delta: Int? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.bodyLarge.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (delta != null) {
            Text(
                modifier = Modifier.padding(start = 6.dp, bottom = 1.dp),
                text = formatDeltaPercent(delta),
                style = AppTypography.labelSmall.copy(
                    color = when {
                        delta > 0 -> AppColor.primary
                        delta < 0 -> AppColor.error
                        else -> AppColor.onSurface.copy(alpha = 0.45f)
                    },
                ),
            )
        }
    }
}

/** 데이터가 아직 없을 때 타일이 보여 주는 자리. */
@Composable
private fun TileEmptyValue(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.detail_tile_no_data),
        style = AppTypography.bodyLarge.copy(
            color = AppColor.onSurface.copy(alpha = 0.3f),
        ),
    )
}

/** 완료율 추세 타일: 스파크라인 + 현재 완료율 + 2주 전 대비 변화. */
@Composable
private fun TrendTile(
    modifier: Modifier = Modifier,
    trend: DailyTrend?,
    accent: Color,
) {
    StatTile(
        modifier = modifier,
        label = stringResource(id = R.string.detail_tile_trend),
    ) {
        if (trend == null) {
            TileEmptyValue(modifier = Modifier.padding(top = 10.dp))
            return@StatTile
        }

        Sparkline(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(TileVizHeight),
            values = trend.rates,
            color = accent,
        )
        TileValueRow(
            modifier = Modifier.padding(top = 8.dp),
            value = "${(trend.rates.last() * 100).roundToInt()}%",
            delta = trend.deltaPercent,
        )
    }
}

/** 요일별 타일: 7개 막대 + 가장 잘 지키는 요일. */
@Composable
private fun WeekdayTile(
    modifier: Modifier = Modifier,
    responses: List<LoopDoneVo>,
    accent: Color,
) {
    val rates = remember(responses) { computeWeekdayRates(responses) }
    val bestIndex = rates
        .withIndex()
        .filter { it.value != null }
        .maxByOrNull { it.value!! }
        ?.index

    StatTile(
        modifier = modifier,
        label = stringResource(id = R.string.detail_tile_weekday),
    ) {
        if (bestIndex == null) {
            TileEmptyValue(modifier = Modifier.padding(top = 10.dp))
            return@StatTile
        }

        TileBars(
            modifier = Modifier.padding(top = 10.dp),
            fractions = rates.map { it ?: 0f },
            highlightIndex = bestIndex,
            accent = accent,
        )
        TileValueRow(
            modifier = Modifier.padding(top = 8.dp),
            value = stringResource(
                id = R.string.detail_tile_best_day,
                stringResource(id = DAYS_WITH_3CHARS_SUNDAY_FIRST[bestIndex]),
                ((rates[bestIndex] ?: 0f) * 100).roundToInt(),
            ),
        )
    }
}

/** 월별 타일: 최근 몇 달 막대 + 이번 달 완료율과 지난달 대비 변화. */
@Composable
private fun MonthlyTile(
    modifier: Modifier = Modifier,
    responses: List<LoopDoneVo>,
    accent: Color,
) {
    val months = remember(responses) { computeMonthlyRates(responses) }

    StatTile(
        modifier = modifier,
        label = stringResource(id = R.string.detail_tile_monthly),
    ) {
        if (months.isEmpty()) {
            TileEmptyValue(modifier = Modifier.padding(top = 10.dp))
            return@StatTile
        }

        val current = months.last()
        val delta = months.getOrNull(months.size - 2)?.let { prev ->
            ((current.second - prev.second) * 100).roundToInt()
        }

        TileBars(
            modifier = Modifier.padding(top = 10.dp),
            fractions = months.map { it.second },
            highlightIndex = months.lastIndex,
            accent = accent,
        )
        TileValueRow(
            modifier = Modifier.padding(top = 8.dp),
            value = stringResource(
                id = R.string.detail_tile_month_rate,
                stringResource(id = ABB_MONTHS[current.first.monthValue - 1]),
                (current.second * 100).roundToInt(),
            ),
            delta = delta,
        )
    }
}

/** 응답 구성 타일: 완료·건너뜀·미응답 비중 막대 + 개수. */
@Composable
private fun CompositionTile(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    accent: Color,
) {
    val total by detailViewModel.allEnabledCount.collectAsState(initial = 0)
    val doneCount by detailViewModel.doneCount.collectAsState(initial = 0)
    val skipCount by detailViewModel.skipCount.collectAsState(initial = 0)
    val respondCount by detailViewModel.respondCount.collectAsState(initial = 0)

    // 미응답 = 전체 − 응답(완료+건너뜀). 데이터 경합으로 음수가 되지 않도록 방어한다.
    val noResponseCount = (total - respondCount).coerceAtLeast(0)

    StatTile(
        modifier = modifier,
        label = stringResource(id = R.string.detail_response_summary),
    ) {
        if (total == 0) {
            TileEmptyValue(modifier = Modifier.padding(top = 10.dp))
            return@StatTile
        }

        CompositionBar(
            modifier = Modifier.padding(top = 10.dp),
            doneCount = doneCount,
            skipCount = skipCount,
            noResponseCount = noResponseCount,
            doneColor = accent,
            skipColor = AppColor.onSurface.copy(alpha = 0.35f),
        )
        TileValueRow(
            modifier = Modifier.padding(top = 8.dp),
            value = "${stringResource(id = R.string.detail_rate_done)} $doneCount",
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = "${stringResource(id = R.string.detail_rate_skip)} $skipCount · " +
                    "${stringResource(id = R.string.detail_rate_no_response)} $noResponseCount",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.labelSmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.45f),
            ),
        )
    }
}

/** 타일 안 시각화가 공유하는 높이. 넷이 같은 높이를 쓰면 격자가 흔들리지 않는다. */
private val TileVizHeight = 32.dp

/**
 * 타일용 막대 열. 라벨 없이 형태만 보여 준다 — 반폭 타일에서 7~8개 라벨은 읽을 수 없을 만큼
 * 작아지고, 정작 알아야 할 값은 아래 [TileValueRow] 가 글자로 말해 주기 때문이다.
 */
@Composable
private fun TileBars(
    modifier: Modifier = Modifier,
    fractions: List<Float>,
    highlightIndex: Int,
    accent: Color,
    barAreaHeight: Dp = TileVizHeight,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barAreaHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        fractions.forEachIndexed { index, fraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    // 값이 0이어도 바닥에 아주 얇은 자국을 남겨 "칸"이 있음을 알린다.
                    .fillMaxHeight(fraction.coerceIn(0.06f, 1f))
                    .clip(RoundShapes.small)
                    .background(
                        if (index == highlightIndex) accent else accent.copy(alpha = 0.25f),
                    ),
            )
        }
    }
}

/**
 * 완료율(0f..1f) 시계열을 선으로 잇는 가벼운 스파크라인.
 * 절대 높이를 읽을 수 있도록 50% 기준선을 점선으로 깔고, 마지막 값에 점을 찍어 "지금"을 표시한다.
 */
@Composable
private fun Sparkline(
    modifier: Modifier = Modifier,
    values: List<Float>,
    color: Color,
) {
    val guideColor = AppColor.onSurface.copy(alpha = 0.12f)
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        drawLine(
            color = guideColor,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
            ),
        )

        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height * (1f - value.coerceIn(0f, 1f))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        drawCircle(
            color = color,
            radius = 2.5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                x = size.width,
                y = size.height * (1f - values.last().coerceIn(0f, 1f)),
            ),
        )
    }
}

/**
 * 완료 · 건너뜀 · 미응답을 이어 붙인 하나의 알약형 막대. 각 구간 너비는 개수에 비례한다.
 * 개수가 0인 구간은 그리지 않아(가중치 0 방지) 크래시를 막고, 미응답 구간은 색을 칠하지 않아
 * 트랙(빈 배경)이 그대로 드러나게 표현한다.
 */
@Composable
private fun CompositionBar(
    modifier: Modifier = Modifier,
    doneCount: Int,
    skipCount: Int,
    noResponseCount: Int,
    doneColor: Color,
    skipColor: Color,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(AppColor.onSurface.copy(alpha = 0.08f)),
    ) {
        if (doneCount > 0) {
            Box(
                modifier = Modifier
                    .weight(doneCount.toFloat())
                    .fillMaxHeight()
                    .background(doneColor),
            )
        }
        if (skipCount > 0) {
            Box(
                modifier = Modifier
                    .weight(skipCount.toFloat())
                    .fillMaxHeight()
                    .background(skipColor),
            )
        }
        if (noResponseCount > 0) {
            Box(modifier = Modifier.weight(noResponseCount.toFloat()))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2-3. 기록 · 회고 섹션
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 한 달 달력을 중심에 두고, 날짜를 누르면 그날의 상태와 회고 메모를 보고 남길 수 있는 섹션.
 * 완료한 날은 잔디처럼 옅은 강조색으로 칠하고, 메모가 있는 날은 우상단에 점 마커를 얹는다.
 */
@Composable
private fun JournalSection(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    responses: List<LoopDoneVo>,
    memoDates: Set<LocalDate>,
    loop: LoopBase,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val today = LocalDate.now()
    val accent = Color(loop.color).compositeOverOnSurface()
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
    val doneStateByDate = remember(responses) {
        responses.associate { it.date.toLocalDate() to it.done }
    }

    var selectedDate by remember { mutableStateOf(today) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }

    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.CalendarMonth,
        title = stringResource(id = R.string.daily_record),
        summary = stringResource(id = R.string.detail_memo_count, memoDates.size),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
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
            doneStateByDate = doneStateByDate,
            memoDates = memoDates,
            createdDate = createdDate,
            today = today,
            selectedDate = selectedDate,
            accent = accent,
            onSelect = { date -> selectedDate = date },
        )

        SelectedDayPanel(
            modifier = Modifier.padding(top = 18.dp),
            detailViewModel = detailViewModel,
            selectedDate = selectedDate,
            doneState = doneStateByDate[selectedDate],
            accent = accent,
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
        NavArrow(icon = Icons.Outlined.ChevronLeft, enabled = canGoPrev, onClick = onPrev)
        Text(
            modifier = Modifier.weight(1f),
            text = visibleMonth.atDay(1).formatYearMonth(),
            textAlign = TextAlign.Center,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        NavArrow(icon = Icons.Outlined.ChevronRight, enabled = canGoNext, onClick = onNext)
    }
}

@Composable
private fun NavArrow(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp)
            .alpha(if (enabled) 1f else 0.25f),
        imageVector = icon,
        tint = AppColor.onSurface.copy(alpha = 0.7f),
        contentDescription = null,
    )
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
        Row(modifier = Modifier.fillMaxWidth()) {
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
                        .height(40.dp),
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
            .clickable(enabled = selectable) { onSelect(date) },
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
                    .padding(top = 4.dp, end = 5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(AppColor.onSurface.copy(alpha = 0.55f)),
            )
        }
    }
}

/**
 * 달력에서 고른 날짜의 상태(완료/건너뜀/기록 없음)와 그날의 회고 메모를 편집하는 패널.
 * 날짜가 바뀌면 해당 날짜의 메모를 다시 불러온다.
 */
@Composable
private fun SelectedDayPanel(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    selectedDate: LocalDate,
    doneState: Int?,
    accent: Color,
) {
    // 선택 날짜가 바뀌면 입력값을 초기화하고, 저장된 메모를 비동기로 불러온다.
    var memo by remember(selectedDate) { mutableStateOf("") }
    var loaded by remember(selectedDate) { mutableStateOf(false) }
    LaunchedEffect(selectedDate) {
        memo = detailViewModel.retrospectOf(selectedDate) ?: ""
        loaded = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = selectedDate.formatMonthDateDay(),
                style = AppTypography.titleSmall.copy(color = AppColor.onSurface),
            )
            DayStatusChip(doneState = doneState, accent = accent)
        }

        JournalMemoField(
            modifier = Modifier.padding(top = 12.dp),
            value = memo,
            onValueChange = { memo = it },
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
                enabled = loaded,
                text = stringResource(id = R.string.save),
                onClick = { detailViewModel.saveRetrospect(selectedDate, memo) },
            )
        }
    }
}

/** 선택한 날짜의 상태를 알약으로 보여준다(완료=강조색 / 건너뜀 / 기록 없음). */
@Composable
private fun DayStatusChip(
    doneState: Int?,
    accent: Color,
) {
    val labelRes: Int
    val color: Color
    when (doneState) {
        DoneState.DONE -> {
            labelRes = R.string.done
            color = accent
        }

        DoneState.SKIP -> {
            labelRes = R.string.skip
            color = AppColor.onSurface.copy(alpha = 0.6f)
        }

        else -> {
            labelRes = R.string.detail_day_no_record
            color = AppColor.onSurface.copy(alpha = 0.4f)
        }
    }
    Text(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = stringResource(id = labelRes),
        style = AppTypography.labelMedium.copy(color = color),
    )
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

// ─────────────────────────────────────────────────────────────────────────────
// 2-4. 삭제
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 목록 맨 아래의 삭제 행. 되돌릴 수 없는 동작이므로 error 색으로만 칠해 다른 행과 구분하고,
 * 실제 삭제는 홈과 같은 확인 다이얼로그를 한 번 거친다(다이얼로그가 기록·메모까지 지워진다고 알린다).
 */
@Composable
private fun DeleteRow(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteLoopDialog(
            loopTitle = loop.title,
            loopColor = loop.color,
            onDismiss = { showDeleteDialog = false },
            onDelete = onDelete,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDeleteDialog = true }
            .padding(horizontal = CardPadding, vertical = SectionRowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.Outlined.Delete,
            tint = AppColor.error,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = stringResource(id = R.string.delete_loop_title),
            style = AppTypography.bodyLarge.copy(
                color = AppColor.error,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 공용 조각
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shared container for every section on the detail screen: a lifted surface with soft
 * rounding and a hairline border so cards read as a distinct layer over the background in
 * both light and dark themes (mirrors the card styling used on the home screen).
 */
@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    // 행이 스스로 여백을 갖는 목록을 담을 때는 바깥 여백을 비워, 탭 영역이 카드 가장자리까지 닿게 한다.
    contentPadding: PaddingValues = PaddingValues(all = CardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundShapes.large,
        color = AppColor.surfaceElevated,
        border = BorderStroke(
            width = 0.5.dp,
            color = AppColor.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(paddingValues = contentPadding),
            content = content,
        )
    }
}

/** 이름 인라인 편집기. 열리자마자 커서가 잡히고, 빈 제목으로는 저장할 수 없다. */
@Composable
private fun TitleEditor(
    modifier: Modifier = Modifier,
    accent: Color,
    value: String,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            BasicTextField(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(AppColor.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onSave() }),
                textStyle = AppTypography.titleLarge.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.detail_loop_name),
                            style = AppTypography.titleLarge.copy(
                                color = AppColor.onSurface.copy(alpha = 0.3f),
                            ),
                        )
                    }
                    innerTextField()
                },
            )
        }
        // 입력 중임을 알리는 밑줄. 테두리 상자를 두르면 카드 안에 카드가 생겨 답답해진다.
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColor.primary.copy(alpha = 0.5f)),
        )
        EditActionRow(
            modifier = Modifier.padding(top = 12.dp),
            saveEnabled = value.isNotBlank(),
            onCancel = onCancel,
            onSave = onSave,
        )
    }
}

/** 인라인 편집의 공통 하단 줄: 오른쪽 정렬된 [취소] [저장]. */
@Composable
private fun EditActionRow(
    modifier: Modifier = Modifier,
    saveEnabled: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextActionButton(
            text = stringResource(id = R.string.cancel),
            onClick = onCancel,
        )
        PrimaryPillButton(
            modifier = Modifier.padding(start = 4.dp),
            enabled = saveEnabled,
            text = stringResource(id = R.string.save),
            onClick = onSave,
        )
    }
}

/** 테두리 없는 보조 동작 버튼(취소 · 시간 수정). */
@Composable
private fun TextActionButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .clip(RoundShapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        text = text,
        style = AppTypography.labelLarge.copy(
            color = AppColor.primary,
        ),
    )
}

/** 강조색으로 채운 알약 버튼(저장). */
@Composable
private fun PrimaryPillButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(AppColor.primary.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        text = text,
        style = AppTypography.labelLarge.copy(color = AppColor.onPrimary),
    )
}

/** 카드 안에서 두 영역을 가르는 머리카락 굵기의 선. */
@Composable
private fun HairlineDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(AppColor.onSurface.copy(alpha = 0.10f)),
    )
}

/**
 * 하루(24시간)를 가로 막대로 놓고 활동 구간([startFraction]~[endFraction], 0f..1f)만 강조색으로 칠한다.
 * 30분 같은 짧은 구간도 사라지지 않도록 최소 폭을 보장하며, 시작 위치는 실제 시각에 맞춘다.
 */
@Composable
private fun DayTimeline(
    modifier: Modifier = Modifier,
    startFraction: Float,
    endFraction: Float,
    accent: Color,
) {
    val before = startFraction.coerceIn(0f, 1f)
    val window = (endFraction - startFraction).coerceIn(0.03f, 1f - before)
    val after = (1f - before - window).coerceAtLeast(0f)

    Row(
        modifier = modifier
            .height(12.dp)
            .clip(CircleShape)
            .background(AppColor.surfaceContainer),
    ) {
        if (before > 0f) {
            Box(modifier = Modifier.weight(before).fillMaxHeight())
        }
        Box(
            modifier = Modifier
                .weight(window)
                .fillMaxHeight()
                .background(accent),
        )
        if (after > 0f) {
            Box(modifier = Modifier.weight(after).fillMaxHeight())
        }
    }
}

/** 타임라인 아래 0·6·12·18·24시 눈금 라벨. */
@Composable
private fun HourTicks(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(0, 6, 12, 18, 24).forEach { hour ->
            Text(
                text = "$hour",
                style = AppTypography.labelSmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** A single "icon · label … value" line used inside the schedule section. */
@Composable
private fun InfoRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = AppColor.onSurface.copy(alpha = 0.5f),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = label,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (trailing != null) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = trailing,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** The seven weekdays as compact chips; active days are filled and tinted with the accent. */
@Composable
private fun ActiveDaysRow(
    modifier: Modifier = Modifier,
    activeDays: Int,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val selected = activeDays.isOn(LoopDay.fromIndex(index))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (selected) {
                            AppColor.primary.copy(alpha = 0.14f)
                        } else {
                            AppColor.surfaceContainer
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(id = dayResId),
                    style = AppTypography.labelMedium.copy(
                        color = if (selected) {
                            AppColor.primary
                        } else {
                            AppColor.onSurface.copy(alpha = 0.4f)
                        },
                    ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 계산용 순수 함수 (Compose 와 무관하게 테스트 가능)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 이번 주(최근 7일) 성과 요약.
 * @param doneThisWeek 최근 7일 중 완료한 날 수
 * @param activeThisWeek 최근 7일 중 이 루프가 활동해야 하는(활성 요일·생성 이후) 날 수
 * @param trend 지난 7일 대비 완료 수 변화(양수=개선, 0=유지, 음수=주춤)
 */
private data class WeeklyProgress(
    val doneThisWeek: Int,
    val activeThisWeek: Int,
    val trend: Int,
)

private fun computeWeeklyProgress(
    doneStateByDate: Map<LocalDate, Int>,
    activeDays: Int,
    createdDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): WeeklyProgress {
    val last7 = (0..6).map { today.minusDays(it.toLong()) }
    val prev7 = (7..13).map { today.minusDays(it.toLong()) }

    val doneThisWeek = last7.count { doneStateByDate[it] == DoneState.DONE }
    val donePrevWeek = prev7.count { doneStateByDate[it] == DoneState.DONE }
    val activeThisWeek = last7.count {
        !it.isBefore(createdDate) && activeDays.isOn(dayForLoop(it))
    }

    return WeeklyProgress(
        doneThisWeek = doneThisWeek,
        activeThisWeek = activeThisWeek,
        trend = doneThisWeek.compareTo(donePrevWeek),
    )
}

/**
 * 일별 완료율 추세.
 * @param rates 각 날짜의 최근 [computeDailyTrend]의 windowDays 일 롤링 완료율(0f..1f), 과거→오늘 순
 * @param deltaPercent 약 2주 전 대비 완료율 변화(%p, 양수=상승)
 */
private data class DailyTrend(
    val rates: List<Float>,
    val deltaPercent: Int,
)

/**
 * 최근 [maxPoints]일에 대해 [windowDays]일 롤링 완료율을 계산한다.
 * 각 날짜의 값 = (창 안의 완료 수) / (창 안의 응답 대상 수). 응답 대상은 비활성(DISABLED)이 아닌 기록.
 * 기록이 전혀 없거나 구간이 너무 짧으면 null 을 돌려 타일을 빈 상태로 둔다.
 */
private fun computeDailyTrend(
    responses: List<LoopDoneVo>,
    createdDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    windowDays: Int = 7,
    maxPoints: Int = 60,
): DailyTrend? {
    val doneByDate = responses
        .filter { !it.isDisabled() }
        .associate { it.date.toLocalDate() to it.isDone() }
    if (doneByDate.isEmpty()) return null

    val start = maxOf(createdDate, today.minusDays((maxPoints - 1).toLong()))
    val totalDays = (today.toEpochDay() - start.toEpochDay()).toInt() + 1
    if (totalDays < 2) return null

    val rates = (0 until totalDays).map { offset ->
        val day = start.plusDays(offset.toLong())
        var enabled = 0
        var done = 0
        var cursor = day.minusDays((windowDays - 1).toLong())
        while (!cursor.isAfter(day)) {
            doneByDate[cursor]?.let { isDone ->
                enabled++
                if (isDone) done++
            }
            cursor = cursor.plusDays(1)
        }
        if (enabled == 0) 0f else done.toFloat() / enabled
    }

    // 약 2주 전 지점과 비교해 최근 추세를 %p 로 낸다(데이터가 짧으면 첫 지점과 비교).
    val referenceIndex = (rates.lastIndex - 14).coerceAtLeast(0)
    val deltaPercent = ((rates.last() - rates[referenceIndex]) * 100).roundToInt()
    return DailyTrend(rates = rates, deltaPercent = deltaPercent)
}

/**
 * 최근 [monthsBack]개월의 월별 완료율(0f..1f). 데이터가 있는 달만, 오래된 달→최신 달 순으로 담는다.
 * 완료율 = 그 달의 완료 수 / 응답 대상(비활성 제외) 수.
 */
private fun computeMonthlyRates(
    responses: List<LoopDoneVo>,
    monthsBack: Int = 6,
): List<Pair<YearMonth, Float>> {
    val enabled = responses.filter { !it.isDisabled() }
    if (enabled.isEmpty()) return emptyList()

    return enabled
        .groupBy { YearMonth.from(it.date.toLocalDate()) }
        .map { (yearMonth, records) ->
            yearMonth to records.count { it.isDone() }.toFloat() / records.size
        }
        .sortedBy { it.first }
        .takeLast(monthsBack)
}

/**
 * 요일별 완료율(0f..1f). 인덱스 0=일요일 … 6=토요일(달력 헤더와 같은 순서).
 * 그 요일에 응답 대상 기록이 하나도 없으면 해당 칸은 null.
 */
private fun computeWeekdayRates(
    responses: List<LoopDoneVo>,
): List<Float?> {
    val byDayOfWeek = responses
        .filter { !it.isDisabled() }
        .groupBy { it.date.toLocalDate().dayOfWeek }

    return (0..6).map { index ->
        val dayOfWeek = DayOfWeek.of(if (index == 0) 7 else index)
        val records = byDayOfWeek[dayOfWeek]
        if (records.isNullOrEmpty()) null
        else records.count { it.isDone() }.toFloat() / records.size
    }
}

/** 변화량을 "+12%p" / "-5%p" / "0%p" 형태의 짧은 배지 문자열로 만든다. */
private fun formatDeltaPercent(delta: Int): String =
    (if (delta > 0) "+$delta" else "$delta") + "%p"
