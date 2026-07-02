package com.pnd.android.loop.appwidget.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isPast
import com.pnd.android.loop.util.toMs
import java.time.LocalTime


@Composable
fun LoopWidgetSmall(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<LoopBase>,
    todayTotal: Int,
) {
    val container = modifier
        .fillMaxSize()
        .background(widgetSurface())
        .cornerRadius(WIDGET_CARD_RADIUS)

    if (loops.isEmpty()) {
        LoopWidgetEmpty(
            modifier = container,
            loopsTotal = todayTotal,
        )
    } else {
        val loop = pickOneLoop(loops)
        key(loop.loopId) {
            LoopWidgetItem(
                modifier = container,
                loop = loop
            )
        }
    }
}


@Composable
private fun LoopWidgetItem(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    val context = LocalContext.current
    val isPast = loop.isPast()
    val isActive = loop.isActive()

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(NavigatePage.Home.deepLink(highlightId = loop.loopId))
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
            }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            LoopColor(
                color = loop.color.compositeOverOnSurface(),
                active = isActive,
            )
            if (!isPast) {
                LoopStartEndTime(
                    modifier = GlanceModifier.padding(start = 8.dp),
                    loop = loop,
                    emphasize = isActive,
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))
        LoopTitle(
            title = loop.title,
            isActive = isActive,
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        if (isPast) {
            LoopDoneOrSkip(
                modifier = GlanceModifier.padding(top = 8.dp),
                loopId = loop.loopId
            )
        }
    }
}


private fun pickOneLoop(loops: List<LoopBase>): LoopBase {
    val now = LocalTime.now().toMs()
    val endedLoop = loops.minBy { it.endInDay }
    return if (endedLoop.endInDay <= now) endedLoop else loops.minBy { it.startInDay }
}
