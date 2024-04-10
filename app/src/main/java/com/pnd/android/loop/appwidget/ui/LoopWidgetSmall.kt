package com.pnd.android.loop.appwidget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pnd.android.loop.MainActivity
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.isPast
import com.pnd.android.loop.util.toMs
import java.time.LocalTime


@Composable
fun LoopWidgetSmall(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<LoopBase>
) {
    if (loops.isEmpty()) {
        LoopWidgetEmpty(
            modifier = modifier,
        )
    } else {
        val loop = pickOneLoop(loops)
        LoopWidgetItem(
            modifier = modifier.background(AppColor.surface.copy(alpha = 0.7f)),
            loop = loop
        )
    }
}

@Composable
private fun LoopWidgetEmpty(
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .background(AppColor.surface.copy(alpha = 0.7f))
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResourceGlide(resId = R.string.desc_no_loops),
            style = TextStyle(
                color = ColorProvider(AppColor.onSurface.copy(alpha = 0.8f)),
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun LoopWidgetItem(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>())
            .padding(
                top = 12.dp,
                start = 4.dp,
                end = 4.dp
            ),
    ) {
        val isPast = loop.isPast()
        if (!isPast) {
            LoopStartEndTime(loop = loop)
        }
        Row(
            modifier = GlanceModifier.padding(top = if (isPast) 2.dp else 4.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            LoopColor(
                modifier = GlanceModifier
                    .size(12.dp),
                color = loop.color.compositeOverOnSurface()
            )
            LoopTitle(
                modifier = GlanceModifier.padding(start = 4.dp),
                title = loop.title
            )
        }
        Spacer(modifier = GlanceModifier.defaultWeight())

        if (isPast) {
            LoopDoneOrSkip(
                modifier = GlanceModifier.padding(top = 8.dp),
                loopId = loop.id
            )
        }
    }
}


private fun pickOneLoop(loops: List<LoopBase>): LoopBase {
    val now = LocalTime.now().toMs()
    val endedLoop = loops.minBy { it.loopEnd }
    return if (endedLoop.loopEnd <= now) endedLoop else loops.minBy { it.loopStart }
}