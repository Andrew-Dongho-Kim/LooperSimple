package com.pnd.android.loop.ui.home.input.selector

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.ui.home.input.InputSelector
import com.pnd.android.loop.ui.home.input.UserInputState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.rememberImeOpenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun Selectors(
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
    focusRequester: FocusRequester,
) {
    val currSelector = inputState.currSelector
    SideEffect {
        if (currSelector != InputSelector.NONE) {
            focusRequester.requestFocus()
        }
    }

    val keyboardShown by rememberImeOpenState()
    val expandedHeight = dimensionResource(
        id = if (currSelector == InputSelector.START_END_TIME) {
            R.dimen.user_input_time_selector_content_height
        } else {
            R.dimen.user_input_selector_content_height
        }
    )
    val selectorHeight by animateDpAsState(
        targetValue = if (currSelector == InputSelector.NONE || keyboardShown) {
            0.dp
        } else {
            expandedHeight
        },
        animationSpec = tween(500),
        label = "selectorHeightAnimation"
    )

    val modifier = Modifier
        .fillMaxWidth()
        .height(selectorHeight)
        .focusRequester(focusRequester)
        .focusTarget()

    Surface(
        modifier = modifier,
        color = AppColor.surfaceElevated,
    ) {
        Selector(
            inputState = inputState,
            snackBarHostState = snackBarHostState,
        )
    }
}

@Composable
private fun Selector(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
) {
    val loop = inputState.value

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    when (inputState.currSelector) {
        InputSelector.NONE -> Box(modifier = modifier)

        InputSelector.COLOR -> ColorSelector(
            modifier = modifier,
            selectedColor = loop.color,
            onColorSelected = { inputState.update(color = it) },
        )

        InputSelector.ALARM_INTERVAL -> IntervalSelector(
            modifier = modifier,
            selectedInterval = loop.interval,
            // An any-time loop has no fixed start/end window, so any interval is
            // valid; otherwise the interval must be shorter than the duration.
            maxInterval = if (loop.isAnyTime) Long.MAX_VALUE else loop.endInDay - loop.startInDay,
            onIntervalSelected = onIntervalChanged@{ interval ->
                if (!loop.isAnyTime && loop.endInDay - loop.startInDay <= interval) {
                    coroutineScope.showWarning(
                        snackBarHostState,
                        context.getString(R.string.warning_interval_must_be_shorter_than_duration)
                    )
                    return@onIntervalChanged
                }

                inputState.update(interval = interval)
            }
        )

        InputSelector.START_END_TIME -> StartEndTimeSelector(
            modifier = modifier,
            isAnyTimeChecked = loop.isAnyTime,
            onIsAnyTimeCheckChanged = onIsAnyTimeCheckChanged@{ isAnyTime ->
                inputState.update(isAnyTime = isAnyTime)
            },
            selectedStartTime = loop.startInDay,
            onStartTimeSelected = { loopStart ->
                if (!loop.isAnyTime && isLoopDurationTooShort(loopStart, loop.endInDay)) {
                    coroutineScope.showWarning(
                        snackBarHostState,
                        context.getString(R.string.warning_end_time_should_be_after_start_time)
                    )
                }
                inputState.update(loopStart = if (loop.isAnyTime) ANY_TIME else loopStart)
            },
            selectedEndTime = loop.endInDay,
            onEndTimeSelected = { loopEnd ->
                if (!loop.isAnyTime && isLoopDurationTooShort(loop.startInDay, loopEnd)) {
                    coroutineScope.showWarning(
                        snackBarHostState,
                        context.getString(R.string.warning_end_time_should_be_after_start_time)
                    )
                }
                inputState.update(loopEnd = if (loop.isAnyTime) ANY_TIME else loopEnd)
            },
            selectedDays = loop.activeDays,
            onSelectedDayChanged = onDayChanged@{ activeDays ->
                if (activeDays == 0) {
                    coroutineScope.showWarning(
                        snackBarHostState,
                        context.getString(R.string.warning_choose_at_least_one_day_of_the_week)
                    )
                    return@onDayChanged
                }
                inputState.update(loopActiveDays = activeDays)
            }
        )
    }
}

/**
 * Shows [message] in the snackbar, replacing any message already on screen so rapid, repeated
 * warnings (e.g. nudging a stepper past its limit) don't pile up in the queue.
 */
private fun CoroutineScope.showWarning(
    snackBarHostState: SnackbarHostState,
    message: String,
) = launch {
    snackBarHostState.currentSnackbarData?.dismiss()
    snackBarHostState.showSnackbar(message)
}
