package com.pnd.android.loop.ui.home.loop.input.selector

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.loop.input.UserInputState
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.util.rememberImeOpenState

enum class InputSelector {
    COLOR,
    ALARM_INTERVAL,
    START_END_TIME,
    NONE,
}

@Composable
fun Selectors(
    inputState: UserInputState,
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

    Surface(color = compositeOverSurface(), elevation = 3.dp) {
        Selector(
            modifier = modifier,
            inputState = inputState,
        )
    }
}

@Composable
private fun Selector(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
) {
    val loop = inputState.value
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
            selectedStartTime = loop.loopStart,
            onStartTimeSelected = { inputState.update(loopStart = it) },
            selectedEndTime = loop.loopEnd,
            onEndTimeSelected = { inputState.update(loopEnd = it) },
            selectedDays = loop.loopActiveDays,
            onSelectedDayChanged = { inputState.update(loopActiveDays = it) }
        )
    }
}
