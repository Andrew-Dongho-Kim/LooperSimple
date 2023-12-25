package com.pnd.android.loop.ui.input.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopVo
import kotlin.math.min
import kotlin.math.roundToInt

private const val COLOR_COLUMNS = 6

@Composable
fun ColorSelector(
    modifier: Modifier = Modifier,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    val description = stringResource(id = R.string.desc_color_selector)
    Column(
        modifier = modifier
            .padding(top = 12.dp)
            .semantics { contentDescription = description }
    ) {
        val colors = LoopVo.SUPPORTED_COLORS
        val numberOfRows = (colors.size + COLOR_COLUMNS - 1) / COLOR_COLUMNS

        repeat(numberOfRows) { row ->
            ColorRows(
                row = row,
                selectedColor = selectedColor,
                onColorSelected = onColorSelected
            )
        }
    }
}

@Composable
private fun ColorRows(
    modifier: Modifier = Modifier,
    row: Int,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = SpaceEvenlyStart
    ) {
        val colors = LoopVo.SUPPORTED_COLORS
        val numberOfColumns = min((colors.size - COLOR_COLUMNS * row), COLOR_COLUMNS)

        repeat(numberOfColumns) { column ->
            val index = row * COLOR_COLUMNS + column
            val color = colors[index]

            val isSelected = color == selectedColor
            ColorBox(
                color = color,
                isSelected = isSelected,
                onColorSelected = onColorSelected
            )
        }
    }
}

@Composable
private fun ColorBox(
    modifier: Modifier = Modifier,
    color: Int,
    isSelected: Boolean,
    onColorSelected: (Int) -> Unit,
) {
    val background = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
    val border = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
    val round = 8.dp
    Box(
        modifier = modifier
            .size(32.dp)
            .drawBehind {
                if (isSelected) {
                    drawRoundRect(
                        color = background,
                        cornerRadius = CornerRadius(
                            x = round.toPx(),
                            y = round.toPx()
                        )
                    )
                    drawRoundRect(
                        color = border,
                        cornerRadius = CornerRadius(
                            x = round.toPx(),
                            y = round.toPx()
                        ),
                        style = Stroke(
                            width = 0.5.dp.toPx()
                        )
                    )

                }
            }
            .clip(RoundedCornerShape(round))
            .clickable { onColorSelected(color) }
            .padding(8.dp)
            .drawBehind { drawCircle(Color(color)) }
            .background(
                color = Color(color),
                shape = CircleShape
            )
    )
}


@Stable
private val SpaceEvenlyStart = object : Arrangement.HorizontalOrVertical {
    override val spacing = 0.dp

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray
    ) = if (layoutDirection == LayoutDirection.Ltr) {
        placeStartSpaceEvenly(
            totalSize,
            sizes,
            outPositions,
            reverseInput = false
        )
    } else {
        placeStartSpaceEvenly(
            totalSize,
            sizes,
            outPositions,
            reverseInput = true
        )
    }

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray
    ) = placeStartSpaceEvenly(
        totalSize,
        sizes,
        outPositions,
        reverseInput = false
    )

    override fun toString() = "Arrangement#SpaceEvenlyStart"
}


private fun placeStartSpaceEvenly(
    totalSize: Int,
    size: IntArray,
    outPosition: IntArray,
    reverseInput: Boolean
) {
    val consumedSize = (size.fold(0) { a, b -> a + b } / size.size) * COLOR_COLUMNS
    val gapSize = (totalSize - consumedSize).toFloat() / (COLOR_COLUMNS + 1)
    var current = gapSize

    size.forEachIndexed(reverseInput) { index, it ->
        if (index >= outPosition.size) return

        outPosition[index] = current.roundToInt()
        current += it.toFloat() + gapSize
    }
}

private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (Int, Int) -> Unit) {
    if (!reversed) {
        forEachIndexed(action)
    } else {
        for (i in (size - 1) downTo 0) {
            action(i, get(i))
        }
    }
}



