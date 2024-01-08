package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

class LoopCardShape(size: Dp) : Shape {
    private val cornerRadius = CornerSize(size)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = createOutlinePath(density, size)
        )
    }

    fun createOutlinePath(
        density: Density,
        size: Size
    ) = Path().apply {
        val radius = cornerRadius.toPx(size, density)

        reset()
        // Top left arc
        arcTo(
            rect = Rect(
                left = 0f,
                top = -radius,
                right = radius * 2f,
                bottom = radius
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = -90f,
            forceMoveTo = false
        )
        lineTo(x = size.width - 2 * radius, y = 0f)

        // Top right arc
        arcTo(
            rect = Rect(
                left = size.width - radius * 2f,
                top = -radius,
                right = size.width,
                bottom = radius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = -90f,
            forceMoveTo = false
        )

        // Center right arc
        arcTo(
            rect = Rect(
                left = size.width - 2 * radius,
                top = radius,
                right = size.width,
                bottom = size.height - radius,
            ),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )

        // Bottom right arc
        arcTo(
            rect = Rect(
                left = size.width - 2 * radius,
                top = size.height - radius,
                right = size.width,
                bottom = size.height + radius
            ),
            startAngleDegrees = 270f,
            sweepAngleDegrees = -90f,
            forceMoveTo = false
        )

        lineTo(x = radius * 2f, y = size.height)
        // Bottom left arc
        arcTo(
            rect = Rect(
                left = 0f,
                top = size.height - radius,
                right = radius * 2f,
                bottom = size.height + radius
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = -90f,
            forceMoveTo = false
        )
        // Center left arc
        arcTo(
            rect = Rect(
                left = 0f,
                top = radius,
                right = radius * 2f,
                bottom = size.height - radius
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        close()
    }
}
