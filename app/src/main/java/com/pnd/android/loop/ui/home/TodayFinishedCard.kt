package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.doneState
import com.pnd.android.loop.data.history.CompletionCounts
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.primarySurface
import com.pnd.android.loop.ui.theme.surfaceContainer

/** Carried-over work remains actionable below the summary; it is not today's record. */
internal val Section.Today.isFinished: Boolean
    get() = items.value.isNotEmpty() && items.value.all {
        it.doneState == DoneState.DONE || it.doneState == DoneState.SKIP
    } && groups.value.none { group -> group.occurrences.any { !it.isCarriedOver } }

@Composable
internal fun TodayFinishedCard(
    loops: List<LoopBase>,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val doneCount = loops.count { it.doneState == DoneState.DONE }
    val skipCount = loops.size - doneCount
    val percent = CompletionCounts(done = doneCount, skipped = skipCount).completionPercent ?: 0
    Column(
        modifier = Modifier.padding(
            horizontal = Dimens.screenHorizontalPadding,
            vertical = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AppCard(color = AppColor.primarySurface) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    tint = AppColor.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(when {
                            doneCount == loops.size -> R.string.today_finished_all_done
                            doneCount == 0 -> R.string.today_finished_all_skipped
                            else -> R.string.today_finished_mixed
                        }),
                        style = AppTypography.titleMedium,
                        color = AppColor.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.today_finished_counts, doneCount, skipCount),
                        style = AppTypography.bodySmall,
                        color = AppColor.onSurfaceVariant,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.today_finished_records),
                    style = AppTypography.titleMedium,
                    color = AppColor.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.today_finished_record_count, loops.size),
                    style = AppTypography.bodySmall,
                    color = AppColor.onSurfaceVariant,
                )
            }
            TodayFinishedRecords(
                loops = loops,
                blurState = blurState,
                loopViewModel = loopViewModel,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
        }
        AppCard(color = waterCardColor(), contentPadding = PaddingValues(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.tree_today_water),
                    style = AppTypography.bodyMedium,
                    color = AppColor.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.tree_water_percent, percent),
                    style = AppTypography.titleMedium,
                    color = AppColor.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { doneCount.toFloat() / loops.size },
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                color = AppColor.primary,
                trackColor = AppColor.surfaceContainer,
            )
            Text(
                text = stringResource(R.string.today_finished_water_hint, doneCount),
                style = AppTypography.bodySmall,
                color = AppColor.onSurfaceVariant,
            )
        }
    }
}
