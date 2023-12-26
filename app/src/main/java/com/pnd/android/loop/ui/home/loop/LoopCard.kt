package com.pnd.android.loop.ui.home.loop

import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.graphics.PathMeasure
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toComposePathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopVo.Day.Companion.isOn
import com.pnd.android.loop.ui.home.LoopViewModel
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.DAY_STRING_MAP
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.rememberDayColor
import com.pnd.android.loop.util.textFormatter
import com.pnd.android.loop.util.toHourMinute
import java.time.LocalDateTime

@Composable
fun LoopCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loop: LoopVo,
) {
    val cardShape = remember { LoopCardShape(12.dp) }
    Card(
        modifier = modifier
            .padding(
                horizontal = 24.dp,
                vertical = 8.dp
            )
            .fillMaxWidth()
            .clip(cardShape)
            .clickable {
                loopViewModel.addLoop(loop.copy(enabled = !loop.enabled))
            },
        shape = cardShape,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )
    ) {
        BoxWithConstraints(modifier = Modifier.wrapContentHeight()) {
            LoopCardContent(
                modifier = Modifier
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                    .alpha(
                        if (loop.enabled) ContentAlpha.high else ContentAlpha.disabled
                    ),
                loop = loop
            )
            LoopCardActiveEffect(
                modifier = Modifier
                    .width(maxWidth)
                    .height(50.dp),
                loopViewModel = loopViewModel,
                loop = loop,
            )
        }
    }
}

@Composable
private fun LoopCardActiveEffect(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loop: LoopVo,
) {
    val localDateTime by loopViewModel.localDateTime.collectAsState(initial = LocalDateTime.now())
    if (!loop.isActive(localDateTime = localDateTime)) return

    val paint = remember { Paint() }
    val pathMeasure = remember { PathMeasure() }
    val cardShape = remember { LoopCardShape(12.dp) }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val phase by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = 30_000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )
    val edgeColor = Color(loop.color).copy(alpha = 0.4f)

    Canvas(modifier = modifier) {
        val path = cardShape.createOutlinePath(
            density = Density(density, fontScale),
            size = size
        )
        pathMeasure.setPath(path.asAndroidPath(), false)

        val pathEffect = PathDashPathEffect(
            Path().apply {
                addCircle(0f, 0f, 3.dp.toPx(), Path.Direction.CW)
            },
            pathMeasure.length / 5f,
            phase * pathMeasure.length,
            PathDashPathEffect.Style.MORPH
        ).toComposePathEffect()

        paint.color = edgeColor
        paint.pathEffect = pathEffect

        with(drawContext.canvas) { drawPath(path, paint) }
    }
}

@Composable
private fun LoopCardContent(
    modifier: Modifier = Modifier,
    loop: LoopVo,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoopCardColor(
            modifier = Modifier.size(10.dp),
            color = loop.color
        )

        LoopCardBody(
            modifier = Modifier
                .padding(top = 4.dp)
                .weight(1f),
            loop = loop,
        )
    }
}


@Composable
fun LoopCardColor(
    modifier: Modifier,
    color: Int
) {
    Box(
        modifier = modifier
            .background(
                color = Color(color),
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                shape = CircleShape
            )
    )
}

@Composable
fun LoopCardBody(
    modifier: Modifier = Modifier,
    loop: LoopVo,
) {
    Column(modifier = modifier.padding(start = 16.dp)) {
        LoopCardTitle(
            title = loop.title,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {

            LoopCardStartEndTime(
                modifier = Modifier.weight(1f),
                loopStart = loop.loopStart,
                loopEnd = loop.loopEnd,
            )

            LoopCardInterval(
                modifier = Modifier.weight(1f),
                interval = loop.interval,
            )

            LoopCardActiveDays(
                modifier = Modifier.weight(1f),
                loop = loop
            )
        }
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
        style = MaterialTheme.typography.body1.copy(
            color = colorBody1Text(),
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
        text = textFormatter(
            intervalString(
                interval,
                highlight = "#",
            )
        ),
        style = MaterialTheme.typography.caption.copy(color = colorBody2Text()),
        modifier = modifier,
    )
}

@Composable
private fun LoopCardStartEndTime(
    modifier: Modifier,
    loopStart: Long,
    loopEnd: Long,
) {
    Text(
        modifier = modifier,
        text = "${loopStart.toHourMinute(false)} ~ ${loopEnd.toHourMinute(false)}",
        style = MaterialTheme.typography.caption.copy(color = colorBody2Text()),
    )
}

@Composable
fun LoopCardActiveDays(
    modifier: Modifier = Modifier,
    loop: LoopVo,
) {
    if (loop.loopActiveDays in arrayOf(
            LoopVo.Day.EVERYDAY,
            LoopVo.Day.WEEKDAYS,
            LoopVo.Day.WEEKENDS
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
    loop: LoopVo
) {
    Text(
        modifier = modifier.padding(end = 2.dp),
        text = stringResource(DAY_STRING_MAP[loop.loopActiveDays]!!),
        style = MaterialTheme.typography.caption.copy(
            color = MaterialTheme.colors.primary
        )
    )
}

@Composable
private fun LoopCardActiveDaysCommon(
    modifier: Modifier,
    loop: LoopVo
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val day = LoopVo.Day.fromIndex(index)
            val selected = loop.loopActiveDays.isOn(day)

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
        val color = MaterialTheme.colors.primary
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
            style = MaterialTheme.typography.caption.copy(
                color = rememberDayColor(day = day).copy(
                    alpha = if (selected) ContentAlpha.high else ContentAlpha.disabled,
                ),
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
private fun colorBody1Text(): Color {
    return MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.high)
}

@Composable
private fun colorBody2Text(): Color {
    return MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
}