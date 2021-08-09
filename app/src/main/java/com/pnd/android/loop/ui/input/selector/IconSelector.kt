package com.pnd.android.loop.ui.input

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.common.log
import com.pnd.android.loop.ui.common.SelectorButton
import com.pnd.android.loop.ui.icons.ALL_ICON_CATEGORIES
import com.pnd.android.loop.ui.icons.IconCategory
import com.pnd.android.loop.ui.icons.category
import kotlin.math.min
import kotlin.math.roundToInt

private val logger = log("IconSelector")

private const val ICON_COLUMNS = 6

@Composable
fun IconSelector(
    selectedIcon: Int,
    onIconSelected: (ImageVector, Int) -> Unit,
    focusRequester: FocusRequester
) {
    var selectedCategory by remember { mutableStateOf(category(selectedIcon)) }

    val description = stringResource(id = R.string.desc_icon_selector)
    Column(
        modifier = Modifier
            .height(dimensionResource(id = R.dimen.user_input_selector_content_height))
            .focusRequester(focusRequester)
            .focusTarget()
            .semantics { contentDescription = description }
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            ALL_ICON_CATEGORIES.forEach { category ->
                InnerTabButton(
                    text = stringResource(id = category.titleResId),
                    onClick = { selectedCategory = category },
                    selected = selectedCategory == category
                )
            }
        }

        Row(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            IconTable(
                category = selectedCategory,
                selectedIcon = selectedIcon,
                onIconSelected = onIconSelected,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}


@Composable
fun IconTable(
    category: IconCategory,
    selectedIcon: Int,
    onIconSelected: (ImageVector, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = category.items
    Column(modifier.fillMaxWidth()) {
        repeat(icons.size / ICON_COLUMNS + if (icons.size % ICON_COLUMNS > 0) 1 else 0) { x ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = SpaceEvenlyStart
            ) {

                repeat(min((icons.size - ICON_COLUMNS * x), ICON_COLUMNS)) { y ->
                    val idx = x * ICON_COLUMNS + y
                    val icon = icons[idx]
                    SelectorButton(
                        modifier = Modifier
                            .sizeIn(minWidth = 42.dp, minHeight = 42.dp)
                            .padding(8.dp),
                        icon = icon,
                        selected = selectedIcon == category.key(idx),
                        contentDescription = null,
                        onClick = {
                            onIconSelected(icon, category.key(idx))
                            logger.d { "onIconSelected : $idx" }
                        }
                    )
                }
            }
        }
    }
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
    val consumedSize = (size.fold(0) { a, b -> a + b } / size.size) * ICON_COLUMNS
    val gapSize = (totalSize - consumedSize).toFloat() / (ICON_COLUMNS + 1)
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



