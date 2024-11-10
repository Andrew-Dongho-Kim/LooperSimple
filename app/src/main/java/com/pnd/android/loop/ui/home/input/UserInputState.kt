package com.pnd.android.loop.ui.home.input

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.edit
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoop
import com.pnd.android.loop.data.putTo

@Stable
class UserInputState(
    prevSelector: InputSelector = InputSelector.NONE,
    currSelector: InputSelector = InputSelector.NONE,
    isOpen: Boolean,
    mode: Mode = Mode.None,
    value: LoopBase = LoopBase.default(isMock = true)
) {
    enum class Mode { None, New, Edit }

    var prevSelector by mutableStateOf(prevSelector)
        private set

    var currSelector by mutableStateOf(currSelector)
        private set

    val isSelectorOpened
        get() = currSelector != InputSelector.NONE

    var mode by mutableStateOf(mode)
        private set

    val isModeNone
        get() = mode == Mode.None

    var value by mutableStateOf(value)
        private set

    var isOpen by mutableStateOf(isOpen)
        private set
    val isVisible
        get() = isOpen || !isModeNone

    var textFieldValue by mutableStateOf(TextFieldValue(text = value.title))
        private set

    val isTitleEmpty
        get() = textFieldValue.text.isEmpty()

    private var hasTextFieldFocused by mutableStateOf(false)

    fun edit(value: LoopBase) {
        mode = Mode.Edit
        this.value = value.copyAs(isMock = true)
        textFieldValue = TextFieldValue(text = value.title)
    }

    fun update(
        title: TextFieldValue = this.textFieldValue,
        color: Int = value.color,
        loopStart: Long = value.loopStart,
        loopEnd: Long = value.loopEnd,
        loopActiveDays: Int = value.loopActiveDays,
        interval: Long = value.interval,
        enabled: Boolean = value.enabled,
    ) {
        value = value.copyAs(
            title = title.text,
            color = color,
            loopStart = loopStart,
            loopEnd = loopEnd,
            loopActiveDays = loopActiveDays,
            interval = interval,
            enabled = enabled,
            isMock = value.isMock,
        )
        textFieldValue = title
        ensureState()
    }

    fun toggleOpen(context: Context) {
        isOpen = !isOpen
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit { putBoolean(KEY_IS_USER_INPUT_OPEN, isOpen) }
    }

    fun setTextFieldFocused(focused: Boolean) {
        hasTextFieldFocused = focused
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

    fun reset(withValue: Boolean = true) {
        mode = Mode.None
        prevSelector = currSelector
        currSelector = InputSelector.NONE

        if (withValue) value = LoopBase.default(isMock = true)
        textFieldValue = TextFieldValue(value.title)
    }

    private fun ensureState() {
        if (currSelector == InputSelector.NONE) {
            if (isTitleEmpty && !hasTextFieldFocused) {
                if (mode == Mode.New) reset(withValue = false)

            } else if (mode == Mode.None) {
                mode = Mode.New
            }
        } else if (mode == Mode.None) {
            mode = Mode.New
        }
    }

    companion object {
        private const val STATE_PREV_SELECTOR = "state_prev_selector"
        private const val STATE_CURR_SELECTOR = "state_curr_selector"
        private const val STATE_IS_OPEN = "state_is_open"
        private const val STATE_MODE = "state_mode"

        val Saver = mapSaver(
            save = { state ->
                mutableMapOf<String, Any?>(
                    STATE_PREV_SELECTOR to state.prevSelector.name,
                    STATE_CURR_SELECTOR to state.currSelector.name,
                    STATE_IS_OPEN to state.isOpen,
                    STATE_MODE to state.mode.name
                ).also {
                    state.value.putTo(it)
                }
            },
            restore = { map ->
                UserInputState(
                    prevSelector = InputSelector.valueOf(map[STATE_PREV_SELECTOR] as String),
                    currSelector = InputSelector.valueOf(map[STATE_CURR_SELECTOR] as String),
                    isOpen = map[STATE_IS_OPEN] as Boolean,
                    mode = Mode.valueOf(map[STATE_MODE] as String),
                    value = map.asLoop()
                )
            }
        )
    }
}

@Composable
fun rememberUserInputState(context: Context) = rememberSaveable(saver = UserInputState.Saver) {
    UserInputState(
        isOpen = true,
//        context
//            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//            .getBoolean(KEY_IS_USER_INPUT_OPEN, true)
    )
}

enum class InputSelector {
    COLOR,
    START_END_TIME,
    ALARM_INTERVAL,
    NONE,
}

private const val PREF_NAME = "user_input_pref"
private const val KEY_IS_USER_INPUT_OPEN = "is_user_input_open"