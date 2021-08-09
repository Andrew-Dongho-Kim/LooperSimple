package com.pnd.android.loop.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.whenStarted
import com.pnd.android.loop.R
import com.pnd.android.loop.alarm.AlarmHelper
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.Loop
import com.pnd.android.loop.data.Loop.Day.Companion.EVERYDAY
import com.pnd.android.loop.data.Loop.Day.Companion.WEEKDAYS
import com.pnd.android.loop.data.Loop.Day.Companion.WEEKENDS
import com.pnd.android.loop.data.Loop.Day.Companion.isOn
import com.pnd.android.loop.ui.animation.CircleProgress
import com.pnd.android.loop.ui.icons.icon
import com.pnd.android.loop.ui.theme.PADDING_HZ_ITEM
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.itemPadding
import com.pnd.android.loop.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val logger = log("LoopUi")

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun Loops(
    alarmHelper: AlarmHelper,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel()

    val vmState = viewModel.loops.observeAsState()

    Box(modifier = modifier.background(MaterialTheme.colors.onSurface.copy(alpha = 0.02f))) {
        val loops = vmState.value
        if (loops.isNullOrEmpty()) {
            EmptyLoops(
                modifier = Modifier.fillMaxSize()
            )
            logger.d { "Empty loops" }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState, reverseScrolling = true)
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                loops.forEach { loop ->
                    key(loop.id) {
                        LoopCard(alarmHelper = alarmHelper, loop = loop)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLoops(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = stringResource(R.string.desc_no_loops),
                style = MaterialTheme.typography.h6
            )
        }
    }

}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun LoopCard(
    alarmHelper: AlarmHelper,
    loop: Loop,
    modifier: Modifier = Modifier
) {

    val pxToolSize = with(LocalDensity.current) { 120.dp.toPx() }

    Box(
        modifier = modifier
    ) {
        val swipeState = rememberSwipeableState(0)

        LoopCardTool(
            alarmHelper = alarmHelper,
            loop = loop,
            swipeState = swipeState,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        LoopCardMain(
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
fun LoopCardMain(
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

    if (loop.loopEnableDays in arrayOf(EVERYDAY, WEEKDAYS, WEEKENDS)) {
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

@ExperimentalMaterialApi
@Composable
fun LoopCardTool(
    alarmHelper: AlarmHelper,
    loop: Loop,
    swipeState: SwipeableState<Int>,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel<HomeViewModel>()
    val showDeleteDialog = remember { mutableStateOf(false) }

    if (showDeleteDialog.value) {
        DeleteDialog(
            viewModel = viewModel,
            alarmHelper = alarmHelper,
            loop = loop,
            showDeleteDialog = showDeleteDialog
        )
    }

    Row(
        modifier = modifier
            .height(36.dp)
            .padding(start = 12.dp)
            .background(MaterialTheme.colors.primary, MaterialTheme.shapes.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scope = rememberCoroutineScope()

        LoopCardToolIcon(
            imageVector = Icons.Outlined.ModeEdit,
            onClick = {
                scope.launch { swipeState.animateTo(targetValue = 0) }
            }
        )

        LoopCardToolIcon(
            imageVector = Icons.Outlined.Delete,
            onClick = {
                showDeleteDialog.value = true
            }
        )
    }
}

@Composable
fun DeleteDialog(
    viewModel: HomeViewModel,
    alarmHelper: AlarmHelper,
    loop: Loop,
    showDeleteDialog: MutableState<Boolean>
) {
    AlertDialog(
        onDismissRequest = {
            showDeleteDialog.value = false
        },
        text = {
            Text(
                text = stringResource(id = R.string.delete_dialog_message),
                style = MaterialTheme.typography.body1
            )
        },
        dismissButton = {
            TextButton(onClick = {
                showDeleteDialog.value = false
            }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                alarmHelper.cancel(loop)
                viewModel.removeLoop(loop.id)
            }) {
                Text(text = stringResource(id = R.string.ok))
            }
        }
    )
}

@Composable
fun LoopCardToolIcon(
    imageVector: ImageVector,
    onClick: (() -> Unit) = {}
) = Icon(
    modifier = Modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
            indication = rememberRipple(bounded = false, color = Color.White)
        )
        .padding(
            start = PADDING_HZ_ITEM,
            end = PADDING_HZ_ITEM
        )
        .size(24.dp),
    painter = rememberVectorPainter(image = imageVector),
    tint = Color.White,
    contentDescription = null
)
