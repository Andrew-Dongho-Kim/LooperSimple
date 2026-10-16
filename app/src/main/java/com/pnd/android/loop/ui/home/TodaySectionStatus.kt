package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.doneState
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant

internal data class TodaySectionProgress(val done: Int, val skipped: Int, val remaining: Int) {
    val total: Int get() = done + skipped + remaining
    val isFinished: Boolean get() = total > 0 && remaining == 0
}

/** Yesterday's waiting occurrences remain actionable but are not today's progress. */
internal val Section.Today.progress: TodaySectionProgress
    get() {
        val loops = items.value
        val done = loops.count { it.doneState == DoneState.DONE }
        val skipped = loops.count { it.doneState == DoneState.SKIP }
        val remaining = maxOf(
            loops.size - done - skipped,
            groups.value.sumOf { group -> group.occurrences.count { !it.isCarriedOver } },
        )
        return TodaySectionProgress(done, skipped, remaining)
    }

internal val Section.Today.isFinished: Boolean get() = progress.isFinished

@Composable
internal fun TodayRemainingEmpty(progress: TodaySectionProgress) {
    AppCard(
        modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.today_remaining_empty),
                style = AppTypography.titleMedium,
                color = AppColor.onSurface,
            )
            Text(
                stringResource(if (progress.done == 0) R.string.today_remaining_skipped_hint else R.string.today_remaining_empty_hint),
                modifier = Modifier.padding(top = 6.dp),
                style = AppTypography.bodySmall,
                color = AppColor.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun TodayRecordsHeader(count: Int, onNavigateToHistoryPage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.today_finished_records), style = AppTypography.titleMedium, color = AppColor.onSurface)
            Text(stringResource(R.string.today_finished_record_count, count), style = AppTypography.bodySmall, color = AppColor.onSurfaceVariant)
        }
        TextButton(onClick = onNavigateToHistoryPage) {
            Text(stringResource(R.string.today_records_history))
        }
    }
}

@Composable
internal fun TodayRecordsEmpty() {
    AppCard(modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding)) {
        Text(stringResource(R.string.today_records_empty), style = AppTypography.bodySmall, color = AppColor.onSurfaceVariant)
    }
}
