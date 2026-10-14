package com.pnd.android.loop.ui.home

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream

internal data class TreePackImage(val path: String, val resource: String)

internal data class TreeRiveDefinition(
    val path: String,
    val artboard: String,
    val stateMachine: String,
    val stagePaths: List<String> = emptyList(),
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

        fun parse(json: String): TreePackDefinition {
            val root = ObjectMapper().readTree(json)
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
            require((trees.flatten() + effectKeys).all { it in images }) { "Missing tree pack image" }
            val rive = root.path("rive")
            val stagePaths = rive.path("stagePaths").map { safePath(it.asText()) }
            require(stagePaths.isEmpty() || stagePaths.size == 10) { "Expected 10 Rive stage files" }
            return TreePackDefinition(images, trees, TreeRiveDefinition(
                safePath(rive.path("path").asText()),
                rive.path("artboard").asText().also { require(it.isNotBlank()) },
                rive.path("stateMachine").asText().also { require(it.isNotBlank()) },
                stagePaths,
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
internal class TreeVisualPack(private val context: Context, val definition: TreePackDefinition) {
    private val directory = "tree-packs/default"

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
        fun load(context: Context): TreeVisualPack {
            val appContext = context.applicationContext
            val json = appContext.assets.open("tree-packs/default/pack.json")
                .bufferedReader().use { it.readText() }
            return TreeVisualPack(appContext, TreePackDefinition.parse(json))
        }
    }
}

@Composable
internal fun rememberTreeVisualPack(): TreeVisualPack {
    val context = LocalContext.current.applicationContext
    return remember(context) { TreeVisualPack.load(context) }
}

@Composable
internal fun rememberTreePackBitmap(pack: TreeVisualPack, key: String, sampleSize: Int = 1): ImageBitmap =
    remember(pack, key, sampleSize) {
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = pack.openImage(key).use { BitmapFactory.decodeStream(it, null, options) }
            ?: pack.openBundledImage(key).use { BitmapFactory.decodeStream(it, null, options) }
        requireNotNull(bitmap) { "Cannot decode tree pack image: $key" }.asImageBitmap()
    }
