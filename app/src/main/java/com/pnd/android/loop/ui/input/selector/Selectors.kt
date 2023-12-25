package com.pnd.android.loop.ui.input.selector

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.input.common.getSelectorExpandedColor

enum class InputSelector {
    NONE,
    COLOR,
    ALARM_INTERVAL,
    START_END_TIME,
    ALARMS,
}

@Composable
fun Selectors(
    focusRequester: FocusRequester,
    currentSelector: InputSelector,
    loop: LoopVo,
    onLoopUpdated: (LoopVo) -> Unit,
) {
    SideEffect {
        if (currentSelector != InputSelector.NONE) {
            focusRequester.requestFocus()
        }
    }
    val selectorExpandedColor = getSelectorExpandedColor()

    val modifier = Modifier
        .fillMaxWidth()
        .height(dimensionResource(id = R.dimen.user_input_selector_content_height))
        .focusRequester(focusRequester)
        .focusTarget()

    Surface(color = selectorExpandedColor, elevation = 3.dp) {
        Selector(
            modifier = modifier,
            currentSelector = currentSelector,
            loop = loop,
            onLoopUpdated = onLoopUpdated
        )
    }
}

@Composable
private fun Selector(
    modifier: Modifier = Modifier,
    currentSelector: InputSelector,
    loop: LoopVo,
    onLoopUpdated: (LoopVo) -> Unit,
) {
    when (currentSelector) {
        InputSelector.NONE -> {
            // do nothing
        }

        InputSelector.COLOR -> ColorSelector(
            modifier = modifier,
            selectedColor = loop.color,
            onColorSelected = { onLoopUpdated(loop.copy(color = it)) },
        )

        InputSelector.ALARM_INTERVAL -> IntervalSelector(
            modifier = modifier,
            selectedInterval = loop.interval,
            onIntervalSelected = { onLoopUpdated(loop.copy(interval = it)) }
        )

        InputSelector.START_END_TIME -> StartEndTimeSelector(
            modifier = modifier,
            selectedStartTime = loop.loopStart,
            onStartTimeSelected = { onLoopUpdated(loop.copy(loopStart = it)) },
            selectedEndTime = loop.loopEnd,
            onEndTimeSelected = { onLoopUpdated(loop.copy(loopEnd = it)) },
            selectedDays = loop.loopActiveDays,
            onSelectedDayChanged = { onLoopUpdated(loop.copy(loopActiveDays = it)) }
        )

        InputSelector.ALARMS -> AlarmSelector(
            modifier = modifier,
            selectedAlarm = loop.alarms,
            onAlarmSelected = { onLoopUpdated(loop.copy(alarms = it)) }
        )
    }
}
