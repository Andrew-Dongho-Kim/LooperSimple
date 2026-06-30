package com.pnd.android.loop.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.TimeStat
import com.pnd.android.loop.data.common.NO_REPEAT
import com.pnd.android.loop.data.currentTimeStat
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOver
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.DAY_STRING_MAP
import com.pnd.android.loop.util.annotatedString
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.rememberDayColor
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime


/**
 * Visual tokens shared by every loop card. Keeping them here makes the spacing,
 * shape and opacity hierarchy consistent and easy to tweak for both light/dark themes.
 */
private object LoopCardDefaults {
    /** Soft rounding applied to the highlight tint / press ripple of the flat row. */
    val RowCorner = 16.dp

    /** Fixed leading column that holds the start / end time labels. */
    val TimeColumnWidth = 46.dp

    /** Vertical timeline rail (line + start/end dots) drawn between time and content. */
    val RailWidth = 22.dp
    val RailDotRadius = 4.dp
    val RailLineWidth = 2.dp

    val ContentHorizontalPadding = 14.dp
    val ContentVerticalPadding = 12.dp

    const val EnabledAlpha = 1f
    const val DisabledAlpha = 0.3f
    const val TitleAlpha = 0.9f
    const val MetaAlpha = 0.6f

    /** Wash of the loop color over the row when it is highlighted (opened from a notification). */
    const val HighlightTintAlpha = 0.12f

    /** Line brightness per phase: bright while live, faded once finished, dim before it starts. */
    const val RailActiveAlpha = 1f
    const val RailFinishedAlpha = 0.45f
    const val RailInactiveAlpha = 0.3f

    /** Soft halo radius (× dot radius) pulsing around the start dot while a loop is in progress. */
    const val RailHaloRadiusScale = 2.2f
    const val RailHaloAlpha = 0.18f
}

/**
 * Where a loop sits in its daily lifecycle, relative to the current clock. Drives how the
 * timeline rail is drawn so each state reads at a glance:
 *  - [BeforeStart] the window hasn't opened yet (or the card isn't clock-synced),
 *  - [InProgress] the loop is happening right now,
 *  - [Finished] today's window has passed.
 */
private enum class LoopPhase { BeforeStart, InProgress, Finished }

/**
 * Maps the clock-derived flags into a single [LoopPhase]. Cards that don't sync with the
 * clock (e.g. the group editor) always read as [LoopPhase.BeforeStart] so they stay neutral.
 */
private fun loopPhaseOf(
    syncWithTime: Boolean,
    isActive: Boolean,
    isPast: Boolean,
): LoopPhase = when {
    !syncWithTime -> LoopPhase.BeforeStart
    isActive -> LoopPhase.InProgress
    isPast -> LoopPhase.Finished
    else -> LoopPhase.BeforeStart
}

@Composable
fun LoopCard(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    cardValues: LoopCardValues,
    onStateChanged: (loop: LoopBase, doneState: Int) -> Unit,
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
    val background = loopCardBackground(loop = loop, isHighlighted = cardValues.isHighlighted)

    Box(modifier = modifier.graphicsLayer { alpha = mockAlpha }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LoopCardDefaults.RowCorner))
                .background(background)
                .clickable(enabled = !loop.isMock) { onNavigateToDetailPage(loop) }
                .height(IntrinsicSize.Min)
                .padding(
                    horizontal = LoopCardDefaults.ContentHorizontalPadding,
                    vertical = LoopCardDefaults.ContentVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AgendaTimeColumn(
                modifier = Modifier
                    .alpha(contentAlpha)
                    .width(LoopCardDefaults.TimeColumnWidth)
                    .fillMaxHeight(),
                loop = loop,
            )
            AgendaRail(
                modifier = Modifier.alpha(contentAlpha),
                loop = loop,
                phase = loopPhaseOf(
                    syncWithTime = syncWithTime,
                    isActive = cardValues.isActive,
                    isPast = timeStat.isPast(),
                ),
            )
            AgendaContent(
                modifier = Modifier
                    .alpha(contentAlpha)
                    .weight(1f),
                loop = loop,
                timeStat = timeStat,
                syncWithTime = syncWithTime,
                onStateChanged = onStateChanged,
            )
            LoopCardMenu(
                cardValues = cardValues,
                onNavigateToGroupPicker = { onNavigateToGroupPicker(loop) },
            )
        }
    }
}

/**
 * Opaque background for the flat agenda row. Stays on the plain surface so rows read as
 * a borderless list; when highlighted (e.g. opened from a notification) it gets a soft
 * wash of the loop's own color. Opaque so the swipe-to-reveal options stay hidden.
 */
@Composable
private fun loopCardBackground(
    loop: LoopBase,
    isHighlighted: Boolean,
): Color {
    val base = AppColor.surface
    if (loop.isMock || !isHighlighted) return base
    return loop.color.compositeOver(
        alpha = LoopCardDefaults.HighlightTintAlpha,
        color = base,
    )
}

/**
 * Leading time column: start time on top, end time on the bottom, aligned to the rail.
 * "Any time" loops collapse to a single centered label.
 */
@Composable
private fun AgendaTimeColumn(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    if (loop.isAnyTime) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(id = R.string.anytime),
                style = AppTypography.labelMedium.copy(color = loopMetaColor()),
            )
        }
        return
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = loop.startInDay.formatHourMinute(withAmPm = false),
            style = AppTypography.bodyMedium.copy(
                color = loopTitleColor(),
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            text = loop.endInDay.formatHourMinute(withAmPm = false),
            style = AppTypography.labelMedium.copy(color = loopMetaColor()),
        )
    }
}

/**
 * Vertical timeline rail tying the start/end times to the content. The two dots track how far
 * the loop has travelled through its window — each is hollow until it is reached and filled once
 * passed — and the connecting line is brightest while the loop is in progress:
 *  - before start: both dots hollow on a dim line,
 *  - in progress: start dot filled with a soft halo, end dot still hollow, bright line,
 *  - finished: both dots filled on a faded line.
 */
@Composable
private fun AgendaRail(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    phase: LoopPhase,
) {
    val color = loop.color.compositeOverOnSurface()

    // Live cues are only meaningful while the loop is in progress.
    val progress = if (phase == LoopPhase.InProgress) rememberInProgressFraction(loop) else null
    val haloScale = if (phase == LoopPhase.InProgress) rememberRailHaloScale() else 0f

    Canvas(
        modifier = modifier
            .width(LoopCardDefaults.RailWidth)
            .fillMaxHeight()
    ) {
        val cx = size.width / 2f
        val r = LoopCardDefaults.RailDotRadius.toPx()
        val lineWidth = LoopCardDefaults.RailLineWidth.toPx()
        val top = Offset(cx, r + 1.dp.toPx())
        val bottom = Offset(cx, size.height - r - 1.dp.toPx())

        fun railLine(end: Offset, alpha: Float) = drawLine(
            color = color.copy(alpha = alpha),
            start = top,
            end = end,
            strokeWidth = lineWidth,
            cap = StrokeCap.Round,
        )

        when (phase) {
            LoopPhase.BeforeStart -> railLine(bottom, LoopCardDefaults.RailInactiveAlpha)

            LoopPhase.Finished -> railLine(bottom, LoopCardDefaults.RailFinishedAlpha)

            LoopPhase.InProgress -> {
                if (progress == null) {
                    // Open-ended ("any time") loop: no end to fill toward, just a bright live rail.
                    railLine(bottom, LoopCardDefaults.RailActiveAlpha)
                } else {
                    // Dim the remaining track and overlay a bright fill that grows toward the end.
                    val head = Offset(cx, top.y + (bottom.y - top.y) * progress)
                    railLine(bottom, LoopCardDefaults.RailInactiveAlpha)
                    railLine(head, LoopCardDefaults.RailActiveAlpha)
                    drawCircle(color = color, radius = r * 0.7f, center = head)
                }

                // Pulsing halo keeps the eye on the loop that is happening right now.
                drawCircle(
                    color = color.copy(alpha = LoopCardDefaults.RailHaloAlpha),
                    radius = r * LoopCardDefaults.RailHaloRadiusScale * haloScale,
                    center = top,
                )
            }
        }

        // Start dot: hollow until the window opens, filled once it has.
        drawRailDot(color = color, center = top, radius = r, filled = phase != LoopPhase.BeforeStart, strokeWidth = lineWidth)

        // End dot: filled only after the window has closed.
        drawRailDot(color = color, center = bottom, radius = r, filled = phase == LoopPhase.Finished, strokeWidth = lineWidth)
    }
}

/** Draws a single rail node, either a solid dot or a hollow ring of the same radius. */
private fun DrawScope.drawRailDot(
    color: Color,
    center: Offset,
    radius: Float,
    filled: Boolean,
    strokeWidth: Float,
) {
    if (filled) {
        drawCircle(color = color, radius = radius, center = center)
    } else {
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = strokeWidth))
    }
}

/**
 * Fraction (0..1) of how far the loop has travelled through today's window, ticking once a
 * minute and animated so the rail fill glides rather than jumps. Returns null for open-ended
 * ("any time") loops that have no end time to measure against.
 */
@Composable
private fun rememberInProgressFraction(loop: LoopBase): Float? {
    if (loop.isAnyTime || loop.startInDay < 0 || loop.endInDay < 0) return null

    var elapsed by remember(loop.loopId) {
        mutableStateOf(loopElapsedFraction(loop.startInDay, loop.endInDay))
    }
    LaunchedEffect(loop.loopId, loop.startInDay, loop.endInDay) {
        while (currentCoroutineContext().isActive) {
            elapsed = loopElapsedFraction(loop.startInDay, loop.endInDay)
            delay(MS_1MIN)
        }
    }

    val animated by animateFloatAsState(targetValue = elapsed, label = "RailProgress")
    return animated
}

/** Elapsed fraction of the [startMs, endMs] window right now, handling windows that cross midnight. */
private fun loopElapsedFraction(startMs: Long, endMs: Long): Float {
    val overnight = startMs > endMs
    val end = if (overnight) endMs + MS_1DAY else endMs
    val nowRaw = LocalTime.now().toMs()
    val now = if (overnight && nowRaw < startMs) nowRaw + MS_1DAY else nowRaw

    val total = (end - startMs).toFloat()
    if (total <= 0f) return 1f
    return ((now - startMs) / total).coerceIn(0f, 1f)
}

/** Slow breathing scale (≈0.7→1) for the in-progress halo so it gently pulses. */
@Composable
private fun rememberRailHaloScale(): Float {
    val transition = rememberInfiniteTransition(label = "RailHalo")
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "RailHaloScale",
    )
    return scale
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

/**
 * Right-hand content of the agenda row: title + a meta line, plus the trailing
 * done/skip (or any-time start/stop) actions when the loop is live today.
 */
@Composable
private fun AgendaContent(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    timeStat: TimeStat,
    syncWithTime: Boolean,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {
    Row(
        modifier = modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LoopCardTitle(title = loop.title)
            AgendaMeta(
                modifier = Modifier.padding(top = 3.dp),
                loop = loop,
                timeStat = timeStat,
                syncWithTime = syncWithTime,
            )
        }

        if (syncWithTime && loop.enabled) {
            if (timeStat.isPast()) {
                LoopDoneOrSkip(
                    modifier = Modifier.height(36.dp),
                    loop = loop,
                    onStateChanged = onStateChanged,
                )
            }
            if (loop.isAnyTime && (loop.startInDay < 0 || loop.endInDay < 0)) {
                AnyTimeLoopStartOrStop(
                    loop = loop,
                    onStateChanged = onStateChanged,
                )
            }
        }
    }
}

/**
 * Single meta line under the title. While syncing with the clock it shows the live status
 * ("32 mins left", "finished", …); otherwise it shows the repeat interval and active days.
 */
@Composable
private fun AgendaMeta(
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

@Composable
private fun LoopCardMenu(
    modifier: Modifier = Modifier,
    onNavigateToGroupPicker: () -> Unit,
    cardValues: LoopCardValues,
) {
    var isPopupMenuOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier
        .clip(CircleShape)
        .clickable { isPopupMenuOpen = true }
        .padding(all = 8.dp)) {

        if (cardValues.showAddToGroup) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Outlined.MoreVert,
                tint = AppColor.onSurface.copy(alpha = 0.8f),
                contentDescription = stringResource(id = R.string.more)
            )
        }
    }

    LoopCardPopupMenu(
        isOpen = isPopupMenuOpen,
        onNavigateToGroupPicker = {
            onNavigateToGroupPicker()
            isPopupMenuOpen = false
        },
        onDismiss = { isPopupMenuOpen = false },
    )
}

@Composable
private fun LoopCardPopupMenu(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    onNavigateToGroupPicker: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isOpen) return
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = modifier
                .shadow(elevation = 1.5.dp)
                .background(color = AppColor.surfaceElevated)
        ) {
            LoopCardPopupMenuItem(
                text = stringResource(id = R.string.add_to_group),
                onClick = onNavigateToGroupPicker
            )
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
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            ),
        text = text,
        style = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.8f)
        )
    )
}

@Composable
fun AnyTimeLoopStartOrStop(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit
) {
    if (loop.startInDay < 0) {
        Image(
            modifier = modifier
                .size(48.dp)
                .clickable {
                    onStateChanged(
                        loop.copyAs(startInDay = LocalTime.now().toMs()),
                        LoopDoneVo.DoneState.IN_PROGRESS
                    )
                }
                .padding(8.dp),
            imageVector = Icons.Filled.PlayArrow,
            colorFilter = ColorFilter.tint(
                AppColor.onSurface.copy(alpha = 0.7f)
            ),
            contentDescription = stringResource(id = R.string.start)
        )
    } else {
        Image(
            modifier = modifier
                .size(48.dp)
                .clickable {
                    onStateChanged(
                        loop.copyAs(endInDay = LocalTime.now().toMs()),
                        LoopDoneVo.DoneState.DONE
                    )
                }
                .padding(8.dp),
            imageVector = Icons.Filled.Stop,
            colorFilter = ColorFilter.tint(
                AppColor.onSurface.copy(alpha = 0.7f)
            ),
            contentDescription = stringResource(id = R.string.stop)
        )
    }
}

@Composable
fun LoopDoneOrSkip(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {

    Row(modifier = modifier) {
        Image(
            modifier = Modifier
                .clickable { onStateChanged(loop, LoopDoneVo.DoneState.DONE) }
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(8.dp),
            imageVector = Icons.Filled.Done,
            colorFilter = ColorFilter.tint(
                AppColor.primary.copy(alpha = 0.7f)
            ),
            contentDescription = stringResource(id = R.string.done)
        )

        Image(
            modifier = Modifier
                .padding(start = 4.dp)
                .clickable { onStateChanged(loop, LoopDoneVo.DoneState.SKIP) }
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(8.dp),
            imageVector = Icons.Filled.Close,
            colorFilter = ColorFilter.tint(
                AppColor.onSurface.copy(
                    alpha = 0.7f
                )
            ),
            contentDescription = stringResource(id = R.string.skip)
        )
    }
}

@Composable
private fun LoopCardTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        style = AppTypography.bodyMedium.copy(
            color = loopTitleColor(),
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
    Text(
        modifier = modifier.padding(end = 2.dp),
        text = stringResource(DAY_STRING_MAP[loop.activeDays]!!),
        style = AppTypography.labelMedium.copy(
            color = AppColor.primary
        )
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
                day = day,
                dayText = stringResource(id = dayResId),
                selected = selected
            )
        }
    }
}

@Composable
private fun ActiveDayText(
    modifier: Modifier = Modifier,
    day: Int,
    dayText: String,
    selected: Boolean,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val color = AppColor.primary
        if (selected) {
            Canvas(modifier = Modifier.padding(bottom = 2.dp)) {
                drawCircle(
                    color = color,
                    radius = 1.5.dp.toPx()
                )
            }
        }
        Text(
            modifier = Modifier,
            text = dayText,
            style = AppTypography.labelMedium.copy(
                color = rememberDayColor(day = day).copy(
                    alpha = if (selected) 0.8f else 0.3f,
                ),
                fontWeight = FontWeight.Normal
            )
        )
    }
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