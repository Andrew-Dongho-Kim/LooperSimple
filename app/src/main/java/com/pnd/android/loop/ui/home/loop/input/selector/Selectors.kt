package com.pnd.android.loop.ui.home.loop.input.selector

import androidx.compose.animation.core.animateDpAsState
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
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.loop.input.InputSelector
import com.pnd.android.loop.ui.home.loop.input.UserInputState
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.util.rememberImeOpenState
import kotlinx.coroutines.launch


@Composable
fun Selectors(
    inputState: UserInputState,
    blurState: BlurState,
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
    val selectorHeight by animateDpAsState(
        targetValue = if (currSelector == InputSelector.NONE || keyboardShown) {
            0.dp
        } else {
            dimensionResource(id = R.dimen.user_input_selector_content_height)
        },
        label = ""
    )

    val modifier = Modifier
        .fillMaxWidth()
        .height(selectorHeight)
        .focusRequester(focusRequester)
        .focusTarget()

    Surface(
        modifier = modifier,
        color = compositeOverSurface(),
        shadowElevation = 3.dp
    ) {
        Selector(
            blurState = blurState,
            inputState = inputState,
            snackBarHostState = snackBarHostState,
        )
    }
}

@Composable
private fun Selector(
    modifier: Modifier = Modifier,
    blurState: BlurState,
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
            onIntervalSelected = { inputState.update(interval = it) }
        )

        InputSelector.START_END_TIME -> StartEndTimeSelector(
            modifier = modifier,
            blurState = blurState,
            selectedStartTime = loop.loopStart,
            onStartTimeSelected = onStartTimeSelected@{ loopStart ->
                if (loopStart >= loop.loopEnd) {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.warning_choose_at_least_one_day_of_the_week)
                        )
                    }
                    return@onStartTimeSelected
                }
                inputState.update(loopStart = loopStart)
            },
            selectedEndTime = loop.loopEnd,
            onEndTimeSelected = onEndTimeSelected@{ loopEnd ->
                if (loop.loopStart >= loopEnd) {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.warning_choose_at_least_one_day_of_the_week)
                        )
                    }
                    return@onEndTimeSelected
                }
                inputState.update(loopEnd = loopEnd)
            },
            selectedDays = loop.loopActiveDays,
            onSelectedDayChanged = onDayChanged@{ activeDays ->
                if (activeDays == 0) {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.warning_choose_at_least_one_day_of_the_week)
                        )
                    }
                    return@onDayChanged
                }
                inputState.update(loopActiveDays = activeDays)
            }
        )
    }
}
