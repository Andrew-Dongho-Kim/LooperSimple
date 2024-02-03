package com.pnd.android.loop.appwidget.ui

import androidx.compose.runtime.Composable
import androidx.glance.LocalContext


@Composable
fun stringResourceGlide(resId: Int, vararg args: Any) =
    LocalContext.current.getString(resId, *args)
