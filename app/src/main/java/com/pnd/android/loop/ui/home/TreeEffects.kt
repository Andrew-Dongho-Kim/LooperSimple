package com.pnd.android.loop.ui.home

/** Shared effect slots; each tree supplies its own artwork and display names. */
internal enum class TreeEffect(val level: Int, val inputName: String) {
    SUNLIGHT(1, "sunlightEnabled"), FIREFLIES(2, "firefliesEnabled"),
    FLOWERS(3, "flowersEnabled"), BUTTERFLIES(4, "butterfliesEnabled"), AURORA(5, "auroraEnabled");

    val bit: Int get() = 1 shl (level - 1)
    fun enabled(mask: Int): Boolean = mask and bit != 0
    fun visible(mask: Int, rewardLevel: Int, wilt: Int = 0): Boolean =
        wilt == 0 && rewardLevel >= level && enabled(mask)

    companion object { const val ALL = 31 }
}
