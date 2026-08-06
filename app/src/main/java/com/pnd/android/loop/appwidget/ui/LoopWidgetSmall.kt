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
import com.pnd.android.loop.appwidget.WidgetLoop
import com.pnd.android.loop.appwidget.awaitsResponse
import com.pnd.android.loop.appwidget.isRunning
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.ui.theme.compositeOverOnSurface


@Composable
fun LoopWidgetSmall(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<WidgetLoop>,
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
        val widgetLoop = pickOneLoop(loops)
        key(widgetLoop.itemId()) {
            LoopWidgetItem(
                modifier = container,
                widgetLoop = widgetLoop,
            )
        }
    }
}


@Composable
private fun LoopWidgetItem(
    modifier: GlanceModifier = GlanceModifier,
    widgetLoop: WidgetLoop,
) {
    val context = LocalContext.current
    val loop = widgetLoop.loop
    // 판정은 Medium 위젯과 같은 규칙을 쓴다([awaitsResponse]·[isRunning]). 특히 자정을 넘기는
    // 루프는 util 의 isPast/isActive 로는 낮 시간대의 "오늘 밤 예정"을 "지난 것"으로 잘못 본다.
    val awaitsResponse = widgetLoop.awaitsResponse()
    val isRunning = widgetLoop.isRunning()

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
                active = isRunning,
            )
            if (!awaitsResponse) {
                LoopStartEndTime(
                    modifier = GlanceModifier.padding(start = 8.dp),
                    loop = loop,
                    emphasize = isRunning,
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))
        LoopTitle(
            title = loop.title,
            isActive = isRunning,
        )

        Spacer(modifier = GlanceModifier.defaultWeight())

        if (awaitsResponse) {
            LoopDoneOrSkip(
                modifier = GlanceModifier.padding(top = 8.dp),
                loopId = loop.loopId,
                // 응답은 이 몫이 시작한 날짜 행에 기록해야 한다(어젯밤 몫이면 어제).
                dateMs = widgetLoop.dateMs,
            )
        }
    }
}


/**
 * 한 칸짜리 위젯에 띄울 몫 하나.
 *
 * 지금 진행 중인 몫이 최우선이다("지금 뭐 할 차례"가 한 칸 위젯의 존재 이유다). 그다음이 답을
 * 기다리는 몫(어젯밤에서 넘어온 것 포함)이고, 그마저 없으면 가장 먼저 시작할 몫을 세운다.
 */
private fun pickOneLoop(loops: List<WidgetLoop>): WidgetLoop {
    loops.firstOrNull { it.isRunning() }?.let { return it }
    loops.firstOrNull { it.awaitsResponse() }?.let { return it }
    return loops.minByOrNull { it.loop.startInDay } ?: loops.first()
}
