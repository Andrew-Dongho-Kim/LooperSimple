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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposePathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.Day
import com.pnd.android.loop.data.Day.Companion.isOn
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.NO_REPEAT
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.isMock
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.DAY_STRING_MAP
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.isPast
import com.pnd.android.loop.util.rememberDayColor
import com.pnd.android.loop.util.textFormatter

@Composable
fun LoopCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    showActiveDays: Boolean,
) {
    val isMock = loop.isMock()
    val animateAlpha = animateCardAlphaWithMock(loopBase = loop)

    val primary = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.medium)
    val surface = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
    val border = remember(isMock, loop.color) {
        BorderStroke(
            width = if (isMock) 1.dp else 0.5.dp,
            color = if (isMock) primary else surface
        )
    }
    Box(modifier = modifier.graphicsLayer { this.alpha = animateAlpha }) {
//        LoopCardTag(
//            modifier = Modifier
//                .padding(start = 30.dp, top = 6.dp)
//                .size(14.dp)
//        )

        val cardShape = remember { LoopCardShape(12.dp) }
        Card(
            modifier = Modifier
                .padding(
                    horizontal = 24.dp,
                    vertical = 8.dp
                )
                .fillMaxWidth()
                .clip(cardShape)
                .clickable {
                    loopViewModel.addOrUpdateLoop(loop.asLoopVo(enabled = !loop.enabled))
                },
            shape = cardShape,
            border = border
        ) {
            LoopCardContent(
                modifier = Modifier
                    .height(54.dp)
                    .alpha(
                        if (loop.enabled) ContentAlpha.high else ContentAlpha.disabled
                    ),
                loopViewModel = loopViewModel,
                loop = loop,
                showActiveDays = showActiveDays || isMock,
            )
        }
    }
}

@Composable
private fun LoopCardTag(
    modifier: Modifier = Modifier
) {
    Image(
        modifier = modifier,
        imageVector = Icons.Filled.Refresh,
        colorFilter = ColorFilter.tint(
            color = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.disabled
            )
        ),
        contentDescription = ""
    )
}

@Composable
private fun LoopCardContent(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    showActiveDays: Boolean,
) {

    BoxWithConstraints(modifier = modifier) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
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
                loopViewModel = loopViewModel,
                loop = loop,
                showActiveDays = showActiveDays,
            )
        }

        LoopCardActiveEffect(
            modifier = Modifier
                .width(maxWidth)
                .height(maxHeight),
            loopViewModel = loopViewModel,
            loop = loop,
        )
    }
}


@Composable
private fun LoopCardActiveEffect(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
) {
    if (loop.isMock()) return

    var isActive by remember { mutableStateOf(false) }
    LaunchedEffect(loop, loopViewModel) {
        loopViewModel.localDateTime.collect { currTime ->
            isActive = loop.isActive(currTime)
        }
    }
    if (!isActive) return

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
fun LoopCardColor(
    modifier: Modifier = Modifier,
    color: Int
) {
    Box(
        modifier = modifier
            .alpha(0.7f)
            .background(
                color = Color(color),
                shape = CircleShape
            )
            .background(
                color = MaterialTheme.colors.surface.copy(alpha = 0.25f),
                shape = CircleShape
            )
    )
}

@Composable
fun LoopCardBody(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    showActiveDays: Boolean,
) {
    var isPast by remember { mutableStateOf(false) }
    LaunchedEffect(loop, loopViewModel) {
        loopViewModel.localDateTime.collect { currTime ->
            isPast = loop.isActiveDay(currTime) && loop.isPast(currTime)
        }
    }

    Row(
        modifier = modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LoopCardTitle(title = loop.title)

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                LoopCardStartEndTime(
                    modifier = Modifier.weight(1f),
                    loopStart = loop.loopStart,
                    loopEnd = loop.loopEnd,
                )

                if (!isPast) {
                    LoopCardInterval(
                        modifier = Modifier.weight(1f),
                        interval = loop.interval,
                    )

                    if (showActiveDays) {
                        LoopCardActiveDays(
                            modifier = Modifier.weight(1f),
                            loop = loop
                        )
                    }
                }
            }
        }
        if (isPast && loop.enabled) {
            LoopDoneOrNotButtons(
                modifier = Modifier.height(36.dp),
                onDone = { done ->
                    loopViewModel.doneLoop(
                        loop = loop,
                        doneState = if (done) DoneState.DONE else DoneState.SKIP
                    )
                }
            )
        }
    }
}

@Composable
private fun LoopDoneOrNotButtons(
    modifier: Modifier = Modifier,
    onDone: (done: Boolean) -> Unit
) {
    Row(modifier = modifier) {
        Image(
            modifier = Modifier
                .clickable { onDone(true) }
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(8.dp),
            imageVector = Icons.Filled.Done,
            colorFilter = ColorFilter.tint(
                MaterialTheme.colors.primary.copy(
                    alpha = ContentAlpha.medium
                )
            ),
            contentDescription = ""
        )

        Image(
            modifier = Modifier
                .padding(start = 4.dp)
                .clickable { onDone(false) }
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(8.dp),
            imageVector = Icons.Filled.Close,
            colorFilter = ColorFilter.tint(
                MaterialTheme.colors.onSurface.copy(
                    alpha = ContentAlpha.medium
                )
            ),
            contentDescription = ""
        )
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
        text = if (interval == NO_REPEAT) {
            AnnotatedString("")
        } else {
            textFormatter(
                intervalString(
                    interval,
                    highlight = "#",
                )
            )
        },
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
        text = "${loopStart.formatHourMinute(false)} ~ ${loopEnd.formatHourMinute(false)}",
        style = MaterialTheme.typography.caption.copy(color = colorBody2Text()),
    )
}

@Composable
fun LoopCardActiveDays(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    if (loop.loopActiveDays in arrayOf(
            Day.EVERYDAY,
            Day.WEEKDAYS,
            Day.WEEKENDS
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
    loop: LoopBase
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
    loop: LoopBase
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val day = Day.fromIndex(index)
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


@Composable
private fun animateCardAlphaWithMock(loopBase: LoopBase): Float {
    if (!loopBase.isMock()) {
        return 1f
    }
    val transition = rememberInfiniteTransition("CreateLoopTransitions")
    val alpha by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = 1_500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaAnimForCreateLoop",
    )

    return alpha
}