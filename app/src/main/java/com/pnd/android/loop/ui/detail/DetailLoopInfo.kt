package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.toLocalDate
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

/** 자주 바꾸지 않는 부가 정보는 더보기에서 확인한다. 겹침 조회는 창을 열었을 때만 수행한다. */
@Composable
internal fun LoopInformationDialog(
    loop: LoopBase,
    today: LocalDate,
    onLoadOverlapCount: suspend (LoopBase) -> Int,
    onDismiss: () -> Unit,
) {
    val createdDate = loop.created.toLocalDate()
    val elapsedDays = (today.toEpochDay() - createdDate.toEpochDay() + 1).coerceAtLeast(1)
    var overlappingCount by remember(loop) { mutableStateOf<Int?>(null) }
    var loadFailed by remember(loop) { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }

    LaunchedEffect(loop, retry) {
        loadFailed = false
        overlappingCount = null
        try {
            overlappingCount = onLoadOverlapCount(loop)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColor.surfaceElevated,
        titleContentColor = AppColor.onSurface,
        title = { Text(stringResource(R.string.detail_loop_info)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DetailSpacing.group),
            ) {
                LoopInfoValue(
                    label = stringResource(R.string.created_date),
                    value = createdDate.formatYearMonthDateDays() + " · " + stringResource(R.string.n_days, elapsedDays),
                )
                Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.related)) {
                    LoopInfoValue(
                        label = stringResource(R.string.detail_overlap_label),
                        value = when {
                            loadFailed -> stringResource(R.string.detail_schedule_check_failed)
                            overlappingCount == null -> stringResource(R.string.detail_schedule_checking)
                            overlappingCount == 0 -> stringResource(R.string.detail_overlap_none)
                            else -> stringResource(R.string.detail_overlap_count, overlappingCount ?: 0)
                        },
                    )
                    if (loadFailed) {
                        TextActionButton(
                            text = stringResource(R.string.detail_journal_retry),
                            onClick = { retry++ },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextActionButton(text = stringResource(R.string.detail_loop_info_close), onClick = onDismiss)
        },
    )
}

@Composable
private fun LoopInfoValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.related)) {
        Text(label, style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.65f)))
        Text(value, style = AppTypography.bodyMedium.copy(color = AppColor.onSurface))
    }
}
