package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.viewmodel.CurrentLoopInfo
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import com.pnd.android.loop.ui.home.viewmodel.LoopTrend
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.home.viewmodel.NextLoopInfo
import com.pnd.android.loop.ui.home.viewmodel.LoopTrends
import com.pnd.android.loop.ui.statisctics.DayOfWeekStat
import com.pnd.android.loop.ui.statisctics.StreakStat
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.secondary
import java.time.DayOfWeek
import java.util.Locale
import kotlin.math.roundToInt
import java.time.format.TextStyle as JavaTextStyle

/**
 * Summary card shown at the top of Home.
 *
 * 오늘 / 전체 탭([selectedTab])에 따라 "성격이 다른" 통계를 보여준다.
 *  - 오늘 탭: 지금에 집중 — 큰 달성률 + 연속 배지 + 최근 7일 스트립 + 진행 중(없으면 다음) 루프. (단일 화면)
 *  - 전체 탭: 장기 축적에 집중 — 3페이지 페이저(축적 요약 / 잘하고 있는 루프 / 주의가 필요한 루프).
 *
 * 뷰모델 상태는 이 함수에서 모두 collect 하고 하위 컴포저블에는 평범한 값으로만 넘겨,
 * 안쪽 컴포저블은 상태를 모르는 순수 UI로 유지한다.
 */
@Composable
fun LoopHeaderCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val todayRates by loopViewModel.todayRates.collectAsState(initial = LoopRates.Empty)
    val overallRates by loopViewModel.overallRates.collectAsState(initial = LoopRates.Empty)
    val streak by loopViewModel.streak.collectAsState(
        initial = StreakStat(
            current = 0,
            longest = 0
        )
    )
    val weekdayStats by loopViewModel.weekdayStats.collectAsState(initial = emptyList())
    val nextLoop by loopViewModel.nextLoop.collectAsState(initial = null)
    val currentLoop by loopViewModel.currentLoop.collectAsState(initial = null)
    val recentDays by loopViewModel.recentDailyDone.collectAsState(initial = emptyList())
    val loopTrends by loopViewModel.loopTrends.collectAsState(initial = LoopTrends.Empty)
    val loops by loopViewModel.allLoopsWithDoneStates.collectAsState(initial = emptyList())

    Card(
        modifier = modifier,
        shape = RoundShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = headerContainerColor(),
            contentColor = AppColor.onSurface,
        ),
    ) {
        Column(modifier = Modifier.padding(Dimens.contentPadding)) {
            if (selectedTab == HomeTab.TODAY) {
                // 오늘 탭: 지금·다음에 집중한 단일 요약(페이저 없음).
                TodayHeroStats(
                    rates = todayRates,
                    currentStreak = streak.current,
                    recentDays = recentDays,
                    currentLoop = currentLoop,
                    nextLoop = nextLoop,
                )
            } else {
                // 전체 탭: 축적 요약 + 잘하고 있는/주의가 필요한 루프를 좌우로 넘겨 본다.
                OverallPager(
                    rates = overallRates,
                    longestStreak = streak.longest,
                    weekdayStats = weekdayStats,
                    trends = loopTrends,
                    // 추세의 loopId만 알고 있으므로, 현재 루프 목록에서 해당 루프를 찾아 상세로 넘긴다.
                    onCheckLoop = { loopId ->
                        loops.firstOrNull { loop -> loop.loopId == loopId }
                            ?.let(onNavigateToDetailPage)
                    },
                )
            }
        }
    }
}

/** Soft primary tint that stays subtle on light and lifts the card on dark. */
@Composable
private fun headerContainerColor(): Color =
    AppColor.primary.compositeOverSurface(
        alpha = if (isSystemInDarkTheme()) 0.20f else 0.08f
    )

// region 헤더 공용 — 오늘 요약 · 추세 · 페이저 인디케이터

/** 페이저 하단 인디케이터. 현재 페이지만 강조 색·길쭉한 점으로 표시한다. */
@Composable
private fun PagerDots(
    modifier: Modifier = Modifier,
    count: Int,
    selected: Int,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (isSelected) 16.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AppColor.primary
                        else AppColor.onSurface.copy(alpha = 0.2f)
                    ),
            )
        }
    }
}

/**
 * 오늘 탭 1페이지(시안 B). 오늘 달성률을 큰 숫자 헤드라인으로 세워 성취감을 주고, 옆에 현재 연속
 * 배지·완료 수를 둔다. 그 아래 최근 7일 스트립으로 "요즘 꾸준한지"를, 맨 아래 한 줄로 "지금 무엇을
 * 하고 있는지"(진행 중, 없으면 다음)를 보여준다.
 */
@Composable
private fun TodayHeroStats(
    modifier: Modifier = Modifier,
    rates: LoopRates,
    currentStreak: Int,
    recentDays: List<Boolean>,
    currentLoop: CurrentLoopInfo?,
    nextLoop: NextLoopInfo?,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            // 달성률 헤드라인: 정수 % + 작은 퍼센트 기호.
            Text(
                text = "${rates.doneRate.roundToInt()}",
                maxLines = 1,
                style = AppTypography.displayMedium.copy(
                    color = AppColor.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                modifier = Modifier.padding(start = 2.dp, bottom = 5.dp),
                text = "%",
                maxLines = 1,
                style = AppTypography.titleMedium.copy(
                    color = AppColor.primary.copy(alpha = 0.7f),
                ),
            )
            Column(
                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                StreakChip(streak = currentStreak)
                Text(
                    text = "${rates.doneCount} / ${rates.totalCount} ${stringResource(id = R.string.header_done_caption)}",
                    maxLines = 1,
                    style = AppTypography.bodySmall.copy(
                        color = AppColor.onSurface.copy(alpha = 0.55f),
                    ),
                )
            }
        }

        if (recentDays.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SegmentStrip(flags = recentDays, accent = AppColor.primary)
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    text = stringResource(id = R.string.header_recent_days),
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    style = AppTypography.bodySmall.copy(
                        color = AppColor.onSurface.copy(alpha = 0.45f),
                    ),
                )
            }
        }

        FocusLine(
            modifier = Modifier.padding(top = 14.dp),
            currentLoop = currentLoop,
            nextLoop = nextLoop,
        )
    }
}

/** 현재 연속 달성 배지. 연속이 1일 이상일 때만 불꽃 아이콘과 함께 조용히 보여준다. */
@Composable
private fun StreakChip(
    modifier: Modifier = Modifier,
    streak: Int,
) {
    if (streak <= 0) return
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(AppColor.primary.compositeOverSurface(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(13.dp),
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = AppColor.primary,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = stringResource(id = R.string.header_streak, streak),
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.primary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * 1페이지 맨 아래 한 줄. 진행 중인 루프가 있으면 "진행 중 · 제목 N분 남음 (외 N개)"를, 없으면
 * "다음 루프 · 제목 N분 후"를, 그마저 없으면 안내 문구를 대비를 낮춰 보여준다.
 */
@Composable
private fun FocusLine(
    modifier: Modifier = Modifier,
    currentLoop: CurrentLoopInfo?,
    nextLoop: NextLoopInfo?,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            currentLoop != null -> {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = AppColor.primary,
                )
                FocusText(
                    modifier = Modifier.weight(1f),
                    prefix = stringResource(id = R.string.header_current_prefix),
                    title = currentLoop.title,
                    trailing = formatEnding(currentLoop.remainingMinutes),
                    // 함께 진행 중인 다른 루프가 있으면 "외 N개"를 옅게 덧붙인다.
                    extra = currentLoop.othersCount
                        .takeIf { it > 0 }
                        ?.let { stringResource(id = R.string.header_current_others, it) },
                )
            }

            nextLoop != null -> FocusText(
                modifier = Modifier.weight(1f),
                prefix = stringResource(id = R.string.header_next_loop),
                title = nextLoop.title,
                trailing = formatRemaining(nextLoop.remainingMinutes),
                extra = null,
            )

            else -> Text(
                text = stringResource(id = R.string.header_next_loop_none),
                maxLines = 1,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

/** 포커스 라인의 텍스트 조각: 접두(진행 중/다음) · 제목(가변폭) · 남은 시간, 그리고 선택적 "외 N개". */
@Composable
private fun FocusText(
    modifier: Modifier = Modifier,
    prefix: String,
    title: String,
    trailing: String,
    extra: String?,
) {
    Row(
        modifier = modifier.padding(start = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$prefix · ",
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.55f),
            ),
        )
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = trailing,
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f),
            ),
        )
        if (extra != null) {
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = extra,
                maxLines = 1,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** 남은 시간을 "N시간 M분 후 / M분 후 / 곧 시작"으로 표기한다(다음 루프용). */
@Composable
private fun formatRemaining(minutes: Long): String = when {
    minutes <= 0L -> stringResource(id = R.string.header_next_loop_soon)
    minutes >= 60L -> stringResource(
        id = R.string.header_next_after_hm,
        (minutes / 60L).toInt(),
        (minutes % 60L).toInt(),
    )

    else -> stringResource(id = R.string.header_next_after_m, minutes.toInt())
}

/** 종료까지 남은 시간을 "N시간 M분 남음 / M분 남음 / 곧 종료"으로 표기한다(진행 중 루프용). */
@Composable
private fun formatEnding(minutes: Long): String = when {
    minutes <= 0L -> stringResource(id = R.string.header_current_remaining_soon)
    minutes >= 60L -> stringResource(
        id = R.string.header_current_remaining_hm,
        (minutes / 60L).toInt(),
        (minutes % 60L).toInt(),
    )

    else -> stringResource(id = R.string.header_current_remaining_m, minutes.toInt())
}

/**
 * 추세 페이지(잘하고 있는/주의가 필요한 루프). 상단 캡션(아이콘+문구) 아래에 대표 루프 하나를
 * 강조 블록으로 크게 세우고([TrendHero]), 나머지는 한 줄짜리 미니 행([TrendMiniRow])으로 쌓는다.
 * [onCheckLoop]가 주어지면(주의가 필요한 루프 페이지) 대표 블록에 "확인하기" 버튼을 달아 상세로 보낸다.
 * 표시할 루프가 없으면(표본 부족 포함) 안내 문구만 조용히 보여준다.
 */
@Composable
private fun LoopTrendPage(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    accent: Color,
    caption: String,
    trends: List<LoopTrend>,
    positive: Boolean,
    onCheckLoop: ((Int) -> Unit)?,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = icon,
                contentDescription = null,
                tint = accent,
            )
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = caption,
                maxLines = 1,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.6f),
                ),
            )
        }

        if (trends.isEmpty()) {
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(id = R.string.header_trend_empty),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.45f),
                ),
            )
        } else {
            val hero = trends.first()
            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendHero(
                    trend = hero,
                    accent = accent,
                    positive = positive,
                    onCheck = onCheckLoop?.let { navigate -> { navigate(hero.loopId) } },
                )
                trends.drop(1).forEach { trend ->
                    TrendMiniRow(
                        trend = trend,
                        accent = accent,
                        positive = positive,
                    )
                }
            }
        }
    }
}

/**
 * 추세 대표 루프 강조 블록. 강조 색 틴트 위에 제목·지표를 한 줄로 두고, 아래에 최근 완료 스트립을
 * 놓는다. [onCheck]가 있으면 우측에 "확인하기" 버튼을 달아 상세로 이동한다.
 */
@Composable
private fun TrendHero(
    modifier: Modifier = Modifier,
    trend: LoopTrend,
    accent: Color,
    positive: Boolean,
    onCheck: (() -> Unit)?,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.compositeOverSurface(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = trend.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = trendMetric(trend = trend, positive = positive),
                maxLines = 1,
                style = AppTypography.bodySmall.copy(
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            if (onCheck != null) {
                CheckButton(
                    modifier = Modifier.padding(start = 8.dp),
                    accent = accent,
                    onClick = onCheck,
                )
            }
        }
        SegmentStrip(
            modifier = Modifier.padding(top = 8.dp),
            // recentDoneFlags는 최신→과거 순이므로 과거→최신으로 뒤집어 왼쪽부터 그린다.
            flags = trend.recentDoneFlags.reversed(),
            accent = accent,
        )
    }
}

/** "확인하기" 알약 버튼. 강조 색 위에 문구와 화살표를 얹어 상세로의 이동을 나타낸다. */
@Composable
private fun CheckButton(
    modifier: Modifier = Modifier,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(accent.compositeOverSurface(alpha = 0.18f))
            .clickable { onClick() }
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.header_trend_check),
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = accent,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = accent,
        )
    }
}

/** 추세 페이지의 2·3위 미니 행: 좌측 강조 틱 + 제목(가변폭) + 우측 지표. */
@Composable
private fun TrendMiniRow(
    modifier: Modifier = Modifier,
    trend: LoopTrend,
    accent: Color,
    positive: Boolean,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent.copy(alpha = 0.6f)),
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            text = trend.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = trendMetric(trend = trend, positive = positive),
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )
    }
}

/** 추세 지표 문구. 연속(완료/놓침)이 2 이상이면 그 연속을, 아니면 "완료수/기록수"를 보여준다. */
@Composable
private fun trendMetric(trend: LoopTrend, positive: Boolean): String = when {
    positive && trend.currentStreak >= 2 ->
        stringResource(id = R.string.header_streak, trend.currentStreak)

    !positive && trend.currentMiss >= 2 ->
        stringResource(id = R.string.header_trend_missed, trend.currentMiss)

    else -> "${trend.doneCount}/${trend.totalCount}"
}

/**
 * 최근 완료 여부를 각진 세그먼트로 나열한다. 채운 칸 = 완료, 옅은 칸 = 미완료.
 * [flags]는 과거→최신 순서이며, 왼쪽부터 그대로 그려 왼쪽이 과거·오른쪽이 최신이 되게 한다.
 */
@Composable
private fun SegmentStrip(
    modifier: Modifier = Modifier,
    flags: List<Boolean>,
    accent: Color,
    segment: Dp = 9.dp,
) {
    val missColor = AppColor.onSurface.copy(alpha = 0.18f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        flags.forEach { done ->
            Box(
                modifier = Modifier
                    .size(segment)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (done) accent else missColor),
            )
        }
    }
}

// endregion

// region 전체 탭 — 축적 요약 + 잘하고 있는/주의가 필요한 루프 (3페이지 뷰페이저)

/** 전체 탭 헤더 페이저의 페이지 수와 고정 높이. 높이는 페이지가 바뀌어도 카드가 출렁이지 않게 고정한다. */
private const val OVERALL_PAGE_COUNT = 3
private val OverallPagerHeight = 156.dp

/**
 * 전체 탭 헤더를 좌우로 넘겨 보는 3페이지 묶음.
 *  0) 축적 요약 — 전체 달성률 + 최장 연속 + 응답·스킵률 + 요일별 달성 패턴.
 *  1) 잘하고 있는 루프 — 최근 완료율이 높은 루프.
 *  2) 주의가 필요한 루프 — 최근 놓치고 있는 루프(대표 항목에서 상세로 이동 가능).
 * 페이지마다 콘텐츠 양이 달라도 카드 높이가 일정하도록 페이저에 고정 높이를 주고, 하단에 점 인디케이터를 둔다.
 */
@Composable
private fun OverallPager(
    modifier: Modifier = Modifier,
    rates: LoopRates,
    longestStreak: Int,
    weekdayStats: List<DayOfWeekStat>,
    trends: LoopTrends,
    onCheckLoop: (Int) -> Unit,
) {
    val pagerState = rememberPagerState { OVERALL_PAGE_COUNT }
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(OverallPagerHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            when (page) {
                0 -> OverallStats(
                    rates = rates,
                    longestStreak = longestStreak,
                    weekdayStats = weekdayStats,
                )

                1 -> LoopTrendPage(
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    accent = AppColor.primary,
                    caption = stringResource(id = R.string.header_trend_doing_well),
                    trends = trends.doingWell,
                    positive = true,
                    // 잘하고 있는 루프는 상세 이동 없이 보기만 한다.
                    onCheckLoop = null,
                )

                else -> LoopTrendPage(
                    icon = Icons.AutoMirrored.Outlined.TrendingDown,
                    accent = AppColor.error,
                    caption = stringResource(id = R.string.header_trend_need_attention),
                    trends = trends.needAttention,
                    positive = false,
                    onCheckLoop = onCheckLoop,
                )
            }
        }
        PagerDots(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp),
            count = OVERALL_PAGE_COUNT,
            selected = pagerState.currentPage,
        )
    }
}

/**
 * 전체 기간의 축적을 보여준다. 왼쪽엔 전체 달성률 헤드라인과 최장 연속 배지를, 오른쪽엔
 * 응답률·스킵률을 두고, 아래에는 요일별 달성 패턴 막대로 "어느 요일에 꾸준한가"를 드러낸다.
 */
@Composable
private fun OverallStats(
    modifier: Modifier = Modifier,
    rates: LoopRates,
    longestStreak: Int,
    weekdayStats: List<DayOfWeekStat>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                HeadlineStat(
                    label = stringResource(id = R.string.done_rate),
                    value = rates.doneRate,
                )
                StreakBadge(
                    modifier = Modifier.padding(top = 8.dp),
                    label = stringResource(id = R.string.stat_streak_longest),
                    days = longestStreak,
                )
            }
            SecondaryStat(
                label = stringResource(id = R.string.response_rate),
                value = rates.responseRate,
            )
            SecondaryStat(
                modifier = Modifier.padding(start = 20.dp),
                label = stringResource(id = R.string.skip_rate),
                value = rates.skipRate,
            )
        }

        if (weekdayStats.size == DayOfWeek.entries.size) {
            WeekdayPattern(
                modifier = Modifier.padding(top = 16.dp),
                stats = weekdayStats,
            )
        }
    }
}

/** 전체 탭의 주인공인 달성률 — 작은 라벨 아래 큰 primary 수치로 시선을 먼저 붙잡는다. */
@Composable
private fun HeadlineStat(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.55f),
            ),
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = formatPercent(value),
            maxLines = 1,
            style = AppTypography.headlineMedium.copy(
                color = AppColor.primary,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** 보조 지표(응답률·스킵률) — 값·라벨 모두 대비를 낮춰 헤드라인 옆에 조용히 놓는다. */
@Composable
private fun SecondaryStat(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = formatPercent(value),
            maxLines = 1,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = label,
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.45f),
            ),
        )
    }
}

/** 최장 연속 기록 배지 — 트로피 아이콘과 함께 "최장 연속 N일"을 강조 색으로 보여준다. */
@Composable
private fun StreakBadge(
    modifier: Modifier = Modifier,
    label: String,
    days: Int,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = AppColor.secondary,
        )
        Text(
            modifier = Modifier.padding(start = 5.dp),
            text = "$label ${stringResource(id = R.string.stat_streak_days, days)}",
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f),
            ),
        )
    }
}

/**
 * 요일별(월~일) 달성 패턴 막대. 각 막대의 높이는 가장 꾸준한 요일 대비 비율([DayOfWeekStat.ratio])
 * 이고, 최고 요일만 primary로 강조해 "언제 가장 잘 지키는지"가 바로 읽히도록 한다.
 */
@Composable
private fun WeekdayPattern(
    modifier: Modifier = Modifier,
    stats: List<DayOfWeekStat>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 막대: 바닥을 맞춰 한 줄에 배치.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            stats.forEach { stat ->
                val isBest = stat.completedCount > 0 && stat.ratio >= 1f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((5 + 25 * stat.ratio).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isBest) AppColor.primary
                            else AppColor.primary.copy(alpha = 0.35f)
                        ),
                )
            }
        }
        // 라벨: 막대와 같은 weight로 나눠 열을 정렬.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            stats.forEach { stat ->
                val isBest = stat.completedCount > 0 && stat.ratio >= 1f
                Text(
                    modifier = Modifier.weight(1f),
                    text = stat.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, Locale.getDefault()),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = AppTypography.bodySmall.copy(
                        color = if (isBest) AppColor.primary
                        else AppColor.onSurface.copy(alpha = 0.5f),
                    ),
                )
            }
        }
    }
}

// endregion

private fun formatPercent(value: Float): String = String.format("%.1f%%", value)