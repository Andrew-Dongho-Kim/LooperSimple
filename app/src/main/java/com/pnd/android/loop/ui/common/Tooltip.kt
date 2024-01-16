package com.pnd.android.loop.ui.common

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt


@Composable
fun Tooltip(
    modifier: Modifier = Modifier,
    anchorContent: @Composable (Modifier) -> Unit,
    tooltipContent: @Composable () -> Unit,
    tooltipBackground: Color,
    tooltipShape: Shape,
) {
    var isShown by rememberSaveable { mutableStateOf(false) }
    var position by remember { mutableStateOf(TooltipPosition()) }

    if (isShown) {
        TooltipPopup(
            onDismissRequest = { isShown = !isShown },
            position = position,
            backgroundColor = tooltipBackground,
            shape = tooltipShape,
        ) {
            tooltipContent()
        }
    }

    val rootView = LocalView.current.rootView
    anchorContent(
        modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { isShown = !isShown }
            )
            .onGloballyPositioned { coordinates ->
                position = calculateTooltipPopupPosition(rootView, coordinates)
            }
    )
}

@Composable
private fun TooltipPopup(
    position: TooltipPosition,
    backgroundColor: Color,
    shape: Shape = MaterialTheme.shapes.medium,
    arrowHeight: Dp = 4.dp,
    paddingHorizontal: Dp = 16.dp,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val arrowPaddingPx = with(density) { arrowHeight.toPx().roundToInt() * 3 }
    var arrowX by remember { mutableFloatStateOf(position.centerX) }

    var offset = position.offset
    offset = when (position.alignment) {
        Alignment.TopCenter -> offset.copy(y = offset.y + arrowPaddingPx)
        Alignment.BottomCenter -> offset.copy(y = offset.y - arrowPaddingPx)
        else -> offset
    }

    val positionProvider = remember(position.alignment, offset) {
        TooltipPositionProvider(
            alignment = position.alignment,
            offset = offset,
            paddingHorizontalPx = with(density) { paddingHorizontal.toPx() },
            centerX = position.centerX,
            onArrowPositionX = { x -> arrowX = x }
        )
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(dismissOnBackPress = false),
    ) {
        BubbleContent(
            modifier = Modifier
                .padding(horizontal = paddingHorizontal)
                .background(
                    color = backgroundColor,
                    shape = shape,
                ),
            alignment = position.alignment,
            arrowColor = backgroundColor,
            arrowHeight = arrowHeight,
            arrowX = arrowX,
        ) {
            content()
        }
    }
}

internal class TooltipPositionProvider(
    val alignment: Alignment,
    val offset: IntOffset,
    val centerX: Float,
    val paddingHorizontalPx: Float,
    private val onArrowPositionX: (Float) -> Unit,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        var popupOffset = popupOffset(
            anchorBounds = anchorBounds,
            layoutDirection = layoutDirection,
            popupContentSize = popupContentSize,
        )

        val leftSpace = centerX - paddingHorizontalPx
        val rightSpace = windowSize.width - centerX - paddingHorizontalPx

        val tooltipWidth = popupContentSize.width
        val halfPopupContentSize = popupContentSize.center.x

        val fullPadding = paddingHorizontalPx * 2

        val maxTooltipSize = windowSize.width - fullPadding

        val isCentralPositionTooltip =
            halfPopupContentSize <= leftSpace && halfPopupContentSize <= rightSpace

        when {
            isCentralPositionTooltip -> {
                popupOffset = IntOffset(centerX.toInt() - halfPopupContentSize, popupOffset.y)
                onArrowPositionX.invoke(halfPopupContentSize.toFloat() - paddingHorizontalPx)
            }

            tooltipWidth >= maxTooltipSize -> {
                popupOffset = IntOffset(windowSize.center.x - halfPopupContentSize, popupOffset.y)
                onArrowPositionX.invoke(centerX - popupOffset.x - paddingHorizontalPx)
            }

            halfPopupContentSize > rightSpace -> {
                popupOffset = IntOffset(centerX.toInt(), popupOffset.y)
                onArrowPositionX.invoke(halfPopupContentSize + (halfPopupContentSize - rightSpace) - fullPadding)
            }

            halfPopupContentSize > leftSpace -> {
                popupOffset = IntOffset(0, popupOffset.y)
                onArrowPositionX.invoke(centerX - paddingHorizontalPx)
            }

            else -> onArrowPositionX.invoke(centerX)
        }
        return popupOffset
    }

    private fun popupOffset(
        anchorBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        var popupOffset = IntOffset(0, 0)

        // Get the aligned point inside the parent
        val parentAlignmentPoint = alignment.align(
            IntSize.Zero,
            IntSize(anchorBounds.width, anchorBounds.height),
            layoutDirection
        )
        // Get the aligned point inside the child
        val relativePopupPos = alignment.align(
            IntSize.Zero,
            IntSize(popupContentSize.width, popupContentSize.height),
            layoutDirection
        )

        // Add the position of the parent
        popupOffset += IntOffset(anchorBounds.left, anchorBounds.top)

        // Add the distance between the parent's top left corner and the alignment point
        popupOffset += parentAlignmentPoint

        // Subtract the distance between the children's top left corner and the alignment point
        popupOffset -= IntOffset(relativePopupPos.x, relativePopupPos.y)

        // Add the user offset
        val resolvedOffset = IntOffset(
            offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1),
            offset.y
        )

        popupOffset += resolvedOffset
        return popupOffset
    }
}

@Composable
private fun BubbleContent(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopCenter,
    arrowColor: Color,
    arrowHeight: Dp,
    arrowX: Float,
    content: @Composable () -> Unit
) {
    val arrowHeightPx = with(LocalDensity.current) { arrowHeight.toPx() }

    Box(
        modifier = modifier
            .drawBehind {
                if (arrowX <= 0f) return@drawBehind

                val isTopCenter = alignment == Alignment.TopCenter

                val path = Path()

                if (isTopCenter) {
                    path.drawTopArrow(
                        offset = Offset(arrowX, 0f),
                        height = arrowHeightPx
                    )

                } else {
                    path.drawBottomArrow(
                        offset = Offset(arrowX, drawContext.size.height),
                        height = arrowHeightPx
                    )
                }

                drawPath(
                    path = path,
                    color = arrowColor,
                )
                path.close()
            }
    ) {
        content()
    }
}

private fun Path.drawTopArrow(offset: Offset, height: Float) {
    moveTo(x = offset.x, y = offset.y)
    lineTo(x = offset.x - height, y = offset.y)
    lineTo(x = offset.x, y = offset.y - height)
    lineTo(x = offset.x + height, y = offset.y)
    lineTo(x = offset.x, y = offset.y)
}

private fun Path.drawBottomArrow(offset: Offset, height: Float) {
    moveTo(x = offset.x, y = offset.y)
    lineTo(x = offset.x + height, y = offset.y)
    lineTo(x = offset.x, y = offset.y + height)
    lineTo(x = offset.x - height, y = offset.y)
    lineTo(x = offset.x, y = offset.y)
}

data class TooltipPosition(
    val alignment: Alignment = Alignment.TopCenter,
    val offset: IntOffset = IntOffset(0, 0),
    val centerX: Float = 0f,
)

fun calculateTooltipPopupPosition(
    view: View,
    coordinates: LayoutCoordinates?,
): TooltipPosition {
    coordinates ?: return TooltipPosition()

    val windowBounds = android.graphics.Rect()
    view.getWindowVisibleDisplayFrame(windowBounds)

    val anchorBounds = coordinates.boundsInWindow()

    val heightAbove = anchorBounds.top - windowBounds.top
    val heightBelow = windowBounds.height() - anchorBounds.bottom

    val centerX = anchorBounds.left + anchorBounds.width / 2
    val offsetX = centerX - windowBounds.centerX()

    return if (heightAbove < heightBelow) {
        TooltipPosition(
            offset = IntOffset(
                x = offsetX.toInt(),
                y = coordinates.size.height,
            ),
            alignment = Alignment.TopCenter,
            centerX = centerX,
        )
    } else {
        TooltipPosition(
            offset = IntOffset(
                x = offsetX.toInt(),
                y = -coordinates.size.height,
            ),
            alignment = Alignment.BottomCenter,
            centerX = centerX,
        )
    }
}
