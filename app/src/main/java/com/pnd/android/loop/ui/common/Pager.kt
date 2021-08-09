@file:Suppress("unused")

package com.pnd.android.loop.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class PagerState(
    currentPage: Int = 0,
    minPage: Int = 0,
    maxPage: Int = 0,
    val infiniteScroll: Boolean = false,
    var onPageSelected: (Int) -> Unit = {}
) {
    private var _minPage by mutableStateOf(minPage)
    var minPage: Int
        get() = _minPage
        set(value) {
            _minPage = value.coerceAtMost(_maxPage)
            _currentPage = _currentPage.coerceIn(_minPage, _maxPage)
        }

    private var _maxPage by mutableStateOf(maxPage, structuralEqualityPolicy())
    var maxPage: Int
        get() = _maxPage
        set(value) {
            _maxPage = value.coerceAtLeast(_minPage)
            _currentPage = _currentPage.coerceIn(_minPage, maxPage)

            _currentPageOffset.updateBounds(-value.toFloat(), value.toFloat())
        }

    private var _currentPage by mutableStateOf(currentPage.coerceIn(minPage, maxPage))
    var currentPage: Int
        get() = _currentPage
        set(value) {
            val newPage = value.coerceIn(minPage, maxPage)
            if (_currentPage != newPage) {
                _currentPage = newPage
                onPageSelected(_currentPage)
            }
        }

    enum class SelectionState { Selected, Undecided }

    var selectionState by mutableStateOf(SelectionState.Selected)

    suspend inline fun <R> selectPage(block: PagerState.() -> R): R = try {
        selectionState = SelectionState.Undecided
        block()
    } finally {
        selectPage()
    }

    suspend fun selectPage() {
        currentPage -= currentPageOffset.roundToInt()
        snapToOffset(0f)
        selectionState = SelectionState.Selected
    }

    private var _currentPageOffset = Animatable(0f)

    val currentPageOffset: Float
        get() = _currentPageOffset.value

    private fun Float.coerceIn(): Float {
        val max = if (currentPage == minPage) 0f else (currentPage).toFloat()
        val min = if (currentPage == maxPage) 0f else (currentPage - maxPage).toFloat()
        return if (infiniteScroll) this else this.coerceIn(min, max)
    }

    suspend fun snapToOffset(offset: Float) {
        _currentPageOffset.snapTo(offset.coerceIn())
    }

    suspend fun fling(velocity: Float) {
        if (velocity < 0 && currentPage == maxPage) return
        if (velocity > 0 && currentPage == minPage) return

        val result = _currentPageOffset.animateDecay(velocity, exponentialDecay())

//        if (result.endReason != AnimationEndReason.Interrupted) {
        _currentPageOffset.animateTo(currentPageOffset.roundToInt().toFloat())


        selectPage()
//        }
    }

    override fun toString(): String = "PagerState{minPage=$minPage, maxPage=$maxPage, " +
            "currentPage=$currentPage, currentPageOffset=$currentPageOffset}"
}

@Immutable
private data class PageData(val page: Int) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@PageData
}

private val Measurable.page: Int
    get() = (parentData as? PageData)?.page ?: error("no PageData for measurable $this")

@Composable
fun Pager(
    modifier: Modifier = Modifier,
    state: PagerState,
    orientation: Orientation,
    offscreenLimit: Int = 2,
    velocityFactor: Float = 1.0f,
    pageContent: @Composable PagerScope.() -> Unit
) {
    var pageSize by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val dragState = rememberDraggableState { dy ->
        coroutineScope.launch {
            with(state) {
                val pos = pageSize * currentPageOffset
                val max = if (currentPage == minPage) 0 else pageSize * offscreenLimit
                val min = if (currentPage == maxPage) 0 else -pageSize * offscreenLimit
                val newPos = (pos + dy).coerceIn(min.toFloat(), max.toFloat())
                snapToOffset(newPos / pageSize)
            }
        }
    }

    Layout(
        content = {
            val minPage = (state.currentPage - offscreenLimit).coerceAtLeast(state.minPage)
            val maxPage = (state.currentPage + offscreenLimit).coerceAtMost(state.maxPage)

            for (page in minPage..maxPage) {
                val pageData = PageData(page)
                val scope = PagerScope(state, page)
                key(pageData) {
                    Box(contentAlignment = Alignment.Center, modifier = pageData) {
                        scope.pageContent()
                    }
                }
            }
        },
        modifier = modifier
            .clip(RectangleShape)
            .draggable(
                state = dragState,
                orientation = orientation,
                onDragStarted = {
                    state.selectionState = PagerState.SelectionState.Undecided
                },
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        // Velocity is in pixels per second, but we deal in percentage offsets, so we
                        // need to scale the velocity to match
                        state.fling((velocity / pageSize) * velocityFactor)
                    }
                }
            )

    ) { measurables, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            val currentPage = state.currentPage
            val offset = state.currentPageOffset
            val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val isHorizontal = orientation == Orientation.Horizontal

            measurables.map {
                it.measure(childConstraints) to it.page

            }.forEach { (placeable, page) ->
                val centerX = (constraints.maxWidth - placeable.width) / 2
                val centerY = (constraints.maxHeight - placeable.height) / 2

                if (currentPage == page) {
                    pageSize = if (isHorizontal) placeable.width else placeable.height
                }

                val pos = ((page + offset - currentPage) * pageSize).roundToInt()
                val xOffset = if (isHorizontal) pos else 0
                val yOffset = if (isHorizontal) 0 else pos

                val left = centerX + xOffset
                val top = centerY + yOffset

                placeable.place(x = left, y = top)
            }
        }
    }

}

/**
 * Scope for [Pager] content.
 */
class PagerScope(
    private val state: PagerState,
    val page: Int
) {
    /**
     * Returns the current selected page
     */
    val currentPage: Int
        get() = state.currentPage

    /**
     * Returns the current selected page offset
     */
    val currentPageOffset: Float
        get() = state.currentPageOffset

    /**
     * Returns the current selection state
     */
    val selectionState: PagerState.SelectionState
        get() = state.selectionState
}
