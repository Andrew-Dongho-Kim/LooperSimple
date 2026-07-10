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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
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
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.formatMonthDateDay

/**
 * 선택한 달의 달성 요약 지표. 달력 상단 배너와 회고 모음에서 함께 사용한다.
 *
 * @param doneCount 그 달에 완료(DONE)한 기록 수.
 * @param totalCount 완료 + 미완료(건너뜀·무응답) 기록 수. 달성률 분모.
 * @param completionRate doneCount / totalCount (0f..1f). totalCount 가 0이면 0.
 * @param activeDays 완료한 기록이 하루라도 있는 날의 수.
 * @param retrospectCount 그 달에 남긴 회고(메모) 개수.
 * @param investedTimeMs 그 달에 완료한 루프에 투자한 시간(ms) 총합.
 */
data class MonthAchievementSummary(
    val doneCount: Int,
    val totalCount: Int,
    val completionRate: Float,
    val activeDays: Int,
    val retrospectCount: Int,
    val investedTimeMs: Long,
) {
    companion object {
        val Empty = MonthAchievementSummary(
            doneCount = 0,
            totalCount = 0,
            completionRate = 0f,
            activeDays = 0,
            retrospectCount = 0,
            investedTimeMs = 0L,
        )
    }
}

/** 진행 링의 기본 지름/굵기. 하루 요약 헤더와 월 요약 배너가 같은 링을 공유한다. */
private val DefaultRingDiameter = 34.dp
private val DefaultRingStroke = 3.dp

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
 * 왼쪽 원형 링은 그 달의 달성률(가운데에 퍼센트 숫자)을, 가운데 텍스트는 완료/전체와 활동일 수를
 * 보여준다. 오른쪽에는 (전체 기록 기준) 연속 달성 스트릭 칩과 그 달의 회고 개수 칩을 둔다.
 * 회고 칩을 누르면 [onClickRetrospects]로 그 달의 회고 모음이 열린다(값이 있을 때만 노출).
 *
 * 모든 색은 [AppColor] 토큰과 primary 기반 반투명이라 라이트/다크 모두에서 대비가 유지된다.
 */
@Composable
fun SelectedMonthSummaryBar(
    modifier: Modifier = Modifier,
    summary: MonthAchievementSummary,
    currentStreak: Int,
    onClickRetrospects: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(AppColor.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.06f else 0.035f))
            .padding(horizontal = Dimens.contentPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 달성률 링 + 가운데 퍼센트 숫자.
        Box(contentAlignment = Alignment.Center) {
            AchievementRing(
                fraction = summary.completionRate,
                color = AppColor.primary,
                diameter = 44.dp,
                stroke = 4.dp,
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
                text = stringResource(id = R.string.achievement_active_days, summary.activeDays),
                style = AppTypography.labelMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.55f),
                ),
            )
        }

        // 연속 달성 스트릭(전체 기록 기준). 살아있는 스트릭이 있을 때만 강조 칩으로 노출한다.
        if (currentStreak > 0) {
            SummaryChip(
                icon = Icons.Outlined.LocalFireDepartment,
                text = stringResource(id = R.string.stat_streak_days, currentStreak),
                tint = AppColor.primary,
            )
        }
        // 그 달에 남긴 회고가 있으면, 눌러서 모아 볼 수 있는 칩을 함께 보여준다.
        if (summary.retrospectCount > 0) {
            SummaryChip(
                modifier = Modifier.padding(start = Dimens.itemSpacing),
                icon = Icons.Outlined.Edit,
                text = "${summary.retrospectCount}",
                tint = AppColor.onSurface.copy(alpha = 0.7f),
                onClick = onClickRetrospects,
            )
        }
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
