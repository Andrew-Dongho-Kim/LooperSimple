package com.pnd.android.loop.ui.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel

@Composable
fun ActivePage(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    Box(
        modifier = modifier
            .background(color = Color.Blue)
            .fillMaxWidth()
            .fillMaxHeight()
    )
}