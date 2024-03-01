package com.pnd.android.loop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

//private val darkColorPalette = darkColors(
//    primary = Blue400,
//    primaryVariant = Blue500,
//    onPrimary = Color(0xfffafafa),
//    secondary = Yellow600,
//    onSecondary = Color.Black,
//    onSurface = AppColor.onSurfaceDark,
//    onBackground = Color(0xfffafafa),
//    error = Red300,
//    onError = Color.Black
//)


private val darkColorScheme = lightColorScheme(
    primary = Blue400,
    onPrimary = Black99,
    surface = Black99,
    onSurface = AppColor.onSurfaceDark,
    background = Black99,
    onBackground = White99,
    error = Red300,
    onError = Black99,
)

//private val lightColorPalette = lightColors(
//    primary = Blue500,
//    primaryVariant = Blue800,
//    secondary = Yellow700,
//    secondaryVariant = Yellow800,
//    surface = White99,
//    background = White99,
//    error = Red800,
//
//    onPrimary = White99,
//    onSecondary = Color.Black,
//    onSurface = AppColor.onSurfaceLight,
//    onBackground = Black99,
//    onError = Color.White
//)

private val lightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = White99,
    surface = White99,
    onSurface = AppColor.onSurfaceLight,
    background = White99,
    onBackground = Black99,
    error = Red800,
    onError = White99,
)


val CORNERS_SMALL = 4.dp
val CORNERS_MEDIUM = 8.dp
val CORNERS_LARGE = 12.dp
val RoundShapes = Shapes(
    small = RoundedCornerShape(CORNERS_SMALL),
    medium = RoundedCornerShape(CORNERS_MEDIUM),
    large = RoundedCornerShape(CORNERS_LARGE)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme
    } else {
        lightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = RoundShapes,
        content = content
    )
}

