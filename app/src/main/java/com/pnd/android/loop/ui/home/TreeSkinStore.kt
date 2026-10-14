package com.pnd.android.loop.ui.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pnd.android.loop.R

internal enum class TreeSkin(val packId: String, val unlockPoints: Int, val title: Int, val description: Int) {
    GARDEN("default", 0, R.string.tree_skin_garden, R.string.tree_skin_garden_description),
    MAPLE("maple", 50, R.string.tree_skin_maple, R.string.tree_skin_maple_description),
    PINE("pine", 100, R.string.tree_skin_pine, R.string.tree_skin_pine_description);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.packId == id } ?: GARDEN
    }
}

internal data class TreeSkinInventory(
    val selected: TreeSkin = TreeSkin.GARDEN,
    val unlocked: Set<TreeSkin> = setOf(TreeSkin.GARDEN),
) {
    fun earn(points: Int) = copy(unlocked = unlocked + TreeSkin.entries.filter { points >= it.unlockPoints })
    fun select(skin: TreeSkin) = if (skin in unlocked) copy(selected = skin) else this
}

/** Cosmetic unlocks never debit the garden ledger, and survive later record edits. */
internal class TreeSkinStore(context: Context) {
    val preferences: SharedPreferences = context.applicationContext.getSharedPreferences("tree_skins", Context.MODE_PRIVATE)

    fun read(): TreeSkinInventory {
        val owned = preferences.getStringSet("unlocked", emptySet()).orEmpty().mapNotNull { id ->
            TreeSkin.entries.firstOrNull { it.packId == id }
        }.toSet() + TreeSkin.GARDEN
        val selection = TreeSkin.fromId(preferences.getString("selected", null)).takeIf { it in owned } ?: TreeSkin.GARDEN
        return TreeSkinInventory(selection, owned)
    }

    fun earn(points: Int) = save(read().earn(points))
    fun select(skin: TreeSkin) = save(read().select(skin))

    private fun save(inventory: TreeSkinInventory) {
        if (inventory == read()) return
        preferences.edit().putStringSet("unlocked", inventory.unlocked.map { it.packId }.toSet())
            .putString("selected", inventory.selected.packId).apply()
    }
}

@Composable
internal fun rememberTreeSkinStore(): TreeSkinStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { TreeSkinStore(context) }
}

@Composable
internal fun rememberTreeSkinInventory(): TreeSkinInventory {
    val store = rememberTreeSkinStore()
    var inventory by remember(store) { mutableStateOf(store.read()) }
    DisposableEffect(store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> inventory = store.read() }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return inventory
}

internal fun treeSkinRewardNames(skin: TreeSkin) = if (skin == TreeSkin.PINE) listOf(
    R.string.tree_pine_reward_light, R.string.tree_pine_reward_dew, R.string.tree_pine_reward_cones,
    R.string.tree_pine_reward_pollen, R.string.tree_pine_reward_halo,
) else if (skin == TreeSkin.MAPLE) listOf(
    R.string.tree_maple_reward_light, R.string.tree_maple_reward_motes, R.string.tree_maple_reward_mushrooms,
    R.string.tree_maple_reward_samara, R.string.tree_maple_reward_halo,
) else rewardNames
