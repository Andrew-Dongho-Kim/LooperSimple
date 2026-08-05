package com.pnd.android.loop.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized spacing tokens shared across every screen.
 *
 * Using one source of truth keeps the horizontal alignment line and the vertical
 * rhythm consistent between Home, Detail, Statistics and History screens,
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

    /**
     * Size (diameter) of the floating add-loop button that hovers over the scrolling content.
     * Shared so the button, the snackbar that must clear it, and the list's bottom spacer all
     * derive from one value instead of repeating the magic number per file.
     */
    val floatingInputButtonSize = 56.dp

    /** Margin from the screen edges to the floating add-loop button. */
    val floatingInputButtonMargin = 32.dp

    /**
     * Bottom clearance reserved below scrolling content so the last item isn't hidden behind the
     * floating add-loop button. Used by both the loop list and the empty-state screen.
     */
    val bottomContentClearance = 150.dp
}
