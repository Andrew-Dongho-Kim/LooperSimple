package com.pnd.android.loop.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
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
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.color
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
    Column(modifier = modifier) {
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
                enabled = viewMode == DailyAchievementPageViewMode.DESCRIPTION_TEXT && isInterest,
                doneCount = doneLoops.size,
                totalCount = doneLoops.size + noDoneLoops.size,
                color = AppColor.primary,
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
    val badgeColor = when {
        isToday -> AppColor.primary
        isSelected -> AppColor.primary.copy(alpha = 0.14f)
        else -> Color.Transparent
    }
    val textColor = when {
        isToday -> AppColor.onPrimary
        isSelected -> AppColor.primary
        else -> dayColor
    }
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
 * 달성도(완료 비율)에 비례해 셀 뒤에 옅은 "히트" 배경을 칠한다.
 * 카운트(설명) 뷰 모드에서만 적용되어 한 달이 은은한 성취 히트맵처럼 보이게 한다.
 */
private fun Modifier.achievementHeat(
    enabled: Boolean,
    doneCount: Int,
    totalCount: Int,
    color: Color,
) = drawBehind {
    if (!enabled || totalCount == 0) return@drawBehind

    val doneRate = doneCount.toFloat() / totalCount
    if (doneRate <= 0.1f) return@drawBehind

    val intensity = (0.4f * doneRate) * (0.4f * doneRate)
    drawRoundRect(
        color = color.copy(alpha = intensity),
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
    Row(
        modifier = modifier.padding(top = 2.dp),
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
