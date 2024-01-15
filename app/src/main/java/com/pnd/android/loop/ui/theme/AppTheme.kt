package com.pnd.android.loop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val darkColorPalette = darkColors(
    primary = Blue400,
    primaryVariant = Blue500,
    onPrimary = Color(0xfffafafa),
    secondary = Yellow600,
    onSecondary = Color.Black,
    onSurface = Color(0xfffafafa),
    onBackground = Color(0xfffafafa),
    error = Red300,
    onError = Color.Black
)

private val lightColorPalette = lightColors(
    primary = Blue500,
    primaryVariant = Blue800,
    secondary = Yellow700,
    secondaryVariant = Yellow800,
    surface = White99,
    background = White99,
    error = Red800,

    onPrimary = Color.White,
    onSecondary = Color.Black,
    onSurface = Black99,
    onBackground = Black99,
    onError = Color.White
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorPalette
    } else {
        lightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = AppTypography,
        shapes = RoundShapes,
        content = content
    )
}