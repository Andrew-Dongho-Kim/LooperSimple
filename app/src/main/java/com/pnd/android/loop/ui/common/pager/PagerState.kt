package com.pnd.android.loop.ui.common.pager

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
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


        if (result.endReason != AnimationEndReason.BoundReached && result.endReason != AnimationEndReason.BoundReached) {
            _currentPageOffset.animateTo(currentPageOffset.roundToInt().toFloat())
            selectPage()
        }
    }

    override fun toString(): String = "PagerState{minPage=$minPage, maxPage=$maxPage, " +
            "currentPage=$currentPage, currentPageOffset=$currentPageOffset}"
}