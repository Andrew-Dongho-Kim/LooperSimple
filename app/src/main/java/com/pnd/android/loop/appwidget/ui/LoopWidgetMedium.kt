package com.pnd.android.loop.appwidget.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.action.mutableActionParametersOf
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
import androidx.glance.layout.size
import com.pnd.android.loop.MainActivity
import com.pnd.android.loop.appwidget.ACTION_PARAM_NAVIGATE
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isPast
import kotlin.random.Random


@Composable
fun LoopWidgetMedium(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<LoopBase>,
    todayTotal: Int,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColor.surface.copy(alpha = 0.7f))
            .padding(
                horizontal = 4.dp,
                vertical = 4.dp,
            ),
    ) {
        LocalDateHeader(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    start = 4.dp,
                    end = 4.dp,
                    bottom = 8.dp,
                ),
            loops = loops,
            todayTotal = todayTotal,
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
            itemId = { loop -> loop.id.toLong() }
        ) { loop ->
            LoopWidgetItem(loop = loop)
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
    val context = LocalContext.current

    Column(
        modifier = modifier
            .background(
                if (loop.isActive()) {
                    AppColor.primary.copy(alpha = 0.1f)
                } else {
                    Color.Transparent
                }
            )
            .fillMaxWidth()
            .clickable {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(NavigatePage.Home.deepLink(highlightId = loop.id))
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
        if (!isPast) {
            LoopStartEndTime(
                modifier = GlanceModifier.padding(start = 22.dp),
                loop = loop
            )
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
                modifier = GlanceModifier.padding(start = 8.dp),
                title = loop.title
            )
        }

        if (isPast) {
            LoopDoneOrSkipMedium(
                modifier = GlanceModifier.padding(
                    start = 8.dp,
                    top = 12.dp
                ),
                loopId = loop.id
            )
        }
    }
}