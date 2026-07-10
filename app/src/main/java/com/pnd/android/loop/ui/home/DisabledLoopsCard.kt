package com.pnd.android.loop.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.toLocalDate

/** 접힘 상태에서 미리보기 칩으로 보여줄 최대 개수. 이보다 많으면 "+N" 칩으로 축약한다. */
private const val PREVIEW_CHIP_LIMIT = 5

/**
 * 전체 탭에서 비활성 루프들을 하나의 카드(아이템)로 묶어 접었다 펼치는 컨테이너.
 * - 접힘: "비활성 루프 N개" 헤더 + 색·제목 미리보기 칩.
 * - 펼침: 각 비활성 루프를 컴팩트 행으로 나열하고, 우측에 "언제부터 비활성인지"를 표시한다.
 * 색은 모두 테마 토큰에서 가져와 라이트/다크 모드에 함께 대응한다.
 */
@Composable
fun DisabledLoopsCard(
    modifier: Modifier = Modifier,
    loops: List<LoopBase>,
    doneHistory: Map<Int, Map<Long, Int>>,
    isExpanded: Boolean,
    onExpandChanged: (Boolean) -> Unit,
    onEnable: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LoopCardShape)
            .background(color = AppColor.surfaceElevated)
            // 배경과 카드를 분리해주는 은은한 헤어라인 테두리 (다크/라이트 공통).
            .border(
                width = 1.dp,
                color = AppColor.onSurface.copy(alpha = 0.08f),
                shape = LoopCardShape,
            )
            .clickable { onExpandChanged(!isExpanded) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        DisabledLoopsHeader(
            count = loops.size,
            isExpanded = isExpanded,
        )

        // 접힘: 미리보기 칩 줄. 펼침 애니메이션과 반대로 접힐 때만 보인다.
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
        ) {
            DisabledLoopsPreview(
                modifier = Modifier.padding(top = 12.dp),
                loops = loops,
            )
        }

        // 펼침: 비활성 루프 상세 목록.
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
        ) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                loops.forEach { loop ->
                    DisabledLoopRow(
                        loop = loop,
                        doneHistory = doneHistory,
                        onEnable = onEnable,
                        onNavigateToDetailPage = onNavigateToDetailPage,
                    )
                }
            }
        }
    }
}

@Composable
private fun DisabledLoopsHeader(
    count: Int,
    isExpanded: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.disabled_loops_header, count),
            style = AppTypography.titleSmall.copy(color = AppColor.onSurface),
        )

        Spacer(modifier = Modifier.weight(1f))

        // 펼침 상태에 따라 셰브론을 180도 돌려 접힘/펼침을 알린다.
        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = tween(300),
            label = "DisabledLoopsChevron",
        )
        Icon(
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
            imageVector = Icons.Rounded.ExpandMore,
            tint = AppColor.onSurface.copy(alpha = 0.6f),
            contentDescription = null,
        )
    }
}

/**
 * 접힘 상태 미리보기. 색 점 + 제목 칩을 가로로 나열하고, [PREVIEW_CHIP_LIMIT] 를 넘으면
 * 마지막에 "+N" 칩으로 축약한다. 좁은 화면에서도 잘리지 않도록 가로 스크롤을 허용한다.
 */
@Composable
private fun DisabledLoopsPreview(
    modifier: Modifier = Modifier,
    loops: List<LoopBase>,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        loops.take(PREVIEW_CHIP_LIMIT).forEach { loop ->
            DisabledLoopChip(title = loop.title, color = loop.color)
        }
        val overflow = loops.size - PREVIEW_CHIP_LIMIT
        if (overflow > 0) {
            OverflowChip(count = overflow)
        }
    }
}

@Composable
private fun DisabledLoopChip(
    title: String,
    color: Int,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoopCardColor(
            modifier = Modifier.size(8.dp),
            color = color,
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = title,
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OverflowChip(count: Int) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$count",
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
    }
}

/**
 * 펼침 상태의 비활성 루프 한 줄. 좌측에 색 점 + 제목, 우측에 "언제부터 비활성인지"를 둔다.
 * 행을 탭하면 상세 화면으로 이동해 다시 활성화할 수 있다.
 */
@Composable
private fun DisabledLoopRow(
    loop: LoopBase,
    doneHistory: Map<Int, Map<Long, Int>>,
    onEnable: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onNavigateToDetailPage(loop) }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoopCardColor(
            modifier = Modifier.size(8.dp),
            color = loop.color,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            text = loop.title,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val sinceText = disabledSinceLabel(loop = loop, doneHistory = doneHistory)
        if (sinceText != null) {
            Text(
                modifier = Modifier.padding(end = 8.dp),
                text = sinceText,
                style = AppTypography.labelMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.5f),
                ),
                maxLines = 1,
            )
        }

        // 우측 끝의 활성화 버튼. 행 자체는 상세로 이동하지만, 이 버튼은 한 번에 다시 켠다.
        EnableButton(onClick = { onEnable(loop) })
    }
}

/**
 * 비활성 루프를 즉시 다시 켜는 버튼. primary 색을 옅게 깐 원형 위에 순환 아이콘을 얹어,
 * 라이트/다크 모드 모두에서 같은 강도로 읽히고 카드 안에서 과하게 튀지 않도록 한다.
 */
@Composable
private fun EnableButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color = AppColor.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.Rounded.Autorenew,
            tint = AppColor.primary,
            contentDescription = stringResource(id = R.string.loop_enable),
        )
    }
}

/**
 * "언제부터 비활성인지" 문구. 별도 타임스탬프가 없으므로, done 이력에서 오늘부터 거슬러
 * 올라가 끊기지 않고 이어지는 비활성(DISABLED) 구간의 시작일을 비활성 시작일로 본다.
 * 이력이 없으면(계산 불가) null 을 반환해 아무것도 표시하지 않는다.
 */
@Composable
private fun disabledSinceLabel(
    loop: LoopBase,
    doneHistory: Map<Int, Map<Long, Int>>,
): String? {
    val sinceMs = disabledSinceMs(doneHistory[loop.loopId]) ?: return null
    val dateText = sinceMs.toLocalDate().formatMonthDateDay()
    return stringResource(id = R.string.disabled_since, dateText)
}

/**
 * done 이력(Map<날짜(자정 epoch ms), 상태>)에서 최근부터 이어지는 DISABLED 구간의
 * 시작일(epoch ms)을 찾는다. 이력에는 DONE/SKIP/DISABLED 만 담겨 있어(NO_RESPONSE 제외),
 * 날짜 내림차순으로 훑다가 DISABLED 가 아닌 첫 상태에서 멈추면 그 구간의 시작일이 된다.
 */
private fun disabledSinceMs(history: Map<Long, Int>?): Long? {
    if (history.isNullOrEmpty()) return null

    var since: Long? = null
    for ((date, state) in history.entries.sortedByDescending { it.key }) {
        if (state == LoopDoneVo.DoneState.DISABLED) {
            since = date
        } else {
            break
        }
    }
    return since
}
