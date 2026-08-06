package com.pnd.android.loop.ui.home.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberLocalTime(): State<LocalTime> {
    val localTime = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(key1 = Unit) {
        while (isActive) {
            // Sleep until the start of the next minute. The previous arithmetic produced
            // (hour+1):59 at minute 59 (stalling ~1h) and crashed at 23:59 (LocalTime.of(24, ..)).
            val now = LocalTime.now()
            val msIntoMinute = now.second * 1000L + now.nano / 1_000_000L
            delay((60_000L - msIntoMinute).milliseconds)
            localTime.value = LocalTime.now()
        }
    }
    return localTime
}
