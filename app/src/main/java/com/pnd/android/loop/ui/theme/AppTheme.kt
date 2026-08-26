package com.pnd.android.loop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.material3.ColorProviders

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
    secondary = Yellow80,
    surface = Black99,
    onSurface = AppColor.onSurfaceDark,
    background = Black99,
    onBackground = White99,
    error = Red300,
    onError = Black99,
    outline = Grey20
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
    secondary = Yellow700,
    surface = White99,
    onSurface = AppColor.onSurfaceLight,
    background = White99,
    onBackground = Black99,
    error = Red800,
    onError = White99,
    outline = Grey80
)


/**
 * Day/night color providers for the Glance app widget, derived from the same
 * Material3 schemes the app UI uses so the widget follows the system theme.
 */
val AppWidgetColorProviders = ColorProviders(
    light = lightColorScheme,
    dark = darkColorScheme,
)

/** 낮/밤 한 쌍의 색. */
data class DayNightColor(
    val day: Color,
    val night: Color,
)

/**
 * 위젯이 쓰는 색의 낮/밤 원본. 앱 UI 와 같은 스킴에서 뽑으므로 앱과 위젯의 색이 갈리지 않는다.
 *
 * [AppWidgetColorProviders] 로도 같은 색을 얻을 수 있지만, 그쪽에서 색을 꺼내면 그리는 시점의
 * 테마 한 벌로 확정된다. 위젯은 "onSurface 를 5% 알파로 깐 배경" 같은 파생 톤을 많이 쓰는데,
 * 그런 톤까지 낮/밤 두 벌로 만들려면 원본 색 두 벌이 필요하다
 * ([com.pnd.android.loop.appwidget.ui] 의 색 토큰 참고).
 */
object AppWidgetPalette {
    val surface = DayNightColor(
        day = lightColorScheme.surface,
        night = darkColorScheme.surface,
    )
    val onSurface = DayNightColor(
        day = lightColorScheme.onSurface,
        night = darkColorScheme.onSurface,
    )
    val primary = DayNightColor(
        day = lightColorScheme.primary,
        night = darkColorScheme.primary,
    )
}

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

