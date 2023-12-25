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
            startAngleDegrees = 90.0f,
            sweepAngleDegrees = -90.0f,
            forceMoveTo = false
        )
        lineTo(x = size.width - radius, y = 0f)
        // Top right arc
        arcTo(
            rect = Rect(
                left = size.width - radius * 2f,
                top = 0f,
                right = size.width,
                bottom = radius * 2f
            ),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        lineTo(x = size.width, y = size.height - radius)
        // Bottom right arc
        arcTo(
            rect = Rect(
                left = size.width - radius * 2f,
                top = size.height - radius * 2f,
                right = size.width,
                bottom = size.height
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90.0f,
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
            startAngleDegrees = 0.0f,
            sweepAngleDegrees = -90.0f,
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
            sweepAngleDegrees = 180.0f,
            forceMoveTo = false
        )
        close()
    }
}
