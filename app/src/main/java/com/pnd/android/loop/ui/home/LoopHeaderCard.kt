package com.pnd.android.loop.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary

/**
 * Summary card shown at the top of Home: the three headline rates plus a wise saying.
 *
 * The rates follow the 오늘 / 전체 tab pinned above the list ([selectedTab]) — today's
 * figures when Today is active, all-time figures when All is. Navigation to the group /
 * statistics / history pages lives in the home app bar, so this card stays focused on
 * "how am I doing".
 *
 * View-model state is collected here and passed down as plain values so the inner
 * composables stay stateless and easy to read.
 */
@Composable
fun LoopHeaderCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
) {
    val todayRates by loopViewModel.todayRates.collectAsState(initial = LoopRates.Empty)
    val overallRates by loopViewModel.overallRates.collectAsState(initial = LoopRates.Empty)
    val wiseSaying by loopViewModel.wiseSaying.collectAsState(initial = loopViewModel.wiseSayingText)

    val rates = if (selectedTab == HomeTab.TODAY) todayRates else overallRates

    Card(
        modifier = modifier,
        shape = RoundShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = headerContainerColor(),
            contentColor = AppColor.onSurface,
        ),
    ) {
        Column(modifier = Modifier.padding(Dimens.contentPadding)) {
            LoopStatsSummary(rates = rates)

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

// region Rate metrics

/**
 * 활성 스코프의 지표 요약. 달성률 하나만 헤드라인으로 크게 세우고 응답률·스킵률은
 * 오른쪽에 낮은 대비의 보조 지표로 접어, "얼마나 해냈는가"가 한눈에 읽히도록 한다.
 */
@Composable
private fun LoopStatsSummary(
    modifier: Modifier = Modifier,
    rates: LoopRates,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            HeadlineStat(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.done_rate),
                value = rates.doneRate,
            )
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

        DoneProgressBar(
            modifier = Modifier.padding(top = 12.dp),
            fraction = rates.doneRate / 100f,
        )
    }
}

/** 카드의 주인공인 달성률 — 작은 라벨 아래 큰 primary 수치로 시선을 먼저 붙잡는다. */
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

/** Horizontal bar that visualises the active scope's done rate; animates as the tab flips. */
@Composable
private fun DoneProgressBar(
    modifier: Modifier = Modifier,
    fraction: Float,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "doneRateFraction",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundShapes.medium)
            .background(color = AppColor.onSurface.copy(alpha = 0.1f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .height(6.dp)
                .clip(RoundShapes.medium)
                .background(color = AppColor.primary),
        )
    }
}

private fun formatPercent(value: Float): String = String.format("%.1f%%", value)

// endregion

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
