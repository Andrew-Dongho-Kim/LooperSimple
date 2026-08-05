package com.pnd.android.loop.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.isSameMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 아래 달력 패널의 내용. 월 요약 배너 → 요일 헤더 → 6주 그리드를 세로로 쌓는다.
 *
 * [headerCollapseProgress]는 패널을 아래로 끌어 접은 정도(`0f` 펼침 ~ `1f` 헤더 완전히 접힘)다.
 * 이 값에 따라 그리드 위 헤더를 **월 요약 배너부터, 그다음 요일 헤더 순서로** 접어, 패널이 낮아지는
 * 만큼을 헤더가 먼저 내놓는다. 접히는 총량([DailyAchievementCollapsibleHeaderHeight])을 dp로 나눠
 * 배분하므로, 접는 중에도 아래 6주 그리드 높이는 변하지 않는다(날짜 셀이 출렁이지 않는다).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyAchievementCalendar(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    achievementViewModel: DailyAchievementViewModel,
    minDate: LocalDate,
    selectedDate: LocalDate,
    headerCollapseProgress: Float,
    onDateSelected: (LocalDate) -> Unit
) {
    // 선택된 날짜가 속한 달을 기준으로 요약/회고를 집계한다.
    val yearMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val summary by remember(yearMonth) {
        achievementViewModel.flowMonthSummary(yearMonth)
    }.collectAsState(initial = MonthAchievementSummary.Empty)
    val retrospects by remember(yearMonth) {
        achievementViewModel.flowMonthRetrospects(yearMonth)
    }.collectAsState(initial = emptyList())

    var showRetrospects by remember { mutableStateOf(false) }

    // 접어야 할 총량을 요약 배너 → 요일 헤더 순서로 채운다. 배너를 다 접기 전까지 요일 헤더는 그대로다.
    val headerCollapsedHeight =
        DailyAchievementCollapsibleHeaderHeight * headerCollapseProgress.coerceIn(0f, 1f)
    val summaryCollapsedHeight = headerCollapsedHeight.coerceAtMost(SummarySlotHeight)
    val weekdayCollapsedHeight = (headerCollapsedHeight - SummarySlotHeight)
        .coerceIn(0.dp, WeekdaySlotHeight)

    Column(modifier = modifier) {
        // 선택한 달 요약 배너: 달성률 링 + 완료/전체 + 회고 칩 + 지표 타일 4칸. 가장 먼저 접히는 칸이다.
        CollapsibleHeaderSlot(
            fullHeight = SummarySlotHeight,
            collapsedHeight = summaryCollapsedHeight,
        ) {
            SelectedMonthSummaryBar(
                modifier = Modifier.padding(bottom = Dimens.itemSpacing),
                summary = summary,
                onClickRetrospects = { showRetrospects = true },
            )
        }
        // 요일 헤더: 배너가 완전히 접힌 뒤에 이어서 접힌다.
        CollapsibleHeaderSlot(
            fullHeight = WeekdaySlotHeight,
            collapsedHeight = weekdayCollapsedHeight,
        ) {
            CalendarHeader(modifier = Modifier.padding(bottom = Dimens.cardSpacing))
        }
        HorizontalPager(
            state = pagerState,
            reverseLayout = true,
        ) { page ->
            CalendarPage(
                achievementViewModel = achievementViewModel,
                minDate = minDate,
                selectedDate = selectedDate,
                firstDateOfMonth = LocalDate.now().minusMonths(page.toLong()).withDayOfMonth(1),
                onDateSelected = onDateSelected
            )
        }
    }

    // 회고 칩을 누르면 그 달의 회고를 한곳에 모아 보여준다.
    if (showRetrospects) {
        MonthRetrospectsDialog(
            monthLabel = selectedDate.formatYearMonth(),
            retrospects = retrospects,
            onDismiss = { showRetrospects = false },
        )
    }
}

/**
 * 접히는 헤더 한 칸. [collapsedHeight]만큼 아래가 잘려 사라지고, 남은 비율만큼 함께 흐려진다.
 *
 * 내용은 [requiredHeight]로 원래 크기를 유지한 채 부모가 잘라 내므로(`clipToBounds`), 접히는 동안
 * 달성률 링이나 글자가 찌그러지지 않는다. 다 접히면 아예 컴포즈하지 않아, 접힌 상태에서는 비용이 없다.
 */
@Composable
private fun CollapsibleHeaderSlot(
    fullHeight: Dp,
    collapsedHeight: Dp,
    content: @Composable () -> Unit,
) {
    val visibleHeight = fullHeight - collapsedHeight
    if (visibleHeight <= 0.dp) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(visibleHeight)
            .clipToBounds()
            .alpha((visibleHeight / fullHeight).coerceIn(0f, 1f)),
    ) {
        Box(modifier = Modifier.requiredHeight(fullHeight)) { content() }
    }
}

@Composable
fun CalendarHeader(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.contentPadding, bottom = Dimens.itemSpacing)
        ) {
            DAYS_WITH_3CHARS_SUNDAY_FIRST.forEachIndexed { index, dayResId ->
                val dayOfWeek = DayOfWeek.of(
                    if (index == 0) DayOfWeek.SUNDAY.value else index
                )
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = dayResId).uppercase(),
                    style = AppTypography.labelSmall.copy(
                        color = dayOfWeek.color().copy(alpha = 0.7f)
                    )
                )
            }
        }
        HorizontalDivider(
            thickness = CalendarHeaderDividerThickness,
            color = AppColor.onSurface.copy(alpha = 0.08f),
        )
    }
}


/**
 * 한 달의 날짜 그리드. 행 수는 달마다 4~6주로 달라지지만, 그리드는 **항상 [CalendarGridRows]주**를
 * 그린다. 예전에는 실제 주 수만큼만 그려서 같은 높이를 5주로 나눌 때와 6주로 나눌 때 셀 높이가
 * 달라졌고, 6주인 달에서는 셀이 필요 높이보다 좁아져 아래쪽 색 도트가 잘려 보였다. 행 수를 고정하면
 * 어느 달에서나 셀 높이가 같고, 달을 넘겨도 셀 크기가 출렁이지 않는다.
 *
 * 그래도 화면이 작거나 글꼴 배율이 커서 셀 높이가 [MinRowHeightForDots]에 못 미치면, 도트를 잘라
 * 보여주는 대신 감춘다(날짜와 히트 배경은 그대로 남는다).
 */
@Composable
private fun CalendarPage(
    modifier: Modifier = Modifier,
    achievementViewModel: DailyAchievementViewModel,
    minDate: LocalDate,
    selectedDate: LocalDate,
    firstDateOfMonth: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    // 그리드 첫 칸의 날짜. 1일이 주 중간이면 그만큼 지난달 날짜로 앞을 채운다.
    val firstCellDate = remember(firstDateOfMonth) {
        firstDateOfMonth.minusDays(firstDateOfMonth.dayOfWeek.value % 7L)
    }
    val lastCellDate = remember(firstCellDate) {
        firstCellDate.plusDays((CalendarGridRows * DAYS_OF_WEEK - 1).toLong())
    }

    val doneLoopsByDate by achievementViewModel.flowsDoneLoopsByDate(
        from = firstCellDate,
        to = lastCellDate,
    ).collectAsState(initial = emptyMap())

    val noDoneLoopsByDate by achievementViewModel.flowsNoDonLoopsByDate(
        from = firstCellDate,
        to = lastCellDate,
    ).collectAsState(initial = emptyMap())

    BoxWithConstraints(modifier = modifier) {
        // 주어진 높이를 고정 행 수로 나눈다. 패널이 6주분을 확보해 주므로 보통은 넉넉하지만,
        // 상한에 걸려 좁아진 경우에도 넘치지 않고 셀 표시만 축약된다.
        val rowHeight = this.maxHeight / CalendarGridRows
        val showDots = rowHeight >= MinRowHeightForDots

        Column {
            repeat(CalendarGridRows) { rowIndex ->
                val rowDate = firstCellDate.plusWeeks(rowIndex.toLong())
                key(rowDate) {
                    CalendarRow(
                        modifier = Modifier.height(rowHeight),
                        doneLoopsByDate = doneLoopsByDate,
                        noDoneLoopsByDate = noDoneLoopsByDate,
                        itemDate = rowDate,
                        minDate = minDate,
                        selectedDate = selectedDate,
                        showDots = showDots,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarRow(
    modifier: Modifier = Modifier,
    doneLoopsByDate: Map<LocalDate, List<LoopByDate>>,
    noDoneLoopsByDate: Map<LocalDate, List<LoopByDate>>,
    itemDate: LocalDate,
    minDate: LocalDate,
    selectedDate: LocalDate,
    showDots: Boolean,
    onDateSelected: (LocalDate) -> Unit,
) {
    var itDate = itemDate
    Row(modifier = modifier.fillMaxWidth()) {
        repeat(DAYS_OF_WEEK) {
            key(itDate) {
                val isThisMonth = selectedDate.isSameMonth(itDate)
                val isBeforeNow = itDate.isBefore(LocalDate.now().plusDays(1))
                val isAfterMinDate = itDate.isAfter(minDate.minusDays(1))
                val isInterest = isThisMonth && isBeforeNow && isAfterMinDate

                CalendarDateItem(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(
                            alpha = if (isInterest) {
                                1f
                            } else {
                                0.3f
                            }
                        ),
                    doneLoops = doneLoopsByDate[itDate] ?: emptyList(),
                    noDoneLoops = noDoneLoopsByDate[itDate] ?: emptyList(),
                    itemDate = itDate,
                    isInterest = isInterest,
                    isToday = itDate == LocalDate.now(),
                    isSelected = itDate == selectedDate,
                    showDots = showDots,
                    onDateSelected = onDateSelected,
                )
            }
            itDate = itDate.plusDays(1)
        }
    }
}

@Composable
private fun CalendarDateItem(
    modifier: Modifier = Modifier,
    doneLoops: List<LoopByDate>,
    noDoneLoops: List<LoopByDate>,
    itemDate: LocalDate,
    isInterest: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    showDots: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    // 회고 여부는 배지 우상단 코너 마커로 넘겨, 더 이상 세로로 한 줄을 차지하지 않는다.
    val hasRetrospect = doneLoops.any { it.retrospect != null } ||
            noDoneLoops.any { it.retrospect != null }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(all = CalendarCellPadding)
            .clip(RoundShapes.medium)
            .achievementHeat(
                // 셀 뒤에 히트맵을 칠해, 색 도트와 함께 한 달의 흐름이 색으로 읽히게 한다.
                enabled = isInterest,
                doneCount = doneLoops.size,
                totalCount = doneLoops.size + noDoneLoops.size,
                color = AppColor.primary,
                isDark = isSystemInDarkTheme(),
            )
            // 선택된 날은 셀 전체를 감싸는 외곽 링으로 표시한다. 면이 아니라 경계선이라 히트 농도와
            // 경쟁하지 않아 어떤 완료율 배경 위에서도 또렷하고, 히트(완료율) 정보도 가리지 않는다.
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = AppColor.primary,
                        shape = RoundShapes.medium,
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onDateSelected(itemDate) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CalendarDayBadge(
            dayOfMonth = itemDate.dayOfMonth,
            dayColor = itemDate.dayOfWeek.color(),
            isToday = isToday,
            isSelected = isSelected,
            hasRetrospect = hasRetrospect,
        )

        // 셀이 도트를 온전히 담을 만큼 높지 않으면(showDots == false) 그리지 않는다. 잘린 도트보다
        // 아무것도 없는 편이 낫고, 그날의 성취는 히트 배경으로 여전히 읽힌다.
        if (isInterest && showDots) {
            ColorDotIndicator(
                modifier = Modifier.padding(top = DotIndicatorTopGap),
                doneLoops = doneLoops,
            )
        }
    }
}

/**
 * 오늘/선택된 날짜를 애플 캘린더처럼 원형 배지로 강조해서 그리는 날짜 숫자.
 *
 * - 오늘: primary 로 채운 원 + 대비되는 onPrimary 글자
 * - 선택: primary 가 옅게 깔린 원 + primary 글자
 * - 그 외: 배경 없이 요일 색 그대로
 *
 * 회고([hasRetrospect])가 있으면 배지 우상단에 작은 연필 마커를 얹는다. 별도의 줄을 차지하지 않아
 * 좁은 셀에서도 아래쪽 성취 지표가 잘리지 않는다. 마커는 패널과 같은 [surfaceContainer] 칩 위에
 * 그려, 오늘 배지(primary 채움)를 포함한 어떤 배경 위에서도 대비를 유지한다.
 *
 * 라이트/다크 모드 모두 [AppColor] 토큰을 사용하므로 테마에 맞춰 자동으로 대비가 유지된다.
 */
@Composable
private fun CalendarDayBadge(
    modifier: Modifier = Modifier,
    dayOfMonth: Int,
    dayColor: Color,
    isToday: Boolean,
    isSelected: Boolean,
    hasRetrospect: Boolean,
) {
    // 선택 표시는 셀 외곽 링이 담당하므로, 배지에는 '오늘'만 primary 채움으로 강조한다.
    // (선택 상태의 파란 반투명 원/파란 글자는 파란 히트 배경과 겹쳐 보이지 않아 제거)
    val badgeColor = if (isToday) AppColor.primary else Color.Transparent
    val textColor = if (isToday) AppColor.onPrimary else dayColor
    Box(
        modifier = modifier.padding(top = DayBadgeTopPadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(DayBadgeSize)
                .background(color = badgeColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$dayOfMonth",
                textAlign = TextAlign.Center,
                style = AppTypography.bodySmall.copy(
                    color = textColor,
                    fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
        }

        if (hasRetrospect) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // 숫자와 겹치지 않도록 배지 밖 우상단 모서리로 살짝 밀어낸다.
                    .offset(x = 4.dp, y = (-3).dp)
                    .size(13.dp)
                    .background(color = AppColor.surfaceContainer, shape = CircleShape)
                    .border(
                        width = 0.5.dp,
                        color = AppColor.onSurface.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier.size(9.dp),
                    imageVector = Icons.Outlined.Edit,
                    colorFilter = ColorFilter.tint(AppColor.onSurface.copy(alpha = 0.6f)),
                    contentDescription = null,
                )
            }
        }
    }
}

/**
 * 달성률(완료 비율)을 5단계로 나눠 셀 뒤에 "히트" 배경을 칠한다. GitHub 잔디처럼 한 달의 성취
 * 흐름이 색 농도로 한눈에 읽히게 한다. 색은 앱 강조색([color], primary)을 그대로 써 앱과 통일감을 준다.
 *
 * 단계별 알파는 라이트/다크에서 각각 대비가 유지되도록 따로 둔다([isDark]가 참이면 더 진하게).
 * 완료가 하나도 없는 날은 칠하지 않아, 색이 곧 "그날 얼마나 해냈는가"를 뜻하게 한다.
 */
private fun Modifier.achievementHeat(
    enabled: Boolean,
    doneCount: Int,
    totalCount: Int,
    color: Color,
    isDark: Boolean,
) = drawBehind {
    if (!enabled || totalCount == 0 || doneCount == 0) return@drawBehind

    val doneRate = doneCount.toFloat() / totalCount
    // 저조 → 완료(100%)로 갈수록 진해지는 5단계 강도. 다크 모드는 배경이 어두워 같은 채도라도
    // 옅게 보이므로 각 단계 알파를 조금씩 높여 두 테마에서 비슷한 존재감을 갖게 한다.
    val level = when {
        doneRate < 0.25f -> 0
        doneRate < 0.50f -> 1
        doneRate < 0.75f -> 2
        doneRate < 1f -> 3
        else -> 4
    }
    val alphas = if (isDark) {
        floatArrayOf(0.14f, 0.24f, 0.36f, 0.48f, 0.62f)
    } else {
        floatArrayOf(0.10f, 0.18f, 0.28f, 0.40f, 0.55f)
    }
    drawRoundRect(
        // 완료 루프 도트가 히트 배경에 묻히지 않도록 전면 워시 강도를 낮춘다(도트는 배경판으로 한 번 더 분리).
        color = color.copy(alpha = alphas[level] * HeatIntensityScale),
        cornerRadius = CornerRadius(x = 8.dp.toPx(), y = 8.dp.toPx()),
    )
}

/** 그날 완료한 루프를 루프 색 도트로 보여주는 셀 하단 지표. */
@Composable
private fun ColorDotIndicator(
    modifier: Modifier = Modifier,
    doneLoops: List<LoopByDate>,
) {
    // 완료한 루프가 없으면 배경판만 덩그러니 남지 않도록 아무것도 그리지 않는다.
    if (doneLoops.isEmpty()) return

    Row(
        modifier = modifier
            // 히트 배경 위에서도 도트 색이 선명하도록, 도트 묶음 뒤에 불투명 배경판(알약)을 깐다.
            // surfaceContainer는 합성된 불투명색이라 어떤 히트 농도도 확실히 가린다.
            .clip(CircleShape)
            .background(AppColor.surfaceContainer)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.10f),
                shape = CircleShape,
            )
            .padding(horizontal = 4.dp, vertical = DotPillVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        doneLoops.take(MAX_VISIBLE_DOTS).forEach { loop ->
            Box(
                modifier = Modifier
                    .size(DotSize)
                    .background(
                        color = loop.color
                            .compositeOverOnSurface()
                            .copy(alpha = 0.85f),
                        shape = CircleShape
                    )
            )
        }
        if (doneLoops.size > MAX_VISIBLE_DOTS) {
            Text(
                text = "+",
                style = AppTypography.labelSmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}


private const val DAYS_OF_WEEK = 7

/** 한 셀에 표시할 색상 도트의 최대 개수. 초과분은 "+" 로 축약한다. */
private const val MAX_VISIBLE_DOTS = 5

/** 히트 배경 강도 배율. 도트가 배경에 겹쳐 묻히지 않도록 전면 워시를 절반 남짓으로 낮춘다. */
private const val HeatIntensityScale = 0.55f

/**
 * 달력 그리드의 고정 행 수. 달마다 다른 실제 주 수(4~6) 대신 항상 6주를 그려, 어느 달에서나 셀 높이가
 * 같도록 한다. 6주가 한 달이 가질 수 있는 최대 주 수라, 이 높이만 확보하면 어떤 달도 잘리지 않는다.
 */
private const val CalendarGridRows = 6

// --- 셀 구성 요소의 크기 ---------------------------------------------------------------------
// 아래 값들은 실제 레이아웃에 그대로 쓰이고, 동시에 셀 최소 높이([MinRowHeightForDots])와
// 달력 전체 높이([DailyAchievementCalendarHeight])를 계산하는 근거가 된다. 값을 바꾸면 두 계산도
// 자동으로 따라오므로, 상수와 실제 크기가 어긋날 일이 없다.
// (파일 상단이 아니라 이곳에 모아 둔 이유: 최상위 프로퍼티는 선언 순서대로 초기화되므로,
//  계산된 값은 근거가 되는 값들보다 뒤에 와야 한다.)

/** 셀 바깥 여백. 셀과 셀 사이 간격이 된다. */
private val CalendarCellPadding = 2.dp

/** 날짜 숫자 배지(원)의 지름과 그 위 여백. */
private val DayBadgeSize = 24.dp
private val DayBadgeTopPadding = 2.dp

/** 날짜 배지와 아래 도트 알약 사이 간격. */
private val DotIndicatorTopGap = 3.dp

/** 색 도트 하나의 지름과, 도트 알약의 위아래 여백. */
private val DotSize = 5.dp
private val DotPillVerticalPadding = 3.dp

/** 요일 헤더의 구분선 굵기와 요일 글자 한 줄의 높이(labelSmall, 기본 글꼴 배율 기준). */
private val CalendarHeaderDividerThickness = 0.5.dp
private val CalendarWeekdayLabelHeight = 14.dp

/**
 * 날짜 배지와 도트 알약이 모두 잘리지 않고 들어가는 셀(=행)의 최소 높이.
 * 행 높이가 이 값에 못 미치면 도트를 감춘다.
 */
private val MinRowHeightForDots = CalendarCellPadding * 2 +
        DayBadgeTopPadding + DayBadgeSize +
        DotIndicatorTopGap + DotPillVerticalPadding * 2 + DotSize

/** 한 행(=날짜 셀)의 목표 높이. 최소 높이에 약간의 여유를 둔 값이다. */
private val CalendarRowHeight = MinRowHeightForDots + 4.dp

/** 요일 헤더 전체 높이(위 여백 + 요일 글자 + 아래 여백 + 구분선). */
private val CalendarHeaderHeight = Dimens.contentPadding + CalendarWeekdayLabelHeight +
        Dimens.itemSpacing + CalendarHeaderDividerThickness

// --- 접히는 헤더 칸의 높이 -------------------------------------------------------------------
// 그리드 위의 두 칸은 각각 아래 여백까지 포함해 한 덩어리로 접힌다. 패널을 접을 때 이 두 값의 합만큼
// 높이를 내놓고, 그리드는 손대지 않는다.

/** 월 요약 배너 칸(배너 + 아래 간격). 가장 먼저 접힌다. */
private val SummarySlotHeight = SelectedMonthSummaryBarHeight + Dimens.itemSpacing

/** 요일 헤더 칸(요일 줄 + 구분선 + 그리드와의 간격). 배너가 다 접힌 뒤에 접힌다. */
private val WeekdaySlotHeight = CalendarHeaderHeight + Dimens.cardSpacing

/**
 * 헤더를 모두 접었을 때 달력이 내놓는 높이. 패널은 이만큼 낮아지는 동안 헤더만 접고 6주 그리드는
 * 그대로 유지한다. 이 지점이 패널 접힘의 중간 스냅 단계가 된다.
 */
val DailyAchievementCollapsibleHeaderHeight = SummarySlotHeight + WeekdaySlotHeight

/**
 * 달력 콘텐츠(월 요약 배너 + 요일 헤더 + 6주 그리드)를 잘림 없이 담는 데 필요한 높이.
 * 아래 달력 패널이 이 값으로 펼침 높이를 역산한다. 화면이 작아 이만큼 확보하지 못하면 행이 그만큼
 * 좁아지고, 좁아진 셀은 도트를 감춰(위 [MinRowHeightForDots]) 잘림 없이 대응한다.
 */
val DailyAchievementCalendarHeight = DailyAchievementCollapsibleHeaderHeight +
        CalendarRowHeight * CalendarGridRows
