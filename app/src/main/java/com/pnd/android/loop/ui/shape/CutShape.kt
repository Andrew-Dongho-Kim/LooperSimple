package com.pnd.android.loop.ui.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class CutShape(
    private val topLeft: Dp = 0.dp,
    private val topRight: Dp = 0.dp,
    private val bottomLeft: Dp = 0.dp,
    private val bottomRight: Dp = 0.dp,
) : Shape {
    constructor(all: Dp) : this(
        topLeft = all,
        topRight = all,
        bottomLeft = all,
        bottomRight = all,
    )

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = createOutlinePath(density, size)
        )
    }

    private fun createOutlinePath(
        density: Density,
        size: Size
    ) = Path().apply {
        val pxTopLeft = with(density) { topLeft.toPx() }
        val pxTopRight = with(density) { topRight.toPx() }
        val pxBottomLeft = with(density) { bottomLeft.toPx() }
        val pxBottomRight = with(density) { bottomRight.toPx() }

        reset()
        moveTo(x = pxTopLeft, y = 0f)
        lineTo(x = size.width - pxTopRight, y = 0f)
        lineTo(x = size.width, y = pxTopRight)
        lineTo(x = size.width, y = size.height - pxBottomRight)
        lineTo(x = size.width - pxBottomRight, y = size.height)
        lineTo(x = pxBottomLeft, y = size.height)
        lineTo(x = 0f, y = size.height - pxBottomLeft)
        lineTo(x = 0f, y = pxTopLeft)
        lineTo(x = pxTopLeft, y = 0f)
    }
}