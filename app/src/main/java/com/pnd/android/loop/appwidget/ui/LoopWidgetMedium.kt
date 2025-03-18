package com.pnd.android.loop.appwidget.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
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

private val WIDGET_MEDIUM_PADDING_HORIZONTAL = 18.dp

@Composable
fun LoopWidgetMedium(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<LoopBase>,
    todayTotal: Int,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(
                horizontal = 4.dp,
                vertical = 4.dp,
            ),
    ) {
        LocalDateHeader(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .padding(horizontal = WIDGET_MEDIUM_PADDING_HORIZONTAL),
        )

        if (loops.isEmpty()) {
            LoopWidgetEmpty(
                modifier = modifier,
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
            LoopWidgetItem(
                modifier = GlanceModifier.padding(horizontal = 8.dp),
                loop = loop
            )
        }
        item(
            itemId = -1L
        ) {
            Spacer(GlanceModifier.fillMaxWidth().height(24.dp))
        }
    }
}

@Composable
private fun LoopWidgetItem(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    val isPast = loop.isPast()
    val isAnyTime = loop.isAnyTime
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
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
            .padding(
                top = 12.dp,
                start = 4.dp,
                end = 4.dp,
                bottom = if (!isPast) 16.dp else 0.dp,
            ),
    ) {

        Row(
            modifier = GlanceModifier
                .padding(top = if (isPast) 2.dp else 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            LoopColor(
                color = loop.color.compositeOverOnSurface()
            )
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(start = WIDGET_MEDIUM_PADDING_HORIZONTAL)
            ) {
                LoopTitle(
                    title = loop.title
                )

                if (!isPast) {
                    LoopStartEndTime(
                        loop = loop
                    )
                }
            }

            if (isAnyTime && (loop.startInDay < 0 || loop.endInDay < 0)) {
                AnyTimeLoopStartOrStop(
                    modifier = GlanceModifier.padding(
                        horizontal = WIDGET_MEDIUM_PADDING_HORIZONTAL,
                    ),
                    loop = loop
                )
            }
        }

        if (!isAnyTime && isPast) {
            LoopDoneOrSkipMedium(
                modifier = GlanceModifier.padding(
                    start = WIDGET_MEDIUM_PADDING_HORIZONTAL,
                    top = 12.dp
                ),
                loopId = loop.loopId
            )
        }


    }
}