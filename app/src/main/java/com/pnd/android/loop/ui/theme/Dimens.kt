package com.pnd.android.loop.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


val PADDING_HZ_ITEM = 16.dp
val PADDING_VT_ITEM = 12.dp

fun Modifier.itemPadding() = padding(
    start = PADDING_HZ_ITEM,
    end = PADDING_HZ_ITEM,
    top = PADDING_VT_ITEM,
    bottom = PADDING_VT_ITEM
)


val CORNERS_SMALL = 4.dp
val CORNERS_MEDIUM = 8.dp
val CORNERS_LARGE = 12.dp
