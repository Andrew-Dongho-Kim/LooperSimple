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
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.ACTION_PARAMS_LOOP_ID
import com.pnd.android.loop.appwidget.AppWidgetDoneAction
import com.pnd.android.loop.appwidget.AppWidgetSkipAction
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.Blue500
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.toMs
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
            color = ColorProvider(AppColor.onSurface.copy(alpha = 0.8f)),
            fontSize = 16.sp
        )
    )
}

@Composable
fun LoopColor(
    modifier: GlanceModifier = GlanceModifier,
    color: Color
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .cornerRadius(3.dp)
                .size(6.dp)
                .background(ColorProvider(color))
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
        style = TextStyle(color = ColorProvider(AppColor.primary.copy(alpha = 0.8f)))
    )
}

@Composable
fun LoopBase.toStartOrEndTime(): String {
    val now = LocalTime.now().toMs()

    return if (now < loopStart) {
        stringResourceGlide(
            resId = R.string.start_at,
            loopStart.formatHourMinute(context = LocalContext.current)
        )
    } else {
        stringResourceGlide(
            resId = R.string.end_at,
            loopEnd.formatHourMinute(context = LocalContext.current)
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
                    actionRunCallback<AppWidgetDoneAction>(
                        parameters = actionParametersOf(ACTION_PARAMS_LOOP_ID to loopId)
                    )
                ),
            provider = ImageProvider(resId = R.drawable.done),
            colorFilter = ColorFilter.tint(ColorProvider(Blue500.copy(alpha = 0.8f))),
            contentDescription = stringResourceGlide(resId = R.string.done)
        )
        Spacer(modifier = GlanceModifier.defaultWeight())

        Image(
            modifier = GlanceModifier.padding(end = 8.dp, bottom = 8.dp)
                .clickable(
                    actionRunCallback<AppWidgetSkipAction>(
                        parameters = actionParametersOf(ACTION_PARAMS_LOOP_ID to loopId)
                    )
                ),
            provider = ImageProvider(resId = R.drawable.skip),
            colorFilter = ColorFilter.tint(ColorProvider(AppColor.onSurface.copy(alpha = 0.8f))),
            contentDescription = stringResourceGlide(resId = R.string.skip)
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
            modifier = GlanceModifier.padding(start = 8.dp, bottom = 8.dp)
                .clickable(
                    actionRunCallback<AppWidgetDoneAction>(
                        parameters = actionParametersOf(ACTION_PARAMS_LOOP_ID to loopId)
                    )
                ),
            provider = ImageProvider(resId = R.drawable.done),
            colorFilter = ColorFilter.tint(ColorProvider(Blue500.copy(alpha = 0.8f))),
            contentDescription = stringResourceGlide(resId = R.string.done)
        )
        Spacer(modifier = GlanceModifier.width(24.dp))

        Image(
            modifier = GlanceModifier.padding(end = 8.dp, bottom = 8.dp)
                .clickable(
                    actionRunCallback<AppWidgetSkipAction>(
                        parameters = actionParametersOf(ACTION_PARAMS_LOOP_ID to loopId)
                    )
                ),
            provider = ImageProvider(resId = R.drawable.skip),
            colorFilter = ColorFilter.tint(ColorProvider(AppColor.onSurface.copy(alpha = 0.8f))),
            contentDescription = stringResourceGlide(resId = R.string.skip)
        )
    }
}
