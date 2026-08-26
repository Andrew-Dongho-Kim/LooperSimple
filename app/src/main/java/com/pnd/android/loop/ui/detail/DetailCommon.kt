package com.pnd.android.loop.ui.detail

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.ABB_DAYS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// 상세 화면 전체가 공유하는 치수·컨테이너·작은 조각들
// ─────────────────────────────────────────────────────────────────────────────

/** Inner padding shared by every card on the detail screen. */
internal val CardPadding = 20.dp

/** Vertical gap between rows inside a card (info rows, header → content). */
internal val CardInnerSpacing = 16.dp

/** Vertical padding of one collapsible section row. */
internal val SectionRowPadding = 15.dp

/** Gap between two stat tiles, horizontally and vertically. */
internal val TileSpacing = 10.dp

/**
 * 손가락으로 눌러야 하는 것의 최소 높이. 달력 칸처럼 폭이 화면에 묶여 있는 요소도 높이만은
 * 이 값을 지키게 해, 오탭이 잦던 자리를 줄인다.
 */
internal val MinTouchTarget = 48.dp

/**
 * 루프 색을 나타내는 점의 지름. 액션 바의 제목 앞과 편집기의 이름 칸이 같은 크기를 써,
 * 두 화면에서 같은 루프가 같은 모습으로 보인다.
 */
internal val LoopColorDotSize = 12.dp

/**
 * 화면의 알림 창구. 저장이 끝났다는 확인, 저장할 수 없다는 경고, 삭제 실행 취소가
 * 모두 같은 스낵바 한 곳으로 나가도록 묶어 둔다.
 *
 * 예전에는 실패한 경우에만 스낵바가 떴고 성공은 아무 표시가 없어, 이름·시간·회고 어느 것을
 * 저장해도 "된 건가?"가 남았다.
 */
@Stable
internal class DetailFeedback(
    private val scope: CoroutineScope,
    private val hostState: SnackbarHostState,
    private val context: Context,
) {
    fun show(@StringRes messageRes: Int, vararg formatArgs: Any) {
        show(context.getString(messageRes, *formatArgs))
    }

    fun show(message: String) {
        scope.launch {
            // 앞선 메시지가 남아 있으면 바로 밀어내, 마지막 동작의 결과가 항상 보이게 한다.
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    /** 되돌릴 수 있는 동작에 실행 취소 버튼을 붙여 알린다. */
    fun showUndo(
        @StringRes messageRes: Int,
        @StringRes actionRes: Int = R.string.detail_undo,
        onUndo: () -> Unit,
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = context.getString(messageRes),
                actionLabel = context.getString(actionRes),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}

@Composable
internal fun rememberDetailFeedback(hostState: SnackbarHostState): DetailFeedback {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    return remember(scope, hostState, context) {
        DetailFeedback(scope = scope, hostState = hostState, context = context)
    }
}

/**
 * Shared container for every section on the detail screen: a lifted surface with soft
 * rounding and a hairline border so cards read as a distinct layer over the background in
 * both light and dark themes (mirrors the card styling used on the home screen).
 */
@Composable
internal fun DetailCard(
    modifier: Modifier = Modifier,
    // 행이 스스로 여백을 갖는 목록을 담을 때는 바깥 여백을 비워, 탭 영역이 카드 가장자리까지 닿게 한다.
    contentPadding: PaddingValues = PaddingValues(all = CardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundShapes.large,
        color = AppColor.surfaceElevated,
        border = BorderStroke(
            width = 0.5.dp,
            color = AppColor.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(paddingValues = contentPadding),
            content = content,
        )
    }
}

/**
 * 접이식 섹션 하나. 접힌 상태에서도 [summary] 가 오른쪽에 남아, 펼치지 않고도 값을 읽을 수 있다.
 * "펼쳐야만 알 수 있는 정보"를 만들지 않는 것이 이 목록의 전제다.
 *
 * 머리 행은 하나의 버튼으로 읽히도록 의미를 합치고, 펼침 여부를 상태로 알린다. 예전에는 맨
 * [Modifier.clickable] 만 걸려 있어 스크린 리더에 "버튼"이라는 것도, 지금 펼쳐졌는지도
 * 전달되지 않았다.
 */
@Composable
internal fun ExpandableSection(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    summary: String?,
    summaryColor: Color = AppColor.onSurface.copy(alpha = 0.55f),
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(
        start = CardPadding,
        end = CardPadding,
        bottom = CardPadding,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "sectionChevron",
    )
    val expandedLabel = stringResource(id = R.string.detail_state_expanded)
    val collapsedLabel = stringResource(id = R.string.detail_state_collapsed)
    val actionLabel = stringResource(
        id = if (expanded) R.string.detail_collapse_section else R.string.detail_expand_section
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = actionLabel) { onExpandedChange(!expanded) }
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    stateDescription = if (expanded) expandedLabel else collapsedLabel
                }
                .padding(horizontal = CardPadding, vertical = SectionRowPadding)
                .sizeIn(minHeight = MinTouchTarget - SectionRowPadding * 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = icon,
                tint = AppColor.onSurface.copy(alpha = 0.5f),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = title,
                style = AppTypography.bodyLarge.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (summary != null) {
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                    text = summary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.bodySmall.copy(color = summaryColor),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Icon(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
                    .rotate(chevronRotation),
                imageVector = Icons.Outlined.ExpandMore,
                tint = AppColor.onSurface.copy(alpha = 0.4f),
                // 펼침 여부는 행 전체의 stateDescription 이 말한다. 아이콘까지 따로 읽으면 중복이다.
                contentDescription = null,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(paddingValues = contentPadding),
                content = content,
            )
        }
    }
}

/** 목록 맨 아래에 놓이는, 펼침이 없는 단독 동작 행(내보내기 · 삭제). */
@Composable
internal fun SectionActionRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    tint: Color = AppColor.onSurface,
    iconTint: Color = AppColor.onSurface.copy(alpha = 0.5f),
    summary: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = CardPadding, vertical = SectionRowPadding)
            .sizeIn(minHeight = MinTouchTarget - SectionRowPadding * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = iconTint,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = title,
            style = AppTypography.bodyLarge.copy(
                color = tint,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (summary != null) {
            Text(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                text = summary,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.55f),
                ),
            )
        }
    }
}

/** 섹션 사이를 가르는 선. 아이콘 열과 시작점을 맞춰 목록이 한 덩어리로 읽히게 한다. */
@Composable
internal fun SectionSeparator() {
    HairlineDivider(modifier = Modifier.padding(start = CardPadding))
}

/** 카드 안에서 두 영역을 가르는 머리카락 굵기의 선. */
@Composable
internal fun HairlineDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(AppColor.onSurface.copy(alpha = 0.10f)),
    )
}

/**
 * 테두리 없는 보조 동작 버튼(저장 · 닫기 · 모아보기).
 *
 * 글자에 직접 최소 높이를 주면 남는 공간이 아래로만 쌓여 글자가 위로 붙는다. 상자를 두르고
 * 그 안에서 가운데 정렬해야 48dp 를 채우면서도 글자가 중앙에 온다.
 *
 * [enabled] 가 false 면 글자만 옅어지고 자리는 그대로 남는다. 버튼이 사라지거나 움직이면
 * "저장이 어디 갔지?"가 되므로, 눌리지 않는 이유는 옆의 경고 줄이 대신 말한다.
 */
@Composable
internal fun TextActionButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundShapes.medium)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .sizeIn(minHeight = MinTouchTarget)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = AppTypography.labelLarge.copy(
                color = AppColor.primary.copy(alpha = if (enabled) 1f else 0.35f),
            ),
        )
    }
}

/** 강조색으로 채운 알약 버튼(저장). */
@Composable
internal fun PrimaryPillButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(AppColor.primary.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .sizeIn(minHeight = MinTouchTarget)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = AppTypography.labelLarge.copy(color = AppColor.onPrimary),
        )
    }
}

/** A single "icon · label … value" line used inside the schedule section. */
@Composable
internal fun InfoRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = AppColor.onSurface.copy(alpha = 0.5f),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = label,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = value,
            textAlign = TextAlign.End,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (trailing != null) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = trailing,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** The seven weekdays as compact chips; active days are filled and tinted with the accent. */
@Composable
internal fun ActiveDaysRow(
    modifier: Modifier = Modifier,
    activeDays: Int,
    contentDescription: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 칩 일곱 개를 하나하나 읽으면 "월 화 수…"만 나열될 뿐 무엇이 켜졌는지 알 수 없다.
            // 대신 "활동 요일: 월, 화, 수" 한 문장으로 대체한다.
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val selected = activeDays.isOn(LoopDay.fromIndex(index))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (selected) {
                            AppColor.primary.copy(alpha = 0.14f)
                        } else {
                            AppColor.surfaceContainer
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(id = dayResId),
                    style = AppTypography.labelMedium.copy(
                        color = if (selected) {
                            AppColor.primary
                        } else {
                            AppColor.onSurface.copy(alpha = 0.4f)
                        },
                    ),
                )
            }
        }
    }
}

/**
 * 하루(24시간)를 가로 막대로 놓고 활동 구간([startFraction]~[endFraction], 0f..1f)만 강조색으로 칠한다.
 * 30분 같은 짧은 구간도 사라지지 않도록 최소 폭을 보장하며, 시작 위치는 실제 시각에 맞춘다.
 */
@Composable
internal fun DayTimeline(
    modifier: Modifier = Modifier,
    startFraction: Float,
    endFraction: Float,
    accent: Color,
    contentDescription: String,
) {
    val before = startFraction.coerceIn(0f, 1f)
    val window = (endFraction - startFraction).coerceIn(0.03f, 1f - before)
    val after = (1f - before - window).coerceAtLeast(0f)

    Row(
        modifier = modifier
            .height(12.dp)
            .clip(CircleShape)
            .background(AppColor.surfaceContainer)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
    ) {
        if (before > 0f) {
            Box(
                modifier = Modifier
                    .weight(before)
                    .fillMaxHeight()
            )
        }
        Box(
            modifier = Modifier
                .weight(window)
                .fillMaxHeight()
                .background(accent),
        )
        if (after > 0f) {
            Box(
                modifier = Modifier
                    .weight(after)
                    .fillMaxHeight()
            )
        }
    }
}

/** 타임라인 아래 0·6·12·18·24시 눈금 라벨. */
@Composable
internal fun HourTicks(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 눈금 숫자만 따로 읽히면 "0 6 12 18 24"라는 뜻 없는 나열이 된다.
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(0, 6, 12, 18, 24).forEach { hour ->
            Text(
                text = "$hour",
                style = AppTypography.labelSmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** 화살표 하나짜리 아이콘 버튼. 최소 터치 크기를 지키고, 무엇을 하는 버튼인지 이름을 갖는다. */
@Composable
internal fun NavArrowButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(MinTouchTarget)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.25f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            tint = AppColor.onSurface.copy(alpha = 0.7f),
            contentDescription = if (enabled) contentDescription else null,
        )
    }
}
