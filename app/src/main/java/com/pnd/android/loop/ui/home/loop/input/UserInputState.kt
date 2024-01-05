package com.pnd.android.loop.ui.home.loop.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoop
import com.pnd.android.loop.data.putTo

@Stable
class UserInputState(
    prevSelector: InputSelector = InputSelector.NONE,
    currSelector: InputSelector = InputSelector.NONE,
    mode: Mode = Mode.None,
    value: LoopBase = LoopBase.default(isMock = true)
) {
    enum class Mode { None, New, Edit }

    var prevSelector by mutableStateOf(prevSelector)
        private set

    var currSelector by mutableStateOf(currSelector)
        private set

    var mode by mutableStateOf(mode)
        private set

    var value by mutableStateOf(value)
        private set

    var textFieldValue by mutableStateOf(TextFieldValue(text = value.title))
        private set

    val isTitleEmpty
        get() = textFieldValue.text.isEmpty()

    fun update(
        title: TextFieldValue = this.textFieldValue,
        color: Int = value.color,
        loopStart: Long = value.loopStart,
        loopEnd: Long = value.loopEnd,
        loopActiveDays: Int = value.loopActiveDays,
        interval: Long = value.interval,
        enabled: Boolean = value.enabled,
    ) {
        value = value.copy(
            title = title.text,
            color = color,
            loopStart = loopStart,
            loopEnd = loopEnd,
            loopActiveDays = loopActiveDays,
            interval = interval,
            enabled = enabled
        )
        textFieldValue = title
        ensureState()
    }

    fun setSelector(selector: InputSelector) {
        prevSelector = currSelector
        currSelector = if (selector == currSelector) {
            InputSelector.NONE
        } else {
            selector
        }
        ensureState()
    }

    fun reset() {
        mode = Mode.None
        value = LoopBase.default(isMock = true)
        textFieldValue = TextFieldValue()
        prevSelector = currSelector
        currSelector = InputSelector.NONE
    }

    private fun ensureState() {
        if (currSelector == InputSelector.NONE) {
            if (isTitleEmpty) {
                reset()
            } else {
                mode = Mode.New
            }
        } else if (mode == Mode.None) {
            mode = Mode.New
        }
    }

    companion object {
        private const val STATE_PREV_SELECTOR = "state_prev_selector"
        private const val STATE_CURR_SELECTOR = "state_curr_selector"
        private const val STATE_MODE = "state_mode"

        val Saver = mapSaver(
            save = { state ->
                mutableMapOf<String, Any?>(
                    STATE_PREV_SELECTOR to state.prevSelector.name,
                    STATE_CURR_SELECTOR to state.currSelector.name,
                    STATE_MODE to state.mode.name
                ).also {
                    state.value.putTo(it)
                }
            },
            restore = { map ->
                UserInputState(
                    prevSelector = InputSelector.valueOf(map[STATE_PREV_SELECTOR] as String),
                    currSelector = InputSelector.valueOf(map[STATE_CURR_SELECTOR] as String),
                    mode = Mode.valueOf(map[STATE_MODE] as String),
                    value = map.asLoop(isMock = true)
                )
            }
        )
    }
}

@Composable
fun rememberUserInputState() = rememberSaveable(saver = UserInputState.Saver) {
    UserInputState()
}

enum class InputSelector {
    COLOR,
    START_END_TIME,
    ALARM_INTERVAL,
    NONE,
}