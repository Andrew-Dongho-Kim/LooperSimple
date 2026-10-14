package com.pnd.android.loop.ui.home

import org.junit.Assert.*
import org.junit.Test

class TreeSkinInventoryTest {
    @Test fun mapleUnlocksAtFiftyWithoutChangingTheOriginalInventory() {
        val below = TreeSkinInventory().earn(49)
        assertEquals(setOf(TreeSkin.GARDEN), below.unlocked)
        assertEquals(setOf(TreeSkin.GARDEN, TreeSkin.MAPLE), below.earn(50).unlocked)
        assertEquals(setOf(TreeSkin.GARDEN), below.unlocked)
    }

    @Test fun pineRequiresOneHundredAndRetainsItsUnlockAfterHistoryEdits() {
        val below = TreeSkinInventory().earn(99)
        assertFalse(TreeSkin.PINE in below.unlocked)
        assertEquals(below, below.select(TreeSkin.PINE))
        val selected = below.earn(100).select(TreeSkin.PINE).earn(0)
        assertTrue(TreeSkin.PINE in selected.unlocked)
        assertTrue(TreeSkin.MAPLE in selected.unlocked)
        assertEquals(TreeSkin.PINE, selected.selected)
    }

    @Test fun lockedSkinCannotBeEquipped() {
        val locked = TreeSkinInventory().earn(49)
        assertEquals(locked, locked.select(TreeSkin.MAPLE))
    }

    @Test fun unlockedSkinAndSelectionSurviveEditedHistory() {
        val chosen = TreeSkinInventory().earn(50).select(TreeSkin.MAPLE)
        val edited = chosen.earn(0)
        assertTrue(TreeSkin.MAPLE in edited.unlocked)
        assertEquals(TreeSkin.MAPLE, edited.selected)
    }

    @Test fun unknownSelectionFallsBackToTheGarden() {
        assertEquals(TreeSkin.GARDEN, TreeSkin.fromId("unknown"))
        assertEquals(TreeSkin.GARDEN, TreeSkin.fromId("../other-pack"))
        assertEquals(TreeSkin.GARDEN, TreeSkin.fromId(null))
    }
}
