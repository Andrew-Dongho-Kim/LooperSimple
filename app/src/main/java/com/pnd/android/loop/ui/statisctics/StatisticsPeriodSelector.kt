package com.pnd.android.loop.ui.statisctics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.formatYearMonth
import java.time.LocalDate
import java.time.YearMonth

/**
 * 월별 탭에 표시할 월 이름. 올해 월은 "10월"처럼 짧게, 다른 해의 월은 "25년 3월"처럼 연도를 붙인다.
 * 접힌 헤더의 좁은 탭 칸에도 들어가도록 연도는 두 자리만 쓴다.
 */
@Composable
internal fun statisticsMonthTabLabel(month: YearMonth, today: LocalDate): String {
    val monthName = stringResource(ABB_MONTHS[month.monthValue - 1])
    return if (month.year == today.year) {
        monthName
    } else {
        stringResource(R.string.stat_tab_month_other_year, month.year % 100, monthName)
    }
}

/**
 * 본문 맨 위에서 지금 보고 있는 기간의 범위를 알려 주는 한 줄 안내.
 *
 * 기간 선택은 헤더 탭([CollapsingStatisticsHeader])이 맡고, 여기는 수치를 해석하는 데 필요한
 * 정보만 남긴다. 특히 이번 달은 아직 진행 중이라 "오늘까지"를 밝혀 둬야 완료 수를 지난달과
 * 그대로 비교하지 않는다.
 */
@Composable
internal fun StatisticsPeriodCaption(
    period: StatisticsPeriod,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val text = when (period) {
        StatisticsPeriod.Total -> stringResource(R.string.stat_period_all_description)
        is StatisticsPeriod.Month -> {
            val month = period.yearMonth
            val isCurrentMonth = month == YearMonth.from(today)
            // 다른 해의 월은 탭 라벨처럼 연도까지 밝혀 어느 해인지 헷갈리지 않게 한다.
            val monthName = if (month.year == today.year) {
                stringResource(ABB_MONTHS[month.monthValue - 1])
            } else {
                month.atDay(1).formatYearMonth()
            }
            stringResource(
                if (isCurrentMonth) R.string.stat_period_current_range else R.string.stat_period_month_range,
                monthName,
                if (isCurrentMonth) today.dayOfMonth else month.lengthOfMonth(),
            )
        }
    }
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        text = text,
        textAlign = TextAlign.Center,
        style = AppTypography.bodySmall,
        color = AppColor.onSurfaceVariant,
    )
}

/**
 * 조회할 월을 고르는 시트. 헤더의 월별 탭을 (선택된 상태에서) 다시 누르면 열린다.
 *
 * 맨 위의 이전/다음 달 버튼은 고른 즉시 적용하되 시트는 닫지 않는다. 한 달씩 넘겨 보며 비교하는
 * 흐름이 가장 잦아서, 매번 시트를 다시 여는 수고를 덜기 위해서다. 그리드에서 월을 직접 고르거나
 * "이번 달"을 누르면 적용과 함께 닫힌다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsMonthSheet(
    selectedMonth: YearMonth,
    firstMonth: YearMonth,
    currentMonth: YearMonth,
    onMonthStepped: (YearMonth) -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedYear by rememberSaveable { mutableStateOf(selectedMonth.year) }
    val year = selectedYear.coerceIn(firstMonth.year, currentMonth.year)
    // 이전/다음 달로 넘기면 그리드의 연도도 그 달을 따라가게 한다.
    val stepTo: (YearMonth) -> Unit = { month ->
        selectedYear = month.year
        onMonthStepped(month)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.surfaceElevated,
        contentColor = AppColor.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenHorizontalPadding).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.stat_choose_month),
                    modifier = Modifier.weight(1f),
                    style = AppTypography.titleLarge,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.stat_month_picker_close))
                }
            }
            // 한 달씩 넘기기: 고른 달이 곧바로 적용되고 시트는 열린 채로 남는다.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundShapes.medium)
                    .background(AppColor.surfaceContainer),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { stepTo(selectedMonth.minusMonths(1)) },
                    enabled = selectedMonth > firstMonth,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.detail_prev_month))
                }
                Text(
                    text = selectedMonth.atDay(1).formatYearMonth(),
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                    style = AppTypography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = AppColor.onSurface,
                )
                IconButton(
                    onClick = { stepTo(selectedMonth.plusMonths(1)) },
                    enabled = selectedMonth < currentMonth,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.detail_next_month))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedYear = year - 1 }, enabled = year > firstMonth.year) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.stat_previous_year))
                }
                Text(
                    text = stringResource(R.string.stat_year, year),
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                    style = AppTypography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { selectedYear = year + 1 }, enabled = year < currentMonth.year) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.stat_next_year))
                }
            }
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                (1..12).chunked(3).forEach { months ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        months.forEach { month ->
                            val value = YearMonth.of(year, month)
                            val selected = value == selectedMonth
                            val available = value in firstMonth..currentMonth
                            val description = value.atDay(1).formatYearMonth()
                            val ink = when {
                                !available -> AppColor.onSurface.copy(alpha = 0.38f)
                                selected -> AppColor.onPrimary
                                else -> AppColor.onSurface
                            }
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundShapes.medium)
                                    .background(if (selected) AppColor.primary else AppColor.surfaceContainer)
                                    .selectable(
                                        selected = selected,
                                        enabled = available,
                                        role = Role.RadioButton,
                                        onClick = { onMonthSelected(value) },
                                    )
                                    .semantics { contentDescription = description }
                                    .heightIn(min = 56.dp).padding(horizontal = 4.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(ABB_MONTHS[month - 1]), style = AppTypography.bodyMedium, color = ink)
                                    if (value == currentMonth) {
                                        Text(stringResource(R.string.stat_this_month), style = AppTypography.labelSmall, color = ink)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.stat_available_months,
                    firstMonth.atDay(1).formatYearMonth(),
                    currentMonth.atDay(1).formatYearMonth(),
                ),
                style = AppTypography.bodySmall,
                color = AppColor.onSurfaceVariant,
            )
            TextButton(onClick = { onMonthSelected(currentMonth) }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.stat_this_month), color = AppColor.primary)
            }
        }
    }
}
