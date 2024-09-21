package com.pnd.android.loop.appwidget.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.glance.LocalContext

@Composable
fun stringResourceGlance(@StringRes id: Int, vararg formatArgs: Any) =
    LocalContext.current.getString(id, *formatArgs)
