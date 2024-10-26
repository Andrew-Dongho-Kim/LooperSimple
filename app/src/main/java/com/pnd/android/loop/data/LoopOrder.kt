package com.pnd.android.loop.data

import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isPast


class TodayLoopOrder : Comparator<LoopBase> {
    override fun compare(loop1: LoopBase, loop2: LoopBase): Int {
        var comp = compareEnabled(loop1, loop2)
        if (comp != 0) return comp

        comp = compareActive(loop1, loop2)
        if (comp != 0) return comp

        comp = comparePast(loop1, loop2)
        if (comp != 0) return comp

        return (loop1.loopEnd - loop2.loopEnd).toInt()
    }

    private fun compareEnabled(loop1: LoopBase, loop2: LoopBase) =
        loop1.enabledValue() - loop2.enabledValue()

    private fun compareActive(loop1: LoopBase, loop2: LoopBase) =
        loop1.activeValue() - loop2.activeValue()

    private fun comparePast(loop1: LoopBase, loop2: LoopBase) =
        loop1.pastValue() - loop2.pastValue()

    private fun LoopBase.enabledValue() = if (enabled) 0 else 1
    private fun LoopBase.activeValue() = if (isActive()) 0 else 1
    private fun LoopBase.pastValue() = if (isPast()) 1 else 0
}