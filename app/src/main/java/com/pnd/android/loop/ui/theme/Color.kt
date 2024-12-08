package com.pnd.android.loop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp

val BlueGreen = Color(0xFF064D8A)

val Blue10 = Color(0xFF000F5E)
val Blue20 = Color(0xFF001E92)
val Blue30 = Color(0xFF002ECC)
val Blue40 = Color(0xFF1546F6)
val Blue80 = Color(0xFFB8C3FF)
val Blue90 = Color(0xFFDDE1FF)

val DarkBlue10 = Color(0xFF00036B)
val DarkBlue20 = Color(0xFF000BA6)
val DarkBlue30 = Color(0xFF1026D3)
val DarkBlue40 = Color(0xFF3648EA)
val DarkBlue80 = Color(0xFFBBC2FF)
val DarkBlue90 = Color(0xFFDEE0FF)

val Yellow10 = Color(0xFF261900)
val Yellow20 = Color(0xFF402D00)
val Yellow30 = Color(0xFF5C4200)
val Yellow40 = Color(0xFF7A5900)
val Yellow80 = Color(0xFFFABD1B)
val Yellow90 = Color(0xFFFFDE9C)

val Red10 = Color(0xFF410001)
val Red20 = Color(0xFF680003)
val Red30 = Color(0xFF930006)
val Red40 = Color(0xFFBA1B1B)
val Red80 = Color(0xFFFFB4A9)
val Red90 = Color(0xFFFFDAD4)

val Grey10 = Color(0xFF191C1D)
val Grey20 = Color(0xFF2D3132)
val Grey80 = Color(0xFFC4C7C7)
val Grey90 = Color(0xFFE0E3E3)
val Grey95 = Color(0xFFEFF1F1)
val Grey99 = Color(0xFFFBFDFD)

val BlueGrey30 = Color(0xFF45464F)
val BlueGrey50 = Color(0xFF767680)
val BlueGrey60 = Color(0xFF90909A)
val BlueGrey80 = Color(0xFFC6C5D0)
val BlueGrey90 = Color(0xFFE2E1EC)

val White99 = Color(0xfffafafa)
val Black99 = Color(0xff010101)


val Yellow400 = Color(0xFFF6E547)
val Yellow600 = Color(0xFFF5CF1B)
val Yellow700 = Color(0xFFF3B711)
val Yellow800 = Color(0xFFF29F05)

val Blue200 = Color(0xFF9DA3FA)
val Blue400 = Color(0xFF4860F7)
val Blue500 = Color(0xFF0540F2)
val Blue800 = Color(0xFF001CCF)


val WineWhite = Color.White
val WineRed = Color(0xFFE30425)
val WinePurple700 = Color(0xFF720D5D)
val WinePurple800 = Color(0xFF5D1049)
val WinePurple900 = Color(0xFF4E0D3A)


val Red300 = Color(0xFFEA6D7E)
val Red500 = Color(0XFFF44336)
val Red800 = Color(0xFFD00036)


val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)


@Composable
fun Int.compositeOverOnSurface(alpha: Float = 0.8f): Color {
    return Color(this).compositeOverOnSurface(alpha = alpha)
}

@Composable
fun Int.compositeOver(alpha: Float = 0.8f, color: Color): Color {
    return Color(this).copy(alpha = alpha).compositeOver(color)
}

@Composable
fun Color.compositeOverSurface(alpha: Float = 0.8f): Color {
    return this.copy(alpha = alpha).compositeOver(AppColor.surface)
}

@Composable
fun Color.compositeOverOnSurface(alpha: Float = 0.8f): Color {
    return this.copy(alpha = alpha).compositeOver(AppColor.onSurface)
}


@Composable
fun compositeOverSurface(): Color {
    return compositedOnSurface(if (isSystemInDarkTheme()) 0.1f else 0.05f)
}

/**
 * Return the fully opaque color that results from compositing [onSurface] atop [surface] with the
 * given [alpha]. Useful for situations where semi-transparent colors are undesirable.
 */
@Composable
fun compositedOnSurface(alpha: Float): Color {
    return AppColor.onSurface.copy(alpha = alpha).compositeOver(AppColor.surface)
}

/**
 * Calculates the color of an elevated `surface` in dark mode. Returns `surface` in light mode.
 */
@Composable
fun Colors.elevatedSurface(elevation: Dp): Color {
    return LocalElevationOverlay.current?.apply(
        color = this.surface,
        elevation = elevation
    ) ?: this.surface
}

object AppColor

val AppColor.primary
    @Composable get() = MaterialTheme.colorScheme.primary

val AppColor.secondary
    @Composable get() = MaterialTheme.colorScheme.secondary
val AppColor.error
    @Composable get() = MaterialTheme.colorScheme.error
val AppColor.background
    @Composable get() = MaterialTheme.colorScheme.background //onSurface.copy(alpha = 0.02f)
val AppColor.surface
    @Composable get() = MaterialTheme.colorScheme.surface

val AppColor.onSurfaceLight get() = Black99
val AppColor.onSurfaceDark get() = White99
val AppColor.onSurface
    @Composable get() = MaterialTheme.colorScheme.onSurface
val AppColor.onBackground
    @Composable get() = MaterialTheme.colorScheme.onBackground

val AppColor.outline
    @Composable get() = MaterialTheme.colorScheme.outline

val AppColor.outlineVariant
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

val AppColor.divider
    @Composable get() = AppColor.onSurface.copy(alpha = 0.3f)