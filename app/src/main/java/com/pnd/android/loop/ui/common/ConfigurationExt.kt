package com.pnd.android.loop.ui.common

import android.content.res.Configuration


fun Configuration.isPortrait() = orientation == Configuration.ORIENTATION_PORTRAIT

fun Configuration.isLargeScreen() = screenWidthDp >= 520