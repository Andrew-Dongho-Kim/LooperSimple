package com.pnd.android.loop.appwidget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pnd.android.loop.HomeActivity
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.ACTION_PARAMS_LOOP_ID
import com.pnd.android.loop.appwidget.AppWidgetLoopAction
import com.pnd.android.loop.appwidget.doneAction
import com.pnd.android.loop.appwidget.skipAction
import com.pnd.android.loop.appwidget.startAction
import com.pnd.android.loop.appwidget.stopAction
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.Blue500
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import java.time.LocalTime


@Composable
fun LoopTitle(
    modifier: GlanceModifier = GlanceModifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        maxLines = 3,
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorProvider(AppColor.onSurface.copy(alpha = 0.8f)),
        )
    )
}

@Composable
fun LoopColor(
    modifier: GlanceModifier = GlanceModifier,
    color: Color
) {
    Box(
        modifier = modifier
            .size(13.dp)
            .cornerRadius(6.dp)
            .background(ColorProvider(color)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .cornerRadius(5.dp)
                .size(10.dp)
                .background(ColorProvider(Color.White))
        ) {
        }
    }
}

@Composable
fun LoopStartEndTime(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    Text(
        modifier = modifier,
        text = loop.toStartOrEndTime(),
        style = TextStyle(
            fontSize = 12.sp,
            color = ColorProvider(AppColor.onSurface.copy(alpha = 0.4f)),
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

@Composable
fun LoopDoneOrSkip(
    modifier: GlanceModifier = GlanceModifier,
    loopId: Int
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Image(
            modifier = GlanceModifier.padding(start = 8.dp, bottom = 8.dp)
                .clickable(
                    actionRunCallback<AppWidgetLoopAction>(
                        parameters = actionParametersOf(ACTION_PARAMS_LOOP_ID to loopId)
                    )
                ),
            provider = ImageProvider(resId = R.drawable.done),
            colorFilter = ColorFilter.tint(ColorProvider(Blue500.copy(alpha = 0.8f))),
            contentDescription = stringResourceGlance(id = R.string.done)
        )
        Spacer(modifier = GlanceModifier.defaultWeight())

        Image(
            modifier = GlanceModifier.padding(end = 8.dp, bottom = 8.dp)
                .clickable(
                    actionRunCallback<AppWidgetLoopAction>(
                        parameters = actionParametersOf(ACTION_PARAMS_LOOP_ID to loopId)
                    )
                ),
            provider = ImageProvider(resId = R.drawable.skip),
            colorFilter = ColorFilter.tint(ColorProvider(AppColor.onSurface.copy(alpha = 0.8f))),
            contentDescription = stringResourceGlance(id = R.string.skip)
        )
    }
}


@Composable
fun LoopDoneOrSkipMedium(
    modifier: GlanceModifier = GlanceModifier,
    loopId: Int
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Image(
            modifier = GlanceModifier
                .padding(start = 8.dp, bottom = 8.dp)
                .clickable(doneAction(loopId = loopId)),
            provider = ImageProvider(resId = R.drawable.done),
            colorFilter = ColorFilter.tint(ColorProvider(Blue500.copy(alpha = 0.8f))),
            contentDescription = stringResourceGlance(id = R.string.done)
        )
        Spacer(modifier = GlanceModifier.width(24.dp))

        Image(
            modifier = GlanceModifier
                .padding(end = 8.dp, bottom = 8.dp)
                .clickable(skipAction(loopId)),
            provider = ImageProvider(resId = R.drawable.skip),
            colorFilter = ColorFilter.tint(ColorProvider(AppColor.onSurface.copy(alpha = 0.8f))),
            contentDescription = stringResourceGlance(id = R.string.skip)
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
    Image(
        modifier = GlanceModifier.padding(start = 8.dp, bottom = 8.dp)
            .clickable(
                if (isStart) startAction(loopId = loopId) else stopAction(loopId = loopId)
            ).then(modifier),
        provider = ImageProvider(
            resId = if (isStart) R.drawable.start else R.drawable.stop
        ),
        colorFilter = ColorFilter.tint(ColorProvider(AppColor.onSurface.copy(alpha = 0.7f))),
        contentDescription = stringResourceGlance(
            id = if (isStart) R.string.start else R.string.stop
        )
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
                color = ColorProvider(AppColor.onSurface),
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
            )
        )
    }
}

@Composable
fun LoopWidgetEmpty(
    modifier: GlanceModifier = GlanceModifier,
    loopsTotal: Int
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .clickable(actionStartActivity<HomeActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResourceGlance(
                id = if (loopsTotal == 0) {
                    R.string.desc_no_loops
                } else {
                    R.string.today_loops_finished
                }
            ),
            style = TextStyle(
                color = ColorProvider(AppColor.onSurface.copy(alpha = 0.8f)),
                fontSize = 16.sp
            )
        )
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
