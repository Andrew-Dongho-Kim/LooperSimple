package com.pnd.android.loop.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.ui.statisctics.investedDurationText
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.ui.theme.warning
import com.pnd.android.loop.util.formatMonthDateDay
import kotlin.math.roundToInt

/**
 * 선택한 달의 달성 요약 지표. 달력 상단 배너와 회고 모음에서 함께 사용한다.
 *
 * @param doneCount 그 달에 완료(DONE)한 기록 수.
 * @param totalCount 완료 + 미완료(건너뜀·무응답) 기록 수. 달성률 분모.
 * @param completionRate doneCount / totalCount (0f..1f). totalCount 가 0이면 0.
 * @param activeDays 완료한 기록이 하루라도 있는 날의 수.
 * @param retrospectCount 그 달에 남긴 회고(메모) 개수.
 * @param investedTimeMs 그 달에 완료한 루프에 투자한 시간(ms) 총합.
 * @param perfectDays 그날 응답한 루프를 하나도 빠뜨리지 않고 모두 완료한 날의 수.
 * @param longestStreak 그 달 안에서 완료가 연속으로 이어진 최대 일수(전체 기록 기준 스트릭이 아니다).
 * @param skippedCount 건너뜀(SKIP)으로 응답한 기록 수.
 * @param noResponseCount 응답하지 않고 지나간 기록 수.
 * @param prevMonthCompletionRate 지난달 완료율(0f..1f). 비교할 기록이 없으면 null.
 */
data class MonthAchievementSummary(
    val doneCount: Int,
    val totalCount: Int,
    val completionRate: Float,
    val activeDays: Int,
    val retrospectCount: Int,
    val investedTimeMs: Long,
    val perfectDays: Int,
    val longestStreak: Int,
    val skippedCount: Int,
    val noResponseCount: Int,
    val prevMonthCompletionRate: Float?,
) {
    /** 완료 기록 중 회고를 남긴 비율(0~100). 완료가 없으면 0. */
    val retrospectPercent: Int
        get() = if (doneCount == 0) 0 else (retrospectCount * 100) / doneCount

    /**
     * 지난달 완료율과의 차이(%p, 반올림). 비교할 지난달 기록이 없으면 null.
     * 예: 이번 달 65%, 지난달 58% → `7`.
     */
    val completionRateDeltaPoints: Int?
        get() = prevMonthCompletionRate?.let { prev ->
            ((completionRate - prev) * 100).roundToInt()
        }

    companion object {
        val Empty = MonthAchievementSummary(
            doneCount = 0,
            totalCount = 0,
            completionRate = 0f,
            activeDays = 0,
            retrospectCount = 0,
            investedTimeMs = 0L,
            perfectDays = 0,
            longestStreak = 0,
            skippedCount = 0,
            noResponseCount = 0,
            prevMonthCompletionRate = null,
        )
    }
}

/** 진행 링의 기본 지름/굵기. 하루 요약 헤더와 월 요약 배너가 같은 링을 공유한다. */
private val DefaultRingDiameter = 34.dp
private val DefaultRingStroke = 3.dp

/** 월 요약 배너의 링 크기와 위아래 여백. 배너 높이([SelectedMonthSummaryBarHeight])의 근거가 된다. */
private val SummaryBarRingDiameter = 40.dp
private val SummaryBarRingStroke = 4.dp
private val SummaryBarVerticalPadding = 10.dp

/** 지표 타일 한 칸의 높이와, 위쪽 요약 줄과의 간격. */
private val SummaryTileHeight = 40.dp
private val SummaryTileRowGap = 8.dp

/**
 * 월 요약 배너가 차지하는 세로 높이. 위 요약 줄(가장 큰 요소가 링)과 아래 지표 타일 줄, 그리고
 * 위아래 여백으로 결정된다. 아래 달력 패널의 펼침 높이를 역산할 때 쓰이므로, 배너 구성이 바뀌면
 * 위 상수도 함께 맞춰야 한다.
 */
val SelectedMonthSummaryBarHeight = SummaryBarVerticalPadding * 2 +
        SummaryBarRingDiameter + SummaryTileRowGap + SummaryTileHeight

/**
 * 달성 정도를 나타내는 얇은 원형 진행 링. 12시 방향에서 시작해 시계 방향으로 [fraction]만큼 채운다.
 * 채우는 색([color])은 호출부가 정하고, 바탕 트랙은 라이트/다크 모두에서 은은한 중립색이라
 * 채워지지 않은 부분도 자연스럽게 얹힌다. 하루 카드·월 요약 배너가 공유해 링이 한 벌만 존재한다.
 */
@Composable
fun AchievementRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = DefaultRingDiameter,
    stroke: Dp = DefaultRingStroke,
) {
    // 채워지지 않은 바탕 트랙 색. onSurface 기반 반투명이라 두 테마 모두에서 어색하지 않다.
    val trackColor = AppColor.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.14f else 0.10f)
    val sweepAngle = fraction.coerceIn(0f, 1f) * 360f

    Canvas(modifier = modifier.size(diameter)) {
        val strokeWidthPx = stroke.toPx()
        // 선은 경로의 중심을 따라 그려지므로, 굵기의 절반만큼 안으로 들여 캔버스 밖으로 잘리지 않게 한다.
        val inset = strokeWidthPx / 2f
        val arcTopLeft = Offset(inset, inset)
        val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

        // 바탕 트랙(전체 원).
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx),
        )
        // 달성 정도만큼 채우는 진행 호. 끝을 둥글려(Round) 부드러운 인상을 준다.
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        )
    }
}

/**
 * 달력 상단에 얹는 "선택한 달" 요약 배너.
 *
 * 위 줄은 그 달의 성적표다. 왼쪽 원형 링이 달성률(가운데에 퍼센트 숫자), 가운데 텍스트가 완료/전체와
 * 활동일 수 그리고 완료하지 못한 기록의 종류(건너뜀/무응답)를, 오른쪽 칩이 회고 수와 비율을 보여준다.
 * 회고 칩을 누르면 [onClickRetrospects]로 그 달의 회고 모음이 열린다(회고가 있을 때만 노출).
 *
 * 아래 줄은 라벨을 가진 지표 타일 네 칸이다. 링·완료율만으로는 알 수 없는 "얼마나 오래(투자 시간),
 * 얼마나 완전하게(완벽한 날), 얼마나 이어서(최장 연속), 지난달보다 나아졌는지(전월 대비)"를 담는다.
 * 네 값 모두 그 달만의 값이라, 달을 넘기면 함께 바뀐다.
 *
 * 모든 색은 [AppColor] 토큰과 primary 기반 반투명이라 라이트/다크 모두에서 대비가 유지된다.
 */
@Composable
fun SelectedMonthSummaryBar(
    modifier: Modifier = Modifier,
    summary: MonthAchievementSummary,
    onClickRetrospects: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(AppColor.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.06f else 0.035f))
            .padding(
                horizontal = Dimens.contentPadding,
                vertical = SummaryBarVerticalPadding,
            ),
    ) {
        Row(
            modifier = Modifier.height(SummaryBarRingDiameter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 달성률 링 + 가운데 퍼센트 숫자.
            Box(contentAlignment = Alignment.Center) {
                AchievementRing(
                    fraction = summary.completionRate,
                    color = AppColor.primary,
                    diameter = SummaryBarRingDiameter,
                    stroke = SummaryBarRingStroke,
                )
                Text(
                    text = "${(summary.completionRate * 100).toInt()}",
                    style = AppTypography.labelMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.8f),
                    ),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Dimens.contentPadding),
            ) {
                Text(
                    text = stringResource(
                        id = R.string.achievement_done_ratio,
                        summary.doneCount,
                        summary.totalCount,
                    ),
                    style = AppTypography.titleSmall.copy(
                        color = AppColor.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = missedBreakdownText(summary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.labelMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.55f),
                    ),
                )
            }

            // 그 달에 남긴 회고가 있으면, 눌러서 모아 볼 수 있는 칩을 보여준다(개수 + 완료 대비 비율).
            if (summary.retrospectCount > 0) {
                SummaryChip(
                    modifier = Modifier.padding(start = Dimens.itemSpacing),
                    icon = Icons.Outlined.Edit,
                    text = stringResource(
                        id = R.string.achievement_retrospect_chip,
                        summary.retrospectCount,
                        summary.retrospectPercent,
                    ),
                    tint = AppColor.onSurface.copy(alpha = 0.7f),
                    onClick = onClickRetrospects,
                )
            }
        }

        MonthStatTileRow(
            modifier = Modifier.padding(top = SummaryTileRowGap),
            summary = summary,
        )
    }
}

/**
 * 완료하지 못한 기록의 종류를 알려 주는 부제. "활동 21일 · 건너뜀 12 · 무응답 5"처럼 이어 붙인다.
 *
 * 완료율만 보면 낮은 이유를 알 수 없는데, 건너뜀(스스로 넘긴 것)과 무응답(그냥 지나간 것)을 나눠
 * 보여 주면 그 달을 어떻게 개선할지가 달라진다. 없는 항목은 아예 빼서 문장이 길어지지 않게 한다.
 */
@Composable
private fun missedBreakdownText(summary: MonthAchievementSummary): String {
    val parts = listOfNotNull(
        stringResource(id = R.string.achievement_active_days, summary.activeDays),
        stringResource(id = R.string.achievement_skipped_count, summary.skippedCount)
            .takeIf { summary.skippedCount > 0 },
        stringResource(id = R.string.achievement_no_response_count, summary.noResponseCount)
            .takeIf { summary.noResponseCount > 0 },
    )
    return parts.joinToString(separator = " · ")
}

/**
 * 요약 배너 아래 줄의 지표 타일 네 칸. 라벨과 값을 위아래로 쌓아, 숫자만 보고 무슨 값인지 헷갈릴
 * 일이 없게 한다. 네 칸이 같은 너비를 나눠 가지므로 값이 길어져도 배치가 흔들리지 않는다.
 */
@Composable
private fun MonthStatTileRow(
    modifier: Modifier = Modifier,
    summary: MonthAchievementSummary,
) {
    val deltaPoints = summary.completionRateDeltaPoints
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SummaryTileHeight),
        horizontalArrangement = Arrangement.spacedBy(Dimens.itemSpacing),
    ) {
        MonthStatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.stat_summary_invested),
            value = investedDurationText(investedTimeMs = summary.investedTimeMs),
        )
        MonthStatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.stat_summary_perfect_days),
            value = "${summary.perfectDays}",
        )
        MonthStatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.stat_streak_longest),
            value = "${summary.longestStreak}",
        )
        MonthStatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.achievement_prev_month),
            // 비교할 지난달 기록이 없으면 0%p처럼 오해되지 않도록 빈 값 기호를 쓴다.
            value = deltaPoints?.let {
                stringResource(id = R.string.achievement_delta_points, it)
            } ?: stringResource(id = R.string.achievement_no_value),
            // 개선은 앱 강조색, 하락은 경고색(앰버). 변화 없음·비교 불가는 담담한 기본색.
            valueColor = when {
                deltaPoints == null || deltaPoints == 0 -> null
                deltaPoints > 0 -> AppColor.primary
                else -> AppColor.warning
            },
        )
    }
}

/** 지표 타일 한 칸. 위에 작은 라벨, 아래에 값. 배경은 패널과 같은 [surfaceContainer]. */
@Composable
private fun MonthStatTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundShapes.small)
            .background(AppColor.surfaceContainer)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.labelSmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.55f),
                letterSpacing = 0.sp,
            ),
        )
        Text(
            modifier = Modifier.padding(top = 1.dp),
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.titleSmall.copy(
                color = valueColor ?: AppColor.onSurface,
                letterSpacing = 0.sp,
            ),
        )
    }
}

/**
 * 요약 배너 오른쪽의 작은 알약형 칩(아이콘 + 숫자). [onClick]이 있으면 눌러서 동작한다.
 * 배경은 패널과 같은 [surfaceContainer]라 어떤 배경 위에서도 대비를 유지한다.
 */
@Composable
private fun SummaryChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    tint: Color,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(AppColor.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = text,
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.85f),
            ),
        )
    }
}

/**
 * 선택한 달의 회고 모음 다이얼로그. 날짜(최신순)별로 루프 색·제목·날짜와 회고 내용을 보여준다.
 * 하루 카드에 흩어져 있던 회고를 한곳에 모아, 그 달의 궤적을 이야기처럼 돌아볼 수 있게 한다.
 * 회고가 없으면 안내 문구만 표시한다. 배경/글자 모두 테마 색이라 라이트/다크에 대응한다.
 */
@Composable
fun MonthRetrospectsDialog(
    monthLabel: String,
    retrospects: List<LoopByDate>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundShapes.large)
                .background(AppColor.surfaceElevated)
                .border(
                    width = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.1f),
                    shape = RoundShapes.large,
                )
                .padding(Dimens.contentPadding),
        ) {
            Text(
                text = stringResource(id = R.string.achievement_month_notes_title, monthLabel),
                style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
            )

            if (retrospects.isEmpty()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    text = stringResource(id = R.string.achievement_no_notes),
                    textAlign = TextAlign.Center,
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.5f),
                    ),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = Dimens.contentPadding)
                        // 회고가 많아도 화면을 넘지 않도록 최대 높이를 두고 안에서 스크롤한다.
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.contentPadding),
                ) {
                    items(retrospects.size) { index ->
                        RetrospectRow(item = retrospects[index])
                    }
                }
            }
        }
    }
}

/** 회고 모음의 한 줄. 왼쪽 루프 색 점, 오른쪽에 제목·날짜와 인용부호로 감싼 회고 내용. */
@Composable
private fun RetrospectRow(
    modifier: Modifier = Modifier,
    item: LoopByDate,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .background(
                    color = item.color.compositeOverOnSurface(),
                    shape = CircleShape,
                ),
        )
        Column(
            modifier = Modifier
                .padding(start = Dimens.contentPadding)
                .weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = item.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    modifier = Modifier.padding(start = Dimens.itemSpacing),
                    text = item.date.formatMonthDateDay(),
                    style = AppTypography.labelMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.5f),
                    ),
                )
            }
            item.retrospect?.let { note ->
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = "“$note”",
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.6f),
                        lineHeight = 18.sp,
                    ),
                )
            }
        }
    }
}
