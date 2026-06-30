package com.pnd.android.loop.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized spacing tokens shared across every screen.
 *
 * Using one source of truth keeps the horizontal alignment line and the vertical
 * rhythm consistent between Home, Detail, Statistics, History and Group screens,
 * instead of scattering magic numbers (24 / 12 / 8 dp) per file.
 */
object Dimens {
    /** Left/right inset that every screen's main content lines up to. */
    val screenHorizontalPadding = 20.dp

    /** Gap between two stacked list cards (applied as vertical padding per card). */
    val cardSpacing = 8.dp

    /** Inner padding used inside cards and sheets. */
    val contentPadding = 16.dp

    /** Vertical gap that separates two logical sections on a screen. */
    val sectionSpacing = 28.dp

    /** Small gap between tightly related elements (icon + label, value + caption). */
    val itemSpacing = 6.dp
}
