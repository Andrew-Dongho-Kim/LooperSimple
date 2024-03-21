package com.pnd.android.loop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pnd.android.loop.R


private val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat_regular),
    Font(R.font.montserrat_light, FontWeight.Light),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold)
)

private val KarlaFontFamily = FontFamily(
    Font(R.font.karla_regular),
    Font(R.font.karla_bold, FontWeight.Bold)
)

val AppTypography = Typography(
//    defaultFontFamily = MontserratFontFamily,
    displayLarge = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.25.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.15.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.sp
    )
)
