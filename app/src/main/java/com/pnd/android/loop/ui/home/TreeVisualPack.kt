package com.pnd.android.loop.ui.home

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

internal data class TreePackImage(val path: String, val resource: String)

internal data class TreeRiveDefinition(
    val path: String,
    val artboard: String,
    val stateMachine: String,
    val stagePaths: List<String> = emptyList(),
    val imageSlots: List<String> = listOf("tree") + TreePackDefinition.effectKeys,
) {
    fun pathForStage(stage: Int): String = stagePaths.getOrNull(stage.coerceIn(1, 10) - 1) ?: path
}

/** Presentation only: earned stages, rewards and growth rules remain owned by the app. */
internal data class TreePackDefinition(
    val images: Map<String, TreePackImage>,
    val trees: List<List<String>>,
    val rive: TreeRiveDefinition,
) {
    fun treeKey(stage: Int, wilt: Int): String = trees[stage.coerceIn(1, 10) - 1][wilt.coerceIn(0, 3)]

    companion object {
        val effectKeys = listOf("sunlight", "firefly", "flowers", "petal", "butterfly", "aurora")

        private val mapper = ObjectMapper()

        fun parse(json: String): TreePackDefinition {
            val root = mapper.readTree(json)
            require(root.path("schemaVersion").asInt() == 1) { "Unsupported tree pack schema" }
            val images = root.path("images").fields().asSequence().associate { (key, value) ->
                key to TreePackImage(
                    safePath(value.path("path").asText()),
                    value.path("resource").asText().also {
                        require(it.matches(Regex("[a-z][a-z0-9_]*"))) { "Invalid drawable name" }
                    },
                )
            }
            val trees = root.path("trees").map { row -> row.map { it.asText() } }
            require(trees.size == 10 && trees.all { it.size == 4 }) { "Expected 10 stages and 4 conditions" }
            val rive = root.path("rive")
            val slots = if (rive.has("imageSlots")) rive.path("imageSlots").map { it.asText() }
                else listOf("tree") + effectKeys
            require(slots.isNotEmpty() && slots.first() == "tree" && slots.distinct().size == slots.size &&
                slots.drop(1).all { it in effectKeys }) { "Invalid Rive image slots" }
            require((trees.flatten() + slots.drop(1)).all { it in images }) { "Missing tree pack image" }
            val stagePaths = rive.path("stagePaths").map { safePath(it.asText()) }
            require(stagePaths.isEmpty() || stagePaths.size == 10) { "Expected 10 Rive stage files" }
            return TreePackDefinition(images, trees, TreeRiveDefinition(
                safePath(rive.path("path").asText()),
                rive.path("artboard").asText().also { require(it.isNotBlank()) },
                rive.path("stateMachine").asText().also { require(it.isNotBlank()) },
                stagePaths,
                slots,
            ))
        }

        private fun safePath(path: String): String {
            require(path.isNotBlank() && !path.contains('\\') && !path.contains(':') &&
                path.split('/').all { it.isNotEmpty() && it != "." && it != ".." }) {
                "Pack paths must be relative to the pack directory"
            }
            return path
        }
    }
}

/** A pack may override individual PNGs; absent overrides use the original bundled artwork. */
internal class TreeVisualPack(private val context: Context, val definition: TreePackDefinition,
    val skin: TreeSkin = TreeSkin.GARDEN) {
    private val directory = "tree-packs/${skin.packId}"

    fun openImage(key: String): InputStream {
        val image = definition.images.getValue(key)
        return try {
            context.assets.open("$directory/${image.path}")
        } catch (_: java.io.IOException) {
            openBundledImage(key)
        }
    }

    fun openBundledImage(key: String): InputStream {
        val image = definition.images.getValue(key)
        val id = context.resources.getIdentifier(image.resource, "drawable", context.packageName)
        require(id != 0) { "Missing tree pack drawable: ${image.resource}" }
        return context.resources.openRawResource(id)
    }

    fun riveBytesOrNull(stage: Int = 1): ByteArray? = try {
        context.assets.open("$directory/${definition.rive.pathForStage(stage)}").use { it.readBytes() }
    } catch (_: java.io.IOException) {
        null
    }

    companion object {
        // pack.json is static per skin, so parse it once instead of on every composition of every tree.
        private val definitions = ConcurrentHashMap<TreeSkin, TreePackDefinition>()

        fun load(context: Context, skin: TreeSkin = TreeSkin.GARDEN): TreeVisualPack {
            val appContext = context.applicationContext
            val definition = definitions[skin] ?: run {
                val json = appContext.assets.open("tree-packs/${skin.packId}/pack.json")
                    .bufferedReader().use { it.readText() }
                TreePackDefinition.parse(json).also { definitions[skin] = it }
            }
            return TreeVisualPack(appContext, definition, skin)
        }
    }
}

@Composable
internal fun rememberTreeVisualPack(skin: TreeSkin? = null): TreeVisualPack {
    val context = LocalContext.current.applicationContext
    // An explicit skin (shop cards, previews) must not subscribe to the inventory at all.
    val selected = skin ?: rememberTreeSkinInventory().selected
    return remember(context, selected) { TreeVisualPack.load(context, selected) }
}

/**
 * Decoded tree art is shared by the Home card, the guide and the shop. Keeping the bitmaps in a
 * bounded cache means re-entering a screen reuses them instead of decoding PNGs on the main thread.
 */
private object TreeBitmapCache {
    private const val MAX_CACHE_KB = 24 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(MAX_CACHE_KB) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4 / 1024
    }

    fun getOrDecode(key: String, decode: () -> ImageBitmap): ImageBitmap {
        synchronized(cache) { cache.get(key) }?.let { return it }
        val bitmap = decode()
        synchronized(cache) { cache.put(key, bitmap) }
        return bitmap
    }
}

@Composable
internal fun rememberTreePackBitmap(pack: TreeVisualPack, key: String, sampleSize: Int = 1): ImageBitmap {
    val cacheKey = "${pack.skin.packId}/$key/$sampleSize"
    return remember(cacheKey) {
        TreeBitmapCache.getOrDecode(cacheKey) {
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = pack.openImage(key).use { BitmapFactory.decodeStream(it, null, options) }
                ?: pack.openBundledImage(key).use { BitmapFactory.decodeStream(it, null, options) }
            requireNotNull(bitmap) { "Cannot decode tree pack image: $key" }.asImageBitmap()
        }
    }
}
