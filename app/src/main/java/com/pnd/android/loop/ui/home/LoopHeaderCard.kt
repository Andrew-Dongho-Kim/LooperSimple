package com.pnd.android.loop.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import com.pnd.android.loop.ui.home.viewmodel.LoopTrend
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.home.viewmodel.NextLoopInfo
import com.pnd.android.loop.ui.home.viewmodel.TodayLoopTrends
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
import java.time.format.TextStyle as JavaTextStyle

/**
 * Summary card shown at the top of Home.
 *
 * 오늘 / 전체 탭([selectedTab])에 따라 "성격이 다른" 통계를 보여준다.
 *  - 오늘 탭: 지금·다음에 집중 — 오늘 달성률(원형 링) + 다음 루프 + 진행 중·현재 연속.
 *  - 전체 탭: 장기 축적에 집중 — 전체 달성률 + 최장 연속 + 요일별 달성 패턴.
 *
 * 뷰모델 상태는 이 함수에서 모두 collect 하고 하위 컴포저블에는 평범한 값으로만 넘겨,
 * 안쪽 컴포저블은 상태를 모르는 순수 UI로 유지한다.
 */
@Composable
fun LoopHeaderCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
) {
    val todayRates by loopViewModel.todayRates.collectAsState(initial = LoopRates.Empty)
    val overallRates by loopViewModel.overallRates.collectAsState(initial = LoopRates.Empty)
    val streak by loopViewModel.streak.collectAsState(initial = StreakStat(current = 0, longest = 0))
    val weekdayStats by loopViewModel.weekdayStats.collectAsState(initial = emptyList())
    val inProgress by loopViewModel.countInActive.collectAsState(initial = 0)
    val nextLoop by loopViewModel.nextLoop.collectAsState(initial = null)
    val loopTrends by loopViewModel.todayLoopTrends.collectAsState(initial = TodayLoopTrends.Empty)
    val wiseSaying by loopViewModel.wiseSaying.collectAsState(initial = loopViewModel.wiseSayingText)

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
                TodayPager(
                    rates = todayRates,
                    currentStreak = streak.current,
                    inProgress = inProgress,
                    nextLoop = nextLoop,
                    trends = loopTrends,
                )
            } else {
                OverallStats(
                    rates = overallRates,
                    longestStreak = streak.longest,
                    weekdayStats = weekdayStats,
                )
            }

            if (wiseSaying.isNotBlank()) {
                WiseSaying(
                    modifier = Modifier.padding(top = 14.dp),
                    text = wiseSaying,
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

// region 오늘 탭 — 3페이지 뷰페이저(지금·다음 / 잘하고 있는 / 주의가 필요한 루프)

/** 오늘 탭 헤더 페이저의 페이지 수와 고정 높이. 높이는 페이지가 바뀌어도 카드가 출렁이지 않게 고정한다. */
private const val TODAY_PAGE_COUNT = 3
private val TodayPagerHeight = 112.dp

/**
 * 오늘 탭 헤더를 좌우로 넘겨 보는 3페이지 묶음.
 *  0) 지금·다음 — 달성률 링 + 다음 루프 + 진행 중·연속.
 *  1) 잘하고 있는 루프 — 최근 완료율이 높은 오늘 루프.
 *  2) 주의가 필요한 루프 — 최근 놓치고 있는 오늘 루프.
 * 페이지마다 콘텐츠 양이 달라도 카드 높이가 일정하도록 페이저에 고정 높이를 주고, 하단에 점 인디케이터를 둔다.
 */
@Composable
private fun TodayPager(
    modifier: Modifier = Modifier,
    rates: LoopRates,
    currentStreak: Int,
    inProgress: Int,
    nextLoop: NextLoopInfo?,
    trends: TodayLoopTrends,
) {
    val pagerState = rememberPagerState { TODAY_PAGE_COUNT }
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(TodayPagerHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            when (page) {
                0 -> TodayStats(
                    rates = rates,
                    currentStreak = currentStreak,
                    inProgress = inProgress,
                    nextLoop = nextLoop,
                )

                1 -> LoopTrendPage(
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    accent = AppColor.primary,
                    caption = stringResource(id = R.string.header_trend_doing_well),
                    trends = trends.doingWell,
                    positive = true,
                )

                else -> LoopTrendPage(
                    icon = Icons.AutoMirrored.Outlined.TrendingDown,
                    accent = AppColor.error,
                    caption = stringResource(id = R.string.header_trend_need_attention),
                    trends = trends.needAttention,
                    positive = false,
                )
            }
        }
        PagerDots(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp),
            count = TODAY_PAGE_COUNT,
            selected = pagerState.currentPage,
        )
    }
}

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
 * 오늘의 실행 현황. 달성률을 원형 링(완료/전체)으로 세워 성취감을 주고, 옆에는 "지금·다음"에
 * 집중한 정보 — 곧 시작할 다음 루프와, 진행 중·현재 연속 두 인라인 지표 — 를 둔다.
 */
@Composable
private fun TodayStats(
    modifier: Modifier = Modifier,
    rates: LoopRates,
    currentStreak: Int,
    inProgress: Int,
    nextLoop: NextLoopInfo?,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DoneRing(
            fraction = rates.doneRate / 100f,
            centerText = "${rates.doneCount}/${rates.totalCount}",
            caption = stringResource(id = R.string.header_done_caption),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NextLoopStat(nextLoop = nextLoop)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InlineStat(
                    icon = Icons.Outlined.PlayCircle,
                    text = stringResource(id = R.string.header_in_progress, inProgress),
                )
                InlineStat(
                    icon = Icons.Outlined.LocalFireDepartment,
                    text = stringResource(id = R.string.header_streak, currentStreak),
                )
            }
        }
    }
}

/** 헤더 상단의 "다음 루프" 한 줄. 예정된 루프가 없으면 대비를 낮춰 안내 문구만 조용히 보여준다. */
@Composable
private fun NextLoopStat(
    modifier: Modifier = Modifier,
    nextLoop: NextLoopInfo?,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.header_next_loop),
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.55f),
            ),
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = nextLoop?.let { "${it.title} · ${formatRemaining(it.remainingMinutes)}" }
                ?: stringResource(id = R.string.header_next_loop_none),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.titleMedium.copy(
                color = if (nextLoop != null) AppColor.onSurface
                else AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
    }
}

/** 남은 시간을 "N시간 M분 후 / M분 후 / 곧 시작"으로 표기한다. */
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

/** 아이콘 + 한 줄 텍스트의 작은 인라인 지표. 오늘 탭의 진행 중·현재 연속에 쓰인다. */
@Composable
private fun InlineStat(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = AppColor.primary,
        )
        Text(
            modifier = Modifier.padding(start = 5.dp),
            text = text,
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f),
            ),
        )
    }
}

/** 오늘 달성률을 나타내는 원형 게이지. 가운데엔 완료/전체를 적고, 값이 바뀌면 부드럽게 채운다. */
@Composable
private fun DoneRing(
    modifier: Modifier = Modifier,
    fraction: Float,
    centerText: String,
    caption: String,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "doneRingFraction",
    )
    val trackColor = AppColor.onSurface.copy(alpha = 0.1f)
    val progressColor = AppColor.primary

    Box(
        modifier = modifier.size(84.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(84.dp)) {
            val strokeWidth = 8.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // 배경 트랙(전체 원).
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            // 달성률만큼 12시 방향(-90도)부터 시계방향으로 채운다.
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                maxLines = 1,
                style = AppTypography.titleLarge.copy(
                    color = AppColor.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = caption,
                maxLines = 1,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.55f),
                ),
            )
        }
    }
}

/**
 * 추세 페이지(잘하고 있는/주의가 필요한 루프) 공통 레이아웃. 상단 캡션(아이콘+문구) 아래에
 * 루프별 한 줄(제목·최근 점·지표)을 쌓는다. [positive]가 true면 지표를 "연속 N일", false면
 * "N일째 놓침"으로 보여주며, 연속이 짧을 때는 대신 "완료수/기록수"를 쓴다.
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
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                trends.forEach { trend ->
                    TrendRow(
                        title = trend.title,
                        flags = trend.recentDoneFlags,
                        metric = trendMetric(trend = trend, positive = positive),
                        accent = accent,
                    )
                }
            }
        }
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

/** 추세 페이지의 한 줄: 제목(가변폭) + 최근 완료 점 + 우측 지표. */
@Composable
private fun TrendRow(
    modifier: Modifier = Modifier,
    title: String,
    flags: List<Boolean>,
    metric: String,
    accent: Color,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 좌측 강조 바: 이 행(페이지)의 성격(잘함=primary / 주의=error)을 색으로 즉시 구분한다.
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
        )
        TrendSegments(
            modifier = Modifier.padding(start = 10.dp),
            flags = flags,
            accent = accent,
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = metric,
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = accent,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/** 최근 완료 여부를 각진 세그먼트로 나열한다. 채운 칸 = 완료, 옅은 칸 = 미완료. 왼쪽이 과거, 오른쪽이 최신. */
@Composable
private fun TrendSegments(
    modifier: Modifier = Modifier,
    flags: List<Boolean>,
    accent: Color,
) {
    val missColor = AppColor.onSurface.copy(alpha = 0.18f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // recentDoneFlags는 최신→과거 순이므로, 왼쪽부터 과거가 오도록 뒤집어 그린다.
        flags.reversed().forEach { done ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (done) accent else missColor),
            )
        }
    }
}

// endregion

// region 전체 탭 — 헤드라인 + 최장 연속 + 요일 패턴 (시안 D)

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

// region Wise saying

@Composable
private fun WiseSaying(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = AppTypography.bodySmall.copy(
            color = AppColor.onSurface.copy(alpha = 0.55f),
            fontStyle = FontStyle.Italic,
        ),
    )
}

// endregion
