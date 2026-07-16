package com.pnd.android.loop.appwidget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pnd.android.loop.HomeActivity
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.doneAction
import com.pnd.android.loop.appwidget.skipAction
import com.pnd.android.loop.appwidget.startAction
import com.pnd.android.loop.appwidget.stopAction
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import java.time.LocalTime


// ---------------------------------------------------------------------------
// Apple-inspired widget design tokens
// A calm, layered light surface with soft rounded "wells", clear typographic
// hierarchy and tactile circular controls.
// ---------------------------------------------------------------------------
internal val WIDGET_CARD_RADIUS = 28.dp
internal val WIDGET_ROW_RADIUS = 22.dp

// Resolve the theme roles to concrete colors so we can derive translucent tints.
// GlanceTheme.colors follows the day/night providers wired in AppWidget.
@Composable
internal fun onSurfaceColor(): Color =
    GlanceTheme.colors.onSurface.getColor(LocalContext.current)

@Composable
internal fun surfaceColor(): Color =
    GlanceTheme.colors.surface.getColor(LocalContext.current)

@Composable
internal fun accentColor(): Color =
    GlanceTheme.colors.primary.getColor(LocalContext.current)

@Composable
internal fun widgetSurface() = ColorProvider(surfaceColor())

@Composable
internal fun widgetWell(active: Boolean = false): ColorProvider =
    if (active) ColorProvider(accentColor().copy(alpha = 0.11f))
    else ColorProvider(onSurfaceColor().copy(alpha = 0.045f))

@Composable
internal fun textPrimary() = ColorProvider(onSurfaceColor().copy(alpha = 0.92f))

@Composable
internal fun textSecondary() = ColorProvider(onSurfaceColor().copy(alpha = 0.5f))

@Composable
internal fun textTertiary() = ColorProvider(onSurfaceColor().copy(alpha = 0.38f))


@Composable
fun LoopTitle(
    modifier: GlanceModifier = GlanceModifier,
    title: String,
    isActive: Boolean = false,
) {
    Text(
        modifier = modifier,
        text = title,
        maxLines = 2,
        style = TextStyle(
            fontSize = 15.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = textPrimary(),
        )
    )
}

/**
 * Loop accent indicator: a solid colored dot wrapped in a soft same-color halo,
 * echoing the iOS Reminders / Calendar dot treatment.
 */
@Composable
fun LoopColor(
    modifier: GlanceModifier = GlanceModifier,
    color: Color,
    active: Boolean = false,
) {
    val outer = if (active) 18.dp else 14.dp
    val inner = if (active) 9.dp else 7.dp
    Box(
        modifier = modifier
            .size(outer)
            .cornerRadius(outer / 2)
            .background(ColorProvider(color.copy(alpha = if (active) 0.22f else 0.16f))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(inner)
                .cornerRadius(inner / 2)
                .background(ColorProvider(color))
        ) {}
    }
}

@Composable
fun LoopStartEndTime(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
    emphasize: Boolean = false,
) {
    Text(
        modifier = modifier,
        text = loop.toStartOrEndTime(),
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasize) ColorProvider(accentColor()) else textSecondary(),
        )
    )
}

@Composable
fun LoopBase.toStartOrEndTime(): String {
    return if (isAnyTime) startOrEndTimeForAnyLoop() else startOrEndTimeForNormalLoop()
}

@Composable
private fun LoopBase.startOrEndTimeForAnyLoop(): String {
    return if (startInDay < 0) {
        stringResourceGlance(id = R.string.anytime)
    } else {
        stringResourceGlance(
            id = R.string.started_at,
            startInDay.formatHourMinute(context = LocalContext.current)
        )
    }
}

@Composable
private fun LoopBase.startOrEndTimeForNormalLoop(): String {
    val now = LocalTime.now().toMs()
    return if (now < startInDay) {
        stringResourceGlance(
            id = R.string.start_at,
            startInDay.formatHourMinute(context = LocalContext.current)
        )
    } else {
        stringResourceGlance(
            id = R.string.end_at,
            endInDay.formatHourMinute(context = LocalContext.current)
        )
    }
}

/**
 * A circular, tappable control used for the loop quick-actions. Tinted-filled to
 * read as a soft button rather than a bare icon.
 */
@Composable
private fun WidgetIconButton(
    onClick: Action,
    resId: Int,
    contentDescription: String,
    tint: Color,
    background: Color,
    modifier: GlanceModifier = GlanceModifier,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .cornerRadius(size / 2)
            .background(ColorProvider(background))
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = GlanceModifier.size(size * 0.5f),
            provider = ImageProvider(resId = resId),
            colorFilter = ColorFilter.tint(ColorProvider(tint)),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun LoopDoneOrSkip(
    modifier: GlanceModifier = GlanceModifier,
    loopId: Int
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetIconButton(
            onClick = doneAction(loopId = loopId),
            resId = R.drawable.done,
            contentDescription = stringResourceGlance(id = R.string.done),
            tint = accentColor(),
            background = accentColor().copy(alpha = 0.14f),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        WidgetIconButton(
            onClick = skipAction(loopId = loopId),
            resId = R.drawable.skip,
            contentDescription = stringResourceGlance(id = R.string.skip),
            tint = onSurfaceColor().copy(alpha = 0.55f),
            background = onSurfaceColor().copy(alpha = 0.06f),
        )
    }
}


@Composable
fun LoopDoneOrSkipMedium(
    modifier: GlanceModifier = GlanceModifier,
    loopId: Int
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetIconButton(
            onClick = doneAction(loopId = loopId),
            resId = R.drawable.done,
            contentDescription = stringResourceGlance(id = R.string.done),
            tint = accentColor(),
            background = accentColor().copy(alpha = 0.14f),
        )
        Spacer(modifier = GlanceModifier.width(12.dp))
        WidgetIconButton(
            onClick = skipAction(loopId = loopId),
            resId = R.drawable.skip,
            contentDescription = stringResourceGlance(id = R.string.skip),
            tint = onSurfaceColor().copy(alpha = 0.55f),
            background = onSurfaceColor().copy(alpha = 0.06f),
        )
    }
}

/**
 * Compact done / skip pair sized for a list row's trailing slot. Unlike
 * [LoopDoneOrSkipMedium] it takes only its intrinsic width (no fillMaxWidth) and uses
 * slightly smaller buttons, so it sits neatly next to the title without stretching the row.
 */
@Composable
fun LoopDoneOrSkipCompact(
    modifier: GlanceModifier = GlanceModifier,
    loopId: Int
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetIconButton(
            onClick = doneAction(loopId = loopId),
            resId = R.drawable.done,
            contentDescription = stringResourceGlance(id = R.string.done),
            tint = accentColor(),
            background = accentColor().copy(alpha = 0.14f),
            size = 30.dp,
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        WidgetIconButton(
            onClick = skipAction(loopId = loopId),
            resId = R.drawable.skip,
            contentDescription = stringResourceGlance(id = R.string.skip),
            tint = onSurfaceColor().copy(alpha = 0.55f),
            background = onSurfaceColor().copy(alpha = 0.06f),
            size = 30.dp,
        )
    }
}

@Composable
fun AnyTimeLoopStartOrStop(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    val isStart = loop.startInDay < 0
    val loopId = loop.loopId
    WidgetIconButton(
        modifier = modifier,
        onClick = if (isStart) startAction(loopId = loopId) else stopAction(loopId = loopId),
        resId = if (isStart) R.drawable.start else R.drawable.stop,
        contentDescription = stringResourceGlance(
            id = if (isStart) R.string.start else R.string.stop
        ),
        tint = if (isStart) accentColor() else onSurfaceColor().copy(alpha = 0.55f),
        background = if (isStart) accentColor().copy(alpha = 0.14f)
        else onSurfaceColor().copy(alpha = 0.06f),
    )
}

@Composable
fun LocalDateHeader(
    modifier: GlanceModifier = GlanceModifier,
    localDate: LocalDate = LocalDate.now(),
) {
    Column(modifier = modifier.clickable(actionStartActivity<HomeActivity>())) {
        Text(
            text = localDate.formatYearMonthDateDaysGlance(),
            style = TextStyle(
                color = textPrimary(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        )
        Spacer(modifier = GlanceModifier.size(6.dp))
        // Short accent underline — a quiet iOS-style emphasis cue beneath the date.
        Box(
            modifier = GlanceModifier
                .width(22.dp)
                .height(3.dp)
                .cornerRadius(2.dp)
                .background(ColorProvider(accentColor()))
        ) {}
    }
}

@Composable
fun LoopWidgetEmpty(
    modifier: GlanceModifier = GlanceModifier,
    loopsTotal: Int
) {
    val isAllDone = loopsTotal != 0
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clickable(actionStartActivity<HomeActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = stringResourceGlance(
                    id = if (isAllDone) R.string.today_loops_finished else R.string.desc_no_loops
                ),
                style = TextStyle(
                    color = textPrimary(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = GlanceModifier.size(4.dp))
            Text(
                text = stringResourceGlance(
                    id = if (isAllDone) R.string.today_loops_finished_hint
                    else R.string.desc_no_loops_hint
                ),
                style = TextStyle(
                    color = textTertiary(),
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
fun LocalDate.formatYearMonthDateDaysGlance(): String {
    return stringResourceGlance(
        id = R.string.format_year_month_date_day,
        "$year",
        stringResourceGlance(id = ABB_MONTHS[monthValue - 1]),
        "$dayOfMonth",
        stringResourceGlance(id = DAYS_WITH_3CHARS[dayOfWeek.value - 1])
    )
}
