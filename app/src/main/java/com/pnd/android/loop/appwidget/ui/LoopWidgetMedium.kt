package com.pnd.android.loop.appwidget.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isPast
import androidx.core.net.toUri

private val WIDGET_MEDIUM_PADDING_HORIZONTAL = 16.dp

@Composable
fun LoopWidgetMedium(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<LoopBase>,
    todayTotal: Int,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(widgetSurface())
            .cornerRadius(WIDGET_CARD_RADIUS)
            .padding(
                horizontal = 8.dp,
                vertical = 10.dp,
            ),
    ) {
        LocalDateHeader(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(
                    horizontal = WIDGET_MEDIUM_PADDING_HORIZONTAL,
                    vertical = 6.dp,
                ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (loops.isEmpty()) {
            LoopWidgetEmpty(
                modifier = GlanceModifier,
                loopsTotal = todayTotal
            )
        } else {
            LoopWidgetBody(loops = loops)
        }
    }
}

@Composable
private fun LoopWidgetBody(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<LoopBase>,
) {
    LazyColumn(modifier = modifier) {
        items(
            items = loops,
            itemId = { loop -> loop.loopId.toLong() }
        ) { loop ->
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(12.dp))
            LoopWidgetItem(
                modifier = GlanceModifier.padding(
                    horizontal = 6.dp,
                ),
                loop = loop
            )
        }
        item(
            itemId = -1L
        ) {
            Spacer(GlanceModifier.fillMaxWidth().height(8.dp))
        }
    }
}

@Composable
private fun LoopWidgetItem(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    val isPast = loop.isPast()
    val isActive = loop.isActive()
    val isAnyTime = loop.isAnyTime
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .cornerRadius(WIDGET_ROW_RADIUS)
            .background(widgetWell(active = isActive))
            .clickable {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        NavigatePage.Home.deepLink(highlightId = loop.loopId).toUri()
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
            }
            .padding(
                horizontal = 15.dp,
                vertical = 8.dp,
            ),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            LoopColor(
                color = loop.color.compositeOverOnSurface(),
                active = isActive,
            )
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(start = 13.dp)
            ) {
                LoopTitle(
                    title = loop.title,
                    isActive = isActive,
                )

                if (!isPast) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    LoopStartEndTime(
                        loop = loop,
                        emphasize = isActive,
                    )
                }
            }

            if (isAnyTime && (loop.startInDay < 0 || loop.endInDay < 0)) {
                AnyTimeLoopStartOrStop(
                    modifier = GlanceModifier.padding(start = 8.dp),
                    loop = loop
                )
            }
        }

        if (!isAnyTime && isPast) {
            Spacer(modifier = GlanceModifier.height(12.dp))
            LoopDoneOrSkipMedium(
                loopId = loop.loopId
            )
        }
    }
}
