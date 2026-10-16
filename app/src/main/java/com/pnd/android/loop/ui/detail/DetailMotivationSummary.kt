package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.HistoryCalculationNote
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surfaceElevated

/** A nearby weekly target leads; recent and lifetime effort remain visible below it. */
@Composable
internal fun MotivationSummaryHeader(loop: LoopBase, stats: DetailStats) {
    val accent = Color(loop.color).compositeOverOnSurface()
    val weekly = stats.weekly
    val remaining = (weekly.target - weekly.done).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.item)) {
        Column {
            Text(stringResource(R.string.detail_routine_eyebrow), style = AppTypography.labelSmall, color = accent)
            Text(loop.title, Modifier.padding(top = 8.dp), style = AppTypography.headlineLarge, color = AppColor.onSurface)
            Text(stringResource(R.string.detail_motivation_caption), Modifier.padding(top = 6.dp),
                style = AppTypography.bodySmall, color = AppColor.onSurface.copy(alpha = 0.65f))
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundShapes.large)
                .background(accent.copy(alpha = 0.08f).compositeOver(AppColor.surfaceElevated))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(DetailSpacing.related),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(when {
                        !loop.enabled -> R.string.detail_motivation_paused
                        weekly.target <= 0 -> R.string.detail_motivation_no_target
                        weekly.isAchieved -> R.string.detail_motivation_achieved
                        remaining == 1 -> R.string.detail_motivation_one_more
                        weekly.done == 0 -> R.string.detail_motivation_start
                        else -> R.string.detail_motivation_keep_going
                    }),
                    modifier = Modifier.weight(1f),
                    style = AppTypography.titleMedium, color = AppColor.onSurface,
                )
                HistoryCalculationNote(stats.hasEstimatedHistory)
            }
            Text(
                if (!loop.enabled) stringResource(R.string.detail_loop_active_off)
                else if (weekly.target <= 0) stringResource(R.string.detail_motivation_no_target_caption)
                else if (weekly.isAchieved) stringResource(R.string.detail_motivation_achieved_caption)
                else stringResource(R.string.detail_motivation_remaining, remaining),
                style = AppTypography.bodySmall, color = AppColor.onSurface.copy(alpha = 0.7f),
            )
            if (weekly.target > 0) {
                val progressLabel = stringResource(if (weekly.hasGoal) R.string.detail_week_goal_label else R.string.detail_kpi_this_week)
                Text(stringResource(R.string.detail_motivation_week_ratio, weekly.done, weekly.target),
                    style = AppTypography.headlineLarge, color = AppColor.onSurface)
                Box(
                    Modifier.fillMaxWidth().height(9.dp).clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f))
                        .semantics {
                            contentDescription = progressLabel
                            progressBarRangeInfo = ProgressBarRangeInfo(weekly.fraction, 0f..1f)
                        },
                ) {
                    if (weekly.fraction > 0f) {
                        Box(Modifier.fillMaxWidth(weekly.fraction).fillMaxHeight().background(accent))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(progressLabel,
                        Modifier.weight(1f), style = AppTypography.bodySmall, color = AppColor.onSurface.copy(alpha = 0.7f))
                    Text(stringResource(R.string.detail_motivation_percent, (weekly.fraction * 100).toInt()),
                        style = AppTypography.bodySmall, color = AppColor.onSurface.copy(alpha = 0.7f))
                }
            }
            stats.pendingGoal?.let { goal ->
                Text(stringResource(if (goal > 0) R.string.history_goal_next_week else R.string.history_goal_removed_next_week, goal),
                    style = AppTypography.bodySmall, color = AppColor.onSurface.copy(alpha = 0.7f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MotivationMetric(Modifier.weight(1f), stringResource(R.string.detail_motivation_recent), stats.activity.counts.done, accent)
            MotivationMetric(Modifier.weight(1f), stringResource(R.string.detail_kpi_streak), stats.streak.current, accent)
            MotivationMetric(Modifier.weight(1f), stringResource(R.string.detail_motivation_lifetime), stats.doneCount, accent)
        }
    }
}

@Composable
private fun MotivationMetric(modifier: Modifier, label: String, days: Int, accent: Color) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = AppTypography.labelSmall, color = AppColor.onSurface.copy(alpha = 0.65f))
        Text(stringResource(R.string.detail_kpi_streak_days, days), style = AppTypography.titleLarge, color = accent)
    }
}

