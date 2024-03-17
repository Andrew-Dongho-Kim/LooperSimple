package com.pnd.android.loop.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun rememberBlurState(radius: Dp = 0.dp) = rememberSaveable(saver = BlurState.Saver) {
    BlurState(radius)
}

@Stable
class BlurState(
    radius: Dp
) {
    var radius by mutableStateOf(radius)

    val isOn get() = radius > 0.dp
    fun on() {
        radius = 5.dp
    }

    fun off() {
        radius = 0.dp
    }

    companion object {
        val Saver = listSaver(
            save = { state ->
                listOf(state.radius.value)
            },
            restore = { list ->
                BlurState(radius = (list[0] as Float).dp)
            }
        )
    }
}
