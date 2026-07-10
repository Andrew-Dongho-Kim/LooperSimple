package com.pnd.android.loop.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.pnd.android.loop.ui.statisctics.StreakStat
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.isSameMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyAchievementCalendar(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    achievementViewModel: DailyAchievementViewModel,
    viewMode: DailyAchievementPageViewMode,
    minDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    // 선택된 날짜가 속한 달을 기준으로 요약/회고를 집계한다.
    val yearMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val summary by remember(yearMonth) {
        achievementViewModel.flowMonthSummary(yearMonth)
    }.collectAsState(initial = MonthAchievementSummary.Empty)
    // 스트릭은 기간과 무관한 전체 기록 기준이라 selectedDate가 바뀌어도 다시 구독하지 않는다.
    val streak by remember {
        achievementViewModel.flowStreak()
    }.collectAsState(initial = StreakStat(current = 0, longest = 0))
    val retrospects by remember(yearMonth) {
        achievementViewModel.flowMonthRetrospects(yearMonth)
    }.collectAsState(initial = emptyList())

    var showRetrospects by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // 선택한 달 요약 배너: 달성률 링 + 완료/전체 + 스트릭·회고 칩.
        SelectedMonthSummaryBar(
            modifier = Modifier.padding(bottom = Dimens.itemSpacing),
            summary = summary,
            currentStreak = streak.current,
            onClickRetrospects = { showRetrospects = true },
        )
        CalendarHeader()
        HorizontalPager(
            modifier = Modifier.padding(top = Dimens.cardSpacing),
            state = pagerState,
            reverseLayout = true,
        ) { page ->
            CalendarPage(
                achievementViewModel = achievementViewModel,
                viewMode = viewMode,
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
            thickness = 0.5.dp,
            color = AppColor.onSurface.copy(alpha = 0.08f),
        )
    }
}


@Composable
private fun CalendarPage(
    modifier: Modifier = Modifier,
    achievementViewModel: DailyAchievementViewModel,
    viewMode: DailyAchievementPageViewMode,
    minDate: LocalDate,
    selectedDate: LocalDate,
    firstDateOfMonth: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val start = remember(firstDateOfMonth) {
        firstDateOfMonth.dayOfWeek.value % 7L
    }
    val days = remember(firstDateOfMonth, start) {
        YearMonth.from(firstDateOfMonth).lengthOfMonth() + start
    }
    val rows = remember(days) {
        ceil(days.toFloat() / DAYS_OF_WEEK).toInt()
    }

    var itemDate = firstDateOfMonth.minusDays(start)
    val doneLoopsByDate by achievementViewModel.flowsDoneLoopsByDate(
        from = itemDate,
        to = itemDate.plusDays((rows * DAYS_OF_WEEK - 1).toLong())
    ).collectAsState(initial = emptyMap())

    val noDoneLoopsByDate by achievementViewModel.flowsNoDonLoopsByDate(
        from = itemDate,
        to = itemDate.plusDays((rows * DAYS_OF_WEEK - 1).toLong())
    ).collectAsState(initial = emptyMap())

    Column(modifier = modifier) {
        repeat(rows) {
            key(itemDate) {
                CalendarRow(
                    modifier = Modifier.weight(1f),
                    viewMode = viewMode,
                    doneLoopsByDate = doneLoopsByDate,
                    noDoneLoopsByDate = noDoneLoopsByDate,
                    itemDate = itemDate,
                    minDate = minDate,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            }
            itemDate = itemDate.plusWeeks(1)
        }
    }
}

@Composable
private fun CalendarRow(
    modifier: Modifier = Modifier,
    viewMode: DailyAchievementPageViewMode,
    doneLoopsByDate: Map<LocalDate, List<LoopByDate>>,
    noDoneLoopsByDate: Map<LocalDate, List<LoopByDate>>,
    itemDate: LocalDate,
    minDate: LocalDate,
    selectedDate: LocalDate,
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
                    viewMode = viewMode,
                    doneLoops = doneLoopsByDate[itDate] ?: emptyList(),
                    noDoneLoops = noDoneLoopsByDate[itDate] ?: emptyList(),
                    itemDate = itDate,
                    isInterest = isInterest,
                    isToday = itDate == LocalDate.now(),
                    isSelected = itDate == selectedDate,
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
    viewMode: DailyAchievementPageViewMode,
    doneLoops: List<LoopByDate>,
    noDoneLoops: List<LoopByDate>,
    itemDate: LocalDate,
    isInterest: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    // 회고 여부는 배지 우상단 코너 마커로 넘겨, 더 이상 세로로 한 줄을 차지하지 않는다.
    val hasRetrospect = doneLoops.any { it.retrospect != null } ||
            noDoneLoops.any { it.retrospect != null }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(all = 2.dp)
            .clip(RoundShapes.medium)
            .achievementHeat(
                // 두 뷰 모드 모두에서 히트맵을 칠해, 기본(색 도트) 화면에서도 한 달의 흐름이 색으로 읽히게 한다.
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

        if (isInterest) {
            AchievementIndicators(
                modifier = Modifier.padding(top = 3.dp),
                viewMode = viewMode,
                doneLoops = doneLoops,
                noDoneLoops = noDoneLoops,
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
        modifier = modifier.padding(top = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
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

@Composable
private fun AchievementIndicators(
    modifier: Modifier = Modifier,
    viewMode: DailyAchievementPageViewMode,
    doneLoops: List<LoopByDate>,
    noDoneLoops: List<LoopByDate>,
) {
    when (viewMode) {
        DailyAchievementPageViewMode.COLOR_DOT -> ColorDotIndicator(
            modifier = modifier,
            doneLoops = doneLoops,
        )

        DailyAchievementPageViewMode.DESCRIPTION_TEXT -> DescriptionTextIndicator(
            modifier = modifier,
            doneCount = doneLoops.size,
            otherCount = noDoneLoops.size,
        )
    }
}

@Composable
private fun DescriptionTextIndicator(
    modifier: Modifier = Modifier,
    doneCount: Int,
    otherCount: Int,
) {
    Text(
        modifier = modifier,
        text = "$doneCount/${doneCount + otherCount}",
        style = AppTypography.labelMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.5f)
        )
    )
}

@Composable
private fun ColorDotIndicator(
    modifier: Modifier = Modifier,
    doneLoops: List<LoopByDate>,
) {
    // 완료한 루프가 없으면 배경판만 덩그러니 남지 않도록 아무것도 그리지 않는다.
    if (doneLoops.isEmpty()) return

    Row(
        modifier = modifier
            .padding(top = 2.dp)
            // 히트 배경 위에서도 도트 색이 선명하도록, 도트 묶음 뒤에 불투명 배경판(알약)을 깐다.
            // surfaceContainer는 합성된 불투명색이라 어떤 히트 농도도 확실히 가린다.
            .clip(CircleShape)
            .background(AppColor.surfaceContainer)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.10f),
                shape = CircleShape,
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        doneLoops.take(MAX_VISIBLE_DOTS).forEach { loop ->
            Box(
                modifier = Modifier
                    .size(5.dp)
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
