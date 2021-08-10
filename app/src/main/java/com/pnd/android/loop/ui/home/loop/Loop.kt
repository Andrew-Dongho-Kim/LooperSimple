package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.whenStarted
import com.pnd.android.loop.alarm.AlarmHelper
import com.pnd.android.loop.data.Loop
import com.pnd.android.loop.data.Loop.Day.Companion.isOn
import com.pnd.android.loop.ui.animation.CircleProgress
import com.pnd.android.loop.ui.home.HomeViewModel
import com.pnd.android.loop.ui.icons.icon
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.itemPadding
import com.pnd.android.loop.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun Loop(
    alarmHelper: AlarmHelper,
    loop: Loop,
    modifier: Modifier = Modifier
) {

    val pxToolSize = with(LocalDensity.current) { 120.dp.toPx() }

    Box(
        modifier = modifier
    ) {
        val swipeState = rememberSwipeableState(0)

        LoopTools(
            alarmHelper = alarmHelper,
            loop = loop,
            swipeState = swipeState,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        LoopMain(
            alarmHelper = alarmHelper,
            loop = loop,
            swipeState = swipeState,
            pxToolSize = pxToolSize
        )

    }
}


@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun LoopMain(
    alarmHelper: AlarmHelper,
    loop: Loop,
    swipeState: SwipeableState<Int>,
    pxToolSize: Float
) {

    val scope = rememberCoroutineScope()

    val viewModel = viewModel<HomeViewModel>()

    var isAlarmOn by remember { mutableStateOf(loop.enabled) }

    Card(
        modifier = Modifier
            .offset { IntOffset(swipeState.offset.value.roundToInt(), 0) }
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundShapes.large)
            .swipeable(
                state = swipeState,
                anchors = mapOf(0f to 0, pxToolSize to 1),
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
            .clickable {
                if (abs(swipeState.offset.value - pxToolSize) >= pxToolSize / 2) {
                    isAlarmOn = !isAlarmOn
                    setAlarmOn(
                        viewModel = viewModel,
                        alarmHelper = alarmHelper,
                        loop = loop,
                        isAlarmOn = isAlarmOn
                    )
                } else {
                    scope.launch { swipeState.animateTo(0) }
                }
            },
        border = BorderStroke(0.5.dp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.large,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.itemPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            LoopImage(loop = loop)

            LoopTexts(
                loop = loop, modifier = Modifier
                    .padding(top = 4.dp)
                    .weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Switch(checked = isAlarmOn, onCheckedChange = { checked ->
                isAlarmOn = checked
                setAlarmOn(
                    viewModel = viewModel,
                    alarmHelper = alarmHelper,
                    loop = loop,
                    isAlarmOn = isAlarmOn
                )
            })
        }
    }
}




@Composable
fun LoopImage(loop: Loop) {
    Box(
        modifier = Modifier.size(30.dp),
        contentAlignment = Alignment.Center
    ) {
        val progress by progress(loop = loop)
        val contentAlpha = if (loop.enabled) ContentAlpha.medium else ContentAlpha.disabled

        LoopProgress(loop = loop, progress = progress)
        Icon(
            modifier = Modifier.size(20.dp),
            painter = rememberVectorPainter(image = icon(loop.icon)),
            tint = MaterialTheme.colors.onSurface.copy(alpha = contentAlpha),
            contentDescription = null
        )
    }
}


@Composable
fun LoopProgress(loop: Loop, progress: Float) {
    if (!loop.enabled) return

    if (loop.isAllowedDay() && loop.isAllowedTime()) {
        CircleProgress(
            modifier = Modifier.size(30.dp),
            progress = progress,
            strokeWidth = 2.dp
        )
    }
}


@Composable
fun progress(loop: Loop): MutableState<Float> {
    val progressState = remember { mutableStateOf(1f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(true) {
        lifecycleOwner.whenStarted {
            while (true) {
                val interval = loop.interval
                val curr = localTime() % MS_1DAY
                val diff = (curr - loop.loopStart) % interval
                val remain = interval - diff

                progressState.value = remain.toFloat() / interval
                delay(MS_1SEC)
            }
        }
    }
    return progressState
}

@Composable
fun LoopTexts(
    loop: Loop,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .padding(start = 16.dp)
    ) {
        val typography = MaterialTheme.typography

        CompositionLocalProvider(
            LocalContentAlpha provides if (loop.enabled) ContentAlpha.medium else ContentAlpha.disabled
        ) {
            Text(
                text = loop.title,
                style = typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CompositionLocalProvider(
            LocalContentAlpha provides if (loop.enabled) ContentAlpha.medium else ContentAlpha.disabled
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "${h2m2(loop.loopStart)} ~ ${h2m2(loop.loopEnd)}",
                    style = typography.caption,
                    modifier = Modifier.width(110.dp)
                )

                Text(
                    text = textFormatter(
                        intervalString(
                            loop.interval,
                            highlight = "#",
                            isAbb = true
                        )
                    ),
                    style = typography.caption,
                    modifier = Modifier.width(70.dp)
                )

                LoopDaysEnabled(loop = loop)
            }
        }
    }
}

private fun setAlarmOn(
    viewModel: HomeViewModel,
    alarmHelper: AlarmHelper,
    loop: Loop,
    isAlarmOn: Boolean
) {
    if (isAlarmOn) {
        alarmHelper.reserveRepeat(loop)
    } else {
        alarmHelper.cancel(loop)
    }
    viewModel.notifyLoops()
}

@Composable
fun LoopDaysEnabled(
    loop: Loop,
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (loop.enabled) ContentAlpha.medium else ContentAlpha.disabled

    if (loop.loopEnableDays in arrayOf(Loop.Day.EVERYDAY, Loop.Day.WEEKDAYS, Loop.Day.WEEKENDS)) {
        Text(
            modifier = modifier.padding(end = 2.dp),
            text = stringResource(DAY_STRING_MAP[loop.loopEnableDays]!!),
            style = MaterialTheme.typography.caption.copy(
                color = MaterialTheme.colors.primary.copy(
                    alpha = contentAlpha
                )
            )
        )
    } else {
        Row(
            modifier = modifier
        ) {
            ABB_DAYS.forEachIndexed { index, dayResId ->
                val day = Loop.Day.fromIndex(index)
                val selected = loop.loopEnableDays.isOn(day)
                val color =
                    (if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface).copy(
                        alpha = contentAlpha
                    )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier) {
                        if (selected) drawCircle(color = color, radius = 1.5.dp.toPx())
                    }
                    Text(
                        modifier = Modifier.padding(end = 2.dp),
                        text = stringResource(dayResId),
                        style = MaterialTheme.typography.caption.copy(color = color)
                    )
                }

            }
        }
    }
}

