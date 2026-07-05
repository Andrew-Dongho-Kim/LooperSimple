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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PendingActions
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
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.statisctics.DayOfWeekStat
import com.pnd.android.loop.ui.statisctics.StreakStat
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.secondary
import com.pnd.android.loop.ui.theme.surfaceContainer
import java.time.DayOfWeek
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

/**
 * Summary card shown at the top of Home.
 *
 * 오늘 / 전체 탭([selectedTab])에 따라 "성격이 다른" 통계를 보여준다.
 *  - 오늘 탭: 지금 실행에 집중 — 오늘 달성률(원형 링) + 현재 연속 + 남은 오늘 루프.
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
    val remainingToday by loopViewModel.countInTodayRemain.collectAsState(initial = 0)
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
                TodayStats(
                    rates = todayRates,
                    currentStreak = streak.current,
                    remainingToday = remainingToday,
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

// region 오늘 탭 — 원형 링 + 현재 연속 + 남은 루프 (시안 A)

/**
 * 오늘의 실행 현황. 달성률을 원형 링으로 크게 세워 성취감을 주고, 옆에는 오늘에만 의미 있는
 * 두 지표(현재 연속·남은 루프)를 둔다. 응답률/스킵률은 하단에 보조 pill로 접는다.
 */
@Composable
private fun TodayStats(
    modifier: Modifier = Modifier,
    rates: LoopRates,
    currentStreak: Int,
    remainingToday: Int,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DoneRing(
                fraction = rates.doneRate / 100f,
                percentText = formatPercent(rates.doneRate),
                caption = stringResource(id = R.string.today_done_rate),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconStat(
                    icon = Icons.Outlined.LocalFireDepartment,
                    iconTint = AppColor.primary,
                    value = stringResource(id = R.string.stat_streak_days, currentStreak),
                    label = stringResource(id = R.string.stat_streak_current),
                )
                IconStat(
                    icon = Icons.Outlined.PendingActions,
                    iconTint = AppColor.primary,
                    value = stringResource(id = R.string.header_remaining_count, remainingToday),
                    label = stringResource(id = R.string.header_remaining_today),
                )
            }
        }

        Row(modifier = Modifier.padding(top = 16.dp)) {
            RatePill(
                label = stringResource(id = R.string.response_rate),
                value = rates.responseRate,
            )
            RatePill(
                modifier = Modifier.padding(start = 8.dp),
                label = stringResource(id = R.string.skip_rate),
                value = rates.skipRate,
            )
        }
    }
}

/** 오늘 달성률을 나타내는 원형 게이지. 탭이 바뀌면 채움 정도가 부드럽게 애니메이션된다. */
@Composable
private fun DoneRing(
    modifier: Modifier = Modifier,
    fraction: Float,
    percentText: String,
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
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
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
                text = percentText,
                maxLines = 1,
                style = AppTypography.headlineSmall.copy(
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

/** 아이콘 + 수치 + 라벨로 구성한 한 줄 지표. 오늘 탭의 현재 연속·남은 루프에 쓰인다. */
@Composable
private fun IconStat(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = value,
                maxLines = 1,
                style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
            )
            Text(
                text = label,
                maxLines = 1,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

/** 응답률/스킵률처럼 조용히 놓는 보조 지표. 라벨과 값을 한 알약(pill) 안에 담는다. */
@Composable
private fun RatePill(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColor.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        text = "$label ${formatPercent(value)}",
        maxLines = 1,
        style = AppTypography.bodySmall.copy(
            color = AppColor.onSurface.copy(alpha = 0.7f),
        ),
    )
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
