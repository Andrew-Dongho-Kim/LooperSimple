package com.pnd.android.loop.ui.input.common

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.theme.compositedOnSurface
import com.pnd.android.loop.ui.theme.elevatedSurface

val KeyboardShownKey = SemanticsPropertyKey<Boolean>("KeyboardShownKey")
var SemanticsPropertyReceiver.keyboardShownProperty by KeyboardShownKey

@Composable
fun getSelectorExpandedColor(): Color {
    return if (MaterialTheme.colors.isLight) {
        MaterialTheme.colors.compositedOnSurface(0.04f)
    } else {
        MaterialTheme.colors.elevatedSurface(8.dp)
    }
}