package com.pnd.android.loop.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.TimeStat
import com.pnd.android.loop.data.actualStartInDay
import com.pnd.android.loop.data.common.NO_REPEAT
import com.pnd.android.loop.data.currentTimeStat
import com.pnd.android.loop.data.doneState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOver
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.DAY_STRING_MAP
import com.pnd.android.loop.util.annotatedString
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.toMs
import java.time.LocalTime


/**
 * Visual tokens shared by every loop card. Keeping them here makes the spacing,
 * shape and opacity hierarchy consistent and easy to tweak for both light/dark themes.
 */
private object LoopCardDefaults {
    /** Rounding of the card surface (also mirrored by the swipe-response backdrop). */
    val CardCorner = 20.dp

    /** Hairline border that separates the card from the background in both themes. */
    val BorderWidth = 1.dp
    const val BorderAlpha = 0.08f

    val ContentHorizontalPadding = 16.dp
    val ContentVerticalPadding = 14.dp

    /** Leading loop-color dot; its footprint stays fixed so the pulsing halo never shifts layout. */
    val ColorDotSize = 10.dp
    val ColorDotFootprint = 20.dp
    const val ColorDotPulseAlpha = 0.25f
    const val ColorDotPulseMaxScale = 2f

    const val EnabledAlpha = 1f
    const val DisabledAlpha = 0.3f
    const val TitleAlpha = 0.9f
    const val MetaAlpha = 0.6f

    /** Active-day letters: on/off is told by ink strength alone, no extra color. */
    const val DaySelectedAlpha = 0.75f
    const val DayUnselectedAlpha = 0.25f

    /** Wash of the loop color over the row when it is highlighted (opened from a notification). */
    const val HighlightTintAlpha = 0.12f

    /**
     * 진행 중(now) 카드를 도드라지게 하는 표시들. 루프 색을 표면에 은은히 깔고(ActiveTintAlpha)
     * 왼쪽 가장자리에 세로 강조 바를 세워 "지금 살아있는" 카드로 읽히게 한다. 라이트/다크 모두
     * 배경 위에서 같은 강도로 떠 보이도록 낮은 알파/얇은 폭으로 유지한다.
     */
    const val ActiveTintAlpha = 0.08f
    val ActiveBarWidth = 3.dp
    val ActiveBarInset = 10.dp


    /** Circular start / stop button trailing an "any time" card. */
    val ResponseButtonSize = 36.dp
    val ResponseIconSize = 18.dp

    /** Container tint of a response button: primary wash for positive, neutral for skip. */
    const val ResponseTintAlpha = 0.12f
    const val ResponseNeutralAlpha = 0.06f

    /**
     * Finished (awaiting-response) card is dimmed so it reads as a past item: the surface
     * sinks one step and the ink softens, while the swipe peek badges stay full-strength.
     */
    const val DimmedInkAlpha = 0.6f
}

/** Shared rounded shape of the card surface. */
internal val LoopCardShape = RoundedCornerShape(LoopCardDefaults.CardCorner)

/**
 * Loop card row. In its normal state it reads as a single quiet line — color dot + title/meta
 * + time chip, with management actions (edit, on/off, group, delete) tucked into the trailing
 * popup menu. Once today's window has closed it switches to the finished state: the surface is
 * dimmed to read as a past item and the done / skip actions appear as two tap buttons grouped
 * on the left, so a response is one tap away without any swipe gesture.
 */
@Composable
fun LoopCard(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    cardValues: LoopCardValues,
    onStateChanged: (loop: LoopBase, doneState: Int) -> Unit,
    onEdit: (LoopBase) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val mockAlpha = animateCardAlphaWithMock(loopBase = loop)
    val contentAlpha = if (loop.enabled) {
        LoopCardDefaults.EnabledAlpha
    } else {
        LoopCardDefaults.DisabledAlpha
    }

    val timeStat = loop.currentTimeStat
    val syncWithTime = !loop.isMock && cardValues.syncWithTime

    // 오늘 창이 끝나 완료/건너뜀 응답을 기다리는 상태. 카드를 한 단계 가라앉혀(디밍) "지나간
    // 항목"으로 읽히게 하고, 완료/건너뜀은 왼쪽에 모은 탭 버튼으로 처리한다.
    val awaitsResponse = syncWithTime && loop.enabled && timeStat.isPast()

    // 진행 중: 지금 이 루프의 시간창 안에 있는 상태. 표면을 루프 색으로 물들이고 왼쪽 강조 바를
    // 세워 목록에서 "지금 이거"가 한눈에 튀도록 한다.
    val isInProgress = syncWithTime && loop.enabled && cardValues.isActive
    // 시작 전: 오늘 예정돼 있지만 아직 시작 시각 전인 상태. 색 도트만 옅게 낮춰 진행 중과 구분한다.

    val background = if (awaitsResponse) {
        // 종료 카드는 강조 표면 대신 한 단계 낮은 컨테이너 톤으로 가라앉힌다.
        AppColor.surfaceContainer
    } else {
        loopCardBackground(
            loop = loop,
            isInProgress = isInProgress,
            isHighlighted = cardValues.isHighlighted,
        )
    }

    // 좌측 강조 바 색은 도트와 같은(표면 위에 얹은) 루프 색을 써 카드 안에서 색을 통일한다.
    val activeBarColor = loop.color.compositeOverOnSurface()

    Row(
        modifier = modifier
            .graphicsLayer { alpha = mockAlpha }
            .fillMaxWidth()
            .clip(LoopCardShape)
            .background(background)
            // 진행 중 카드에만 왼쪽 가장자리에 세로 강조 바를 그린다. drawBehind 라 레이아웃을
            // 밀지 않고, 위의 clip 으로 카드 모서리 안쪽에 깔끔히 잘려 보인다.
            .then(
                if (isInProgress) {
                    Modifier.drawBehind {
                        val barWidth = LoopCardDefaults.ActiveBarWidth.toPx()
                        val inset = LoopCardDefaults.ActiveBarInset.toPx()
                        drawRoundRect(
                            color = activeBarColor,
                            topLeft = Offset(x = 0f, y = inset),
                            size = Size(width = barWidth, height = size.height - inset * 2),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        )
                    }
                } else {
                    Modifier
                }
            )
            // 배경과 카드를 분리해주는 은은한 헤어라인 테두리 (다크/라이트 공통).
            .border(
                width = LoopCardDefaults.BorderWidth,
                color = AppColor.onSurface.copy(alpha = LoopCardDefaults.BorderAlpha),
                shape = LoopCardShape,
            )
            .clickable(enabled = !loop.isMock) { onNavigateToDetailPage(loop) }
            .padding(
                horizontal = LoopCardDefaults.ContentHorizontalPadding,
                vertical = LoopCardDefaults.ContentVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (awaitsResponse) {
            FinishedCardContent(
                loop = loop,
                onStateChanged = onStateChanged,
            )
        } else {
            ActiveCardContent(
                loop = loop,
                cardValues = cardValues,
                timeStat = timeStat,
                syncWithTime = syncWithTime,
                contentAlpha = contentAlpha,
                isInProgress = isInProgress,
                onStateChanged = onStateChanged,
                onEdit = onEdit,
                onEnabled = onEnabled,
                onDelete = onDelete,
                onNavigateToGroupPicker = onNavigateToGroupPicker,
            )
        }
    }
}

/**
 * Normal (not-yet-finished) row content: color dot + title/meta, a trailing time chip, and —
 * for live cards — the any-time start/stop control plus the management overflow menu.
 */
@Composable
private fun RowScope.ActiveCardContent(
    loop: LoopBase,
    cardValues: LoopCardValues,
    timeStat: TimeStat,
    syncWithTime: Boolean,
    contentAlpha: Float,
    isInProgress: Boolean,
    onStateChanged: (loop: LoopBase, doneState: Int) -> Unit,
    onEdit: (LoopBase) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
) {
    LoopColorDot(
        modifier = Modifier.alpha(contentAlpha),
        color = loop.color,
        // 진행 중이면 도트 뒤로 헤일로가 숨 쉬고, 시작 전이면 도트를 옅게 낮춰 대비시킨다.
        isPulsing = isInProgress,
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(start = 12.dp)
            .alpha(contentAlpha),
    ) {
        LoopCardTitle(title = loop.title)
        LoopCardMeta(
            modifier = Modifier.padding(top = 3.dp),
            loop = loop,
            timeStat = timeStat,
            syncWithTime = syncWithTime,
        )
    }
    LoopTimeChip(
        modifier = Modifier
            .padding(start = 8.dp)
            .alpha(contentAlpha),
        loop = loop,
    )
    if (syncWithTime && loop.enabled &&
        loop.isAnyTime && (loop.startInDay < 0 || loop.endInDay < 0)
    ) {
        AnyTimeLoopStartOrStop(
            modifier = Modifier.padding(start = 8.dp),
            loop = loop,
            onStateChanged = onStateChanged,
        )
    }
    if (!loop.isMock) {
        LoopCardMenu(
            modifier = Modifier.padding(start = 4.dp),
            loop = loop,
            showAddToGroup = cardValues.showAddToGroup,
            onEdit = onEdit,
            onEnabled = onEnabled,
            onDelete = onDelete,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
        )
    }
}

/**
 * Finished (awaiting-response) row content: the done / skip tap buttons grouped on the left at
 * full strength, then the dimmed title, then the muted finished-time label on the right. The
 * dimmed title + time read as "already ran today" while the buttons stay clearly actionable.
 */
@Composable
private fun RowScope.FinishedCardContent(
    loop: LoopBase,
    onStateChanged: (loop: LoopBase, doneState: Int) -> Unit,
) {
    LoopDoneOrSkip(
        loop = loop,
        onStateChanged = onStateChanged,
    )
    LoopCardTitle(
        modifier = Modifier
            .weight(1f)
            .padding(start = 14.dp),
        title = loop.title,
        dimmed = true,
    )
    FinishedTimeLabel(
        modifier = Modifier.padding(start = 8.dp),
        loop = loop,
    )
}

/**
 * Opaque background for the loop card. Sits on the elevated surface so each card reads as
 * a distinct raised layer; when highlighted (e.g. opened from a notification) it gets a soft
 * wash of the loop's own color. Opaque so the swipe-response actions stay hidden at rest.
 */
@Composable
private fun loopCardBackground(
    loop: LoopBase,
    isInProgress: Boolean,
    isHighlighted: Boolean,
): Color {
    // 다크모드에서는 배경(거의 검정)보다 한 단계 밝은 표면을 써서 카드가 떠 보이도록 한다.
    val base = AppColor.surfaceElevated
    if (loop.isMock) return base

    // 진행 중인 루프는 자신의 색으로 표면을 은은하게 물들여 "지금 살아있는" 카드로 떠 보이게 한다.
    if (isInProgress) {
        return loop.color.compositeOver(
            alpha = LoopCardDefaults.ActiveTintAlpha,
            color = base,
        )
    }
    if (!isHighlighted) return base
    return loop.color.compositeOver(
        alpha = LoopCardDefaults.HighlightTintAlpha,
        color = base,
    )
}

/**
 * Leading identity dot in the loop's own color. While the loop is in progress a soft halo
 * of the same color breathes behind it — the only "live" cue on the quiet card surface.
 */
@Composable
private fun LoopColorDot(
    modifier: Modifier = Modifier,
    color: Int,
    isPulsing: Boolean,
) {
    val dotColor = color.compositeOverOnSurface()

    Box(
        modifier = modifier.size(LoopCardDefaults.ColorDotFootprint),
        contentAlignment = Alignment.Center,
    ) {
        if (isPulsing) {
            val pulseScale = rememberDotPulseScale()
            Box(
                modifier = Modifier
                    .size(LoopCardDefaults.ColorDotSize)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = LoopCardDefaults.ColorDotPulseAlpha
                    }
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        Box(
            modifier = Modifier
                .size(LoopCardDefaults.ColorDotSize)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

/** Slow breathing scale (1 → 2) for the in-progress halo behind the color dot. */
@Composable
private fun rememberDotPulseScale(): Float {
    val transition = rememberInfiniteTransition(label = "LoopDotPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = LoopCardDefaults.ColorDotPulseMaxScale,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "LoopDotPulseScale",
    )
    return scale
}

@Composable
private fun LoopCardMeta(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    timeStat: TimeStat,
    syncWithTime: Boolean,
) {
    if (syncWithTime) {
        val text = timeStat.asString(LocalContext.current, false)
        if (text.isNotEmpty()) {
            Text(
                modifier = modifier,
                text = annotatedString(text),
                style = AppTypography.labelMedium.copy(color = loopMetaColor()),
            )
        }
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoopCardInterval(
            modifier = Modifier,
            interval = loop.interval,
        )
        LoopCardActiveDays(
            modifier = Modifier.padding(start = 8.dp),
            loop = loop,
        )
    }
}

/**
 * Trailing pill with the daily window ("07:00 – 08:00"), or "any time" for open-ended loops.
 * Replaces the old leading time column + rail so the card stays a single quiet line.
 */
@Composable
private fun LoopTimeChip(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    val text = if (loop.isAnyTime) {
        stringResource(id = R.string.anytime)
    } else {
        "${loop.startInDay.formatHourMinute(withAmPm = false)} – " +
                loop.endInDay.formatHourMinute(withAmPm = false)
    }

    Text(
        modifier = modifier
            .clip(CircleShape)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = AppTypography.labelMedium.copy(color = loopMetaColor()),
    )
}

/**
 * Trailing time label for a finished (awaiting-response) card. A small check icon plus the
 * daily window in muted ink signals the loop has already run today, without the loud "chip"
 * styling used by live cards — reinforcing the dimmed, past-item look.
 */
@Composable
private fun FinishedTimeLabel(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    val text = if (!loop.isAnyTime || (loop.startInDay >= 0 && loop.endInDay >= 0)) {
        "${loop.startInDay.formatHourMinute(withAmPm = false)} – " +
                loop.endInDay.formatHourMinute(withAmPm = false)
    } else {
        stringResource(id = R.string.anytime)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = Icons.Filled.Done,
            tint = loopMetaColor(),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = text,
            style = AppTypography.labelMedium.copy(color = loopMetaColor()),
        )
    }
}

/**
 * Trailing overflow menu. With done / skip moved onto the swipe gesture, every management
 * action (edit, on/off, add-to-group, delete) is gathered here so the card surface itself
 * carries nothing but identity and status.
 */
@Composable
private fun LoopCardMenu(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    showAddToGroup: Boolean,
    onEdit: (LoopBase) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
) {
    var isPopupMenuOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier
        .clip(CircleShape)
        .clickable { isPopupMenuOpen = true }
        .padding(all = 8.dp)) {

        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Outlined.MoreVert,
            tint = AppColor.onSurface.copy(alpha = 0.5f),
            contentDescription = stringResource(id = R.string.more)
        )
    }

    if (!isPopupMenuOpen) return

    fun closeAfter(action: () -> Unit) {
        action()
        isPopupMenuOpen = false
    }
    LoopCardPopupMenu(
        onDismiss = { isPopupMenuOpen = false },
    ) {
        LoopCardPopupMenuItem(
            text = stringResource(id = R.string.edit),
            onClick = { closeAfter { onEdit(loop) } },
        )
        LoopCardPopupMenuItem(
            text = stringResource(
                id = if (loop.enabled) R.string.loop_disable else R.string.loop_enable
            ),
            onClick = { closeAfter { onEnabled(!loop.enabled) } },
        )
        if (showAddToGroup) {
            LoopCardPopupMenuItem(
                text = stringResource(id = R.string.add_to_group),
                onClick = { closeAfter { onNavigateToGroupPicker(loop) } },
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = AppColor.onSurface.copy(alpha = 0.08f),
        )
        LoopCardPopupMenuItem(
            text = stringResource(id = R.string.delete),
            onClick = { closeAfter { onDelete() } },
        )
    }
}

@Composable
private fun LoopCardPopupMenu(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    // 둥근 모서리 + 헤어라인 테두리의 작은 시트로 띄워, 다크 모드(그림자가 안 보이는)에서도
    // 테두리가 경계를 잡아주고 라이트 모드에서는 부드러운 그림자로 떠 보인다.
    val menuShape = RoundedCornerShape(12.dp)
    // 옵션 버튼의 오른쪽 아래 모서리에 맞춰(TopEnd) 살짝 내려서 띄운다.
    val verticalOffset = with(LocalDensity.current) { 4.dp.roundToPx() }
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = 0, y = verticalOffset),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = modifier
                // Popup 안에서는 들어오는 max 제약이 화면 전체라서, 아이템의 fillMaxWidth 가
                // 화면 폭까지 늘어난다. 가장 넓은 아이템 폭(IntrinsicSize.Max)으로 열을 고정해
                // 컨텐츠 크기만큼만 차지하게 한다.
                .width(IntrinsicSize.Max)
                .widthIn(min = 160.dp)
                .shadow(elevation = 4.dp, shape = menuShape)
                .clip(menuShape)
                .background(color = AppColor.surfaceElevated)
                .border(
                    width = 1.dp,
                    color = AppColor.onSurface.copy(alpha = 0.08f),
                    shape = menuShape,
                )
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun LoopCardPopupMenuItem(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
        text = text,
        style = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.8f)
        )
    )
}

@Composable
fun LoopCardColor(
    modifier: Modifier = Modifier,
    color: Int
) {
    // Solid dot reads cleaner than a hollow ring and stays legible on both themes.
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color = color.compositeOverOnSurface())
    )
}

@Composable
fun AnyTimeLoopStartOrStop(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit
) {
    // 시작·정지 모두 이 루프를 "진행"시키는 긍정 동작이므로 primary 틴트로 통일한다.
    if (loop.doneState != LoopDoneVo.DoneState.IN_PROGRESS) {
        ResponseButton(
            modifier = modifier,
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = stringResource(id = R.string.start),
            containerColor = AppColor.primary.copy(alpha = LoopCardDefaults.ResponseTintAlpha),
            tint = AppColor.primary,
            onClick = {
                onStateChanged(
                    loop.copyAs(startInDay = LocalTime.now().toMs()),
                    LoopDoneVo.DoneState.IN_PROGRESS
                )
            },
        )
    } else {
        ResponseButton(
            modifier = modifier,
            imageVector = Icons.Filled.Stop,
            contentDescription = stringResource(id = R.string.stop),
            containerColor = AppColor.primary.copy(alpha = LoopCardDefaults.ResponseTintAlpha),
            tint = AppColor.primary,
            onClick = {
                onStateChanged(
                    // 완료 시각(now)과 함께, 시작 때 기록해 둔 실제 시작 시각(actualStartInDay)을
                    // 그대로 넘겨야 한다. loop.startInDay 는 anytime 이라 항상 ANY_TIME(-1)이므로,
                    // 이를 쓰면 done 기록의 시작 시각이 -1 로 덮여 소요 시간 계산이 깨진다.
                    loop.copyAs(
                        startInDay = loop.actualStartInDay,
                        endInDay = LocalTime.now().toMs(),
                    ),
                    LoopDoneVo.DoneState.DONE
                )
            },
        )
    }
}

@Composable
fun LoopDoneOrSkip(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 완료는 primary 틴트로 앞세우고, 스킵은 중립 톤으로 낮춰 두 동작의 무게를 구분한다.
        ResponseButton(
            imageVector = Icons.Filled.Done,
            contentDescription = stringResource(id = R.string.done),
            containerColor = AppColor.primary.copy(alpha = LoopCardDefaults.ResponseTintAlpha),
            tint = AppColor.primary,
            onClick = { onStateChanged(loop, LoopDoneVo.DoneState.DONE) },
        )
        ResponseButton(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(id = R.string.skip),
            containerColor = AppColor.onSurface.copy(alpha = LoopCardDefaults.ResponseNeutralAlpha),
            tint = AppColor.onSurface.copy(alpha = 0.6f),
            onClick = { onStateChanged(loop, LoopDoneVo.DoneState.SKIP) },
        )
    }
}

/**
 * 원형 응답 버튼(시작/정지 등). 은은한 틴트 원 위에 아이콘만 얹은 형태라 라이트/다크
 * 모두에서 같은 강도로 읽히고, 카드 안에서 과하게 튀지 않는다.
 */
@Composable
private fun ResponseButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String,
    containerColor: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(LoopCardDefaults.ResponseButtonSize)
            .clip(CircleShape)
            .background(color = containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(LoopCardDefaults.ResponseIconSize),
            imageVector = imageVector,
            tint = tint,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun LoopCardTitle(
    modifier: Modifier = Modifier,
    title: String,
    dimmed: Boolean = false,
) {
    Text(
        modifier = modifier,
        text = title,
        style = AppTypography.bodyMedium.copy(
            color = if (dimmed) {
                AppColor.onSurface.copy(alpha = LoopCardDefaults.DimmedInkAlpha)
            } else {
                loopTitleColor()
            },
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun LoopCardInterval(
    modifier: Modifier,
    interval: Long,
) {
    Text(
        text = if (interval == NO_REPEAT) {
            AnnotatedString("")
        } else {
            annotatedString(
                intervalString(
                    interval,
                    highlight = "#",
                )
            )
        },
        style = AppTypography.labelMedium.copy(color = loopMetaColor()),
        modifier = modifier,
    )
}

@Composable
fun LoopCardActiveDays(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    if (loop.activeDays in arrayOf(
            LoopDay.EVERYDAY,
            LoopDay.WEEKDAYS,
            LoopDay.WEEKENDS
        )
    ) {
        LoopCardActiveDaysPronoun(
            modifier = modifier,
            loop = loop
        )
    } else {
        LoopCardActiveDaysCommon(
            modifier = modifier,
            loop = loop
        )
    }
}

@Composable
private fun LoopCardActiveDaysPronoun(
    modifier: Modifier = Modifier,
    loop: LoopBase
) {
    // "매일/주중/주말" 같은 대명사도 메타 정보일 뿐이므로 강조 없이 메타 톤으로 둔다.
    Text(
        modifier = modifier.padding(end = 2.dp),
        text = stringResource(DAY_STRING_MAP[loop.activeDays]!!),
        style = AppTypography.labelMedium.copy(color = loopMetaColor())
    )
}

@Composable
private fun LoopCardActiveDaysCommon(
    modifier: Modifier,
    loop: LoopBase
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val day = LoopDay.fromIndex(index)
            val selected = loop.activeDays.isOn(day)

            ActiveDayText(
                modifier = Modifier.padding(horizontal = 1.dp),
                dayText = stringResource(id = dayResId),
                selected = selected
            )
        }
    }
}

@Composable
private fun ActiveDayText(
    modifier: Modifier = Modifier,
    dayText: String,
    selected: Boolean,
) {
    // 요일은 진하기(알파)와 굵기만으로 켜짐/꺼짐을 구분한다. 주말 색상이나 표시 점 없이
    // 모노톤으로 두어 메타 줄이 조용해지고, 라이트/다크 어디서든 같은 대비로 읽힌다.
    Text(
        modifier = modifier,
        text = dayText,
        style = AppTypography.labelMedium.copy(
            color = AppColor.onSurface.copy(
                alpha = if (selected) LoopCardDefaults.DaySelectedAlpha else LoopCardDefaults.DayUnselectedAlpha,
            ),
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    )
}

@Composable
private fun loopTitleColor(): Color =
    AppColor.onSurface.copy(alpha = LoopCardDefaults.TitleAlpha)

@Composable
private fun loopMetaColor(): Color =
    AppColor.onSurface.copy(alpha = LoopCardDefaults.MetaAlpha)


@Composable
private fun animateCardAlphaWithMock(loopBase: LoopBase): Float {
    if (!loopBase.isMock) {
        return 1f
    }
    val transition = rememberInfiniteTransition("CreateLoopTransitions")
    val alpha by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = 1_500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaAnimForCreateLoop",
    )

    return alpha
}
