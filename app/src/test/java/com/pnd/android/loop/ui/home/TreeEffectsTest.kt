package com.pnd.android.loop.ui.home

import org.junit.Assert.*
import org.junit.Test

class TreeEffectsTest {
    @Test fun disablingAuroraKeepsButterfliesAndOtherEffects() {
        val mask = TreeEffect.ALL and TreeEffect.AURORA.bit.inv()
        assertFalse(TreeEffect.AURORA.visible(mask, 5))
        assertTrue(TreeEffect.BUTTERFLIES.visible(mask, 5))
        assertTrue(TreeEffect.SUNLIGHT.visible(mask, 5))
    }
    @Test fun selectingOneEffectDoesNotEnableEarlierRewards() {
        val mask = TreeEffect.AURORA.bit
        assertTrue(TreeEffect.AURORA.visible(mask, 5))
        assertFalse(TreeEffect.BUTTERFLIES.visible(mask, 5))
        assertFalse(TreeEffect.SUNLIGHT.visible(mask, 5))
    }
    @Test fun selectionCannotBypassRewardsOrWilt() {
        assertFalse(TreeEffect.AURORA.visible(TreeEffect.ALL, 4))
        assertTrue(TreeEffect.AURORA.visible(TreeEffect.ALL, 5))
        for (effect in TreeEffect.entries) {
            assertFalse(effect.visible(0, 5))
            assertFalse(effect.visible(TreeEffect.ALL, 5, 1))
        }
    }
    @Test fun preferencesAreSeparatePerTreeAndRetainedAfterHistoryEdits() {
        val original = TreeSkinInventory().earn(100)
        val changed = original.setEffects(TreeSkin.MAPLE, TreeEffect.BUTTERFLIES.bit).select(TreeSkin.MAPLE).earn(0)
        assertEquals(TreeEffect.BUTTERFLIES.bit, changed.effectsFor(TreeSkin.MAPLE))
        assertEquals(TreeEffect.ALL, changed.effectsFor(TreeSkin.GARDEN))
        assertEquals(TreeEffect.ALL, changed.effectsFor(TreeSkin.PINE))
        assertEquals(TreeSkin.MAPLE, changed.selected)
        assertEquals(TreeEffect.ALL, original.effectsFor(TreeSkin.MAPLE))
    }
    @Test fun lockedTreesCannotSaveEffectsAndUnknownBitsAreRemoved() {
        val locked = TreeSkinInventory()
        assertEquals(locked, locked.setEffects(TreeSkin.PINE, 0))
        assertEquals(TreeEffect.ALL, locked.setEffects(TreeSkin.GARDEN, -1).effectsFor(TreeSkin.GARDEN))
    }
}
