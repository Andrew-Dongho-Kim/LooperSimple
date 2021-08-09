package com.pnd.android.loop.ui.animation

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.whenStarted
import kotlin.math.pow


/**
 * Returns a [State] holding a local animation time in milliseconds. The value always starts
 * at `0L` and stops updating when the call leaves the composition.
 */
@Composable
fun animationTimeMillis(): State<Long> {
    val millisState = remember { mutableStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(true) {
        val startTime = withFrameMillis { it }
        lifecycleOwner.whenStarted {
            while (true) {
                withFrameMillis { frameTime ->
                    millisState.value = frameTime - startTime
                }
            }
        }
    }
    return millisState
}

fun ease(p: Float, g: Float): Float {
    return if (p < 0.5f) {
        0.5f * (2 * p).pow(g)
    } else {
        1 - 0.5f * (2 * (1 - p)).pow(g)
    }
}

fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

/**
 * Linearly interpolates two lists of path nodes to simulate path morphing.
 */
fun lerp(fromPathNodes: List<PathNode>, toPathNodes: List<PathNode>, t: Float): List<PathNode> {
    return fromPathNodes.mapIndexed { i, from ->
        val to = toPathNodes[i]
        if (from is PathNode.MoveTo && to is PathNode.MoveTo) {
            PathNode.MoveTo(
                lerp(from.x, to.x, t),
                lerp(from.y, to.y, t),
            )
        } else if (from is PathNode.CurveTo && to is PathNode.CurveTo) {
            PathNode.CurveTo(
                lerp(from.x1, to.x1, t),
                lerp(from.y1, to.y1, t),
                lerp(from.x2, to.x2, t),
                lerp(from.y2, to.y2, t),
                lerp(from.x3, to.x3, t),
                lerp(from.y3, to.y3, t),
            )
        } else {
            // TODO: support all possible SVG path data types
            throw IllegalStateException("Unsupported SVG PathNode command")
        }
    }
}
