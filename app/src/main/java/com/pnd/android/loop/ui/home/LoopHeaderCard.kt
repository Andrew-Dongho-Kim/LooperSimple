package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary

/**
 * Summary card shown at the top of Home: today's rate metrics and a wise saying.
 *
 * Navigation to the group / statistics / history pages lives in the home app bar
 * (see HomeAppBar) so this card stays focused on "how am I doing today".
 *
 * View-model state is collected here and passed down as plain values so the inner
 * composables stay stateless and easy to read.
 */
@Composable
fun LoopHeaderCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    val todayDoneRate by loopViewModel.todayDoneRate.collectAsState(initial = 0f)
    val responseRate by loopViewModel.allResponseRate.collectAsState(initial = 0f)
    val doneRate by loopViewModel.doneRate.collectAsState(initial = 0f)
    val skipRate by loopViewModel.skipRate.collectAsState(initial = 0f)
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
            LoopStatsSummary(
                todayDoneRate = todayDoneRate,
                doneRate = doneRate,
                responseRate = responseRate,
                skipRate = skipRate,
            )

            if (wiseSaying.isNotBlank()) {
                WiseSaying(
                    modifier = Modifier.padding(top = 10.dp),
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
 * Compact "how am I doing today" summary. All four rates sit in a single row of equal
 * columns — today's done rate highlighted, the all-time figures muted — over one thin
 * progress bar for today. Folding everything into one row keeps the card half its former
 * height so the loop list below gets the screen it needs.
 */
@Composable
private fun LoopStatsSummary(
    modifier: Modifier = Modifier,
    todayDoneRate: Float,
    doneRate: Float,
    responseRate: Float,
    skipRate: Float,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatColumn(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.today_done_rate),
                value = todayDoneRate,
                highlighted = true,
            )
            VerticalStatsDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.total_done_rate),
                value = doneRate,
            )
            VerticalStatsDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.response_rate),
                value = responseRate,
            )
            VerticalStatsDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.skip_rate),
                value = skipRate,
            )
        }

        DoneProgressBar(
            modifier = Modifier.padding(top = 10.dp),
            fraction = todayDoneRate / 100f,
        )
    }
}

/**
 * A single "value over label" stat, centred within its slot. The [highlighted] figure
 * (today's done rate) is tinted with [primary] to stand out among the muted all-time ones.
 */
@Composable
private fun StatColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    highlighted: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatPercent(value),
            maxLines = 1,
            style = AppTypography.titleMedium.copy(
                color = if (highlighted) {
                    AppColor.primary
                } else {
                    AppColor.onSurface.copy(alpha = 0.9f)
                },
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
            ),
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = label,
            maxLines = 1,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.55f),
            ),
        )
    }
}

/**
 * Hairline separator between stat columns, drawn from [onSurface] at low alpha so it stays
 * subtle on both themes: a faint dark line on light backgrounds, a faint light one on dark.
 */
@Composable
private fun VerticalStatsDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(28.dp)
            .width(1.dp)
            .background(color = dividerColor()),
    )
}

private const val DIVIDER_ALPHA = 0.1f

@Composable
private fun dividerColor(): Color = AppColor.onSurface.copy(alpha = DIVIDER_ALPHA)

/** Horizontal bar that visualises today's done rate at a glance. */
@Composable
private fun DoneProgressBar(
    modifier: Modifier = Modifier,
    fraction: Float,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundShapes.medium)
            .background(color = AppColor.onSurface.copy(alpha = 0.1f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
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
