package com.pnd.android.loop.ui.home

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pnd.android.loop.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Every layer shares the tree's square, including on wide cards and small screens. */
@Composable
internal fun LoopTreeScene(tree: Int, rewardLevel: Int, description: String, modifier: Modifier) {
    val level = rewardLevel.coerceIn(0, 5)
    val time = rememberGardenTime(level > 0)
    // Load only unlocked art. Downsample effect overlays rather than decoding four full-size PNGs.
    val sunlight = if (level >= 1) effectBitmap(R.drawable.loop_effect_sunlight) else null
    val flowers = if (level >= 3) effectBitmap(R.drawable.loop_effect_flowers) else null
    val butterfly = if (level >= 4) effectBitmap(R.drawable.loop_effect_butterfly) else null
    val aurora = if (level >= 5) effectBitmap(R.drawable.loop_effect_aurora) else null
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        androidx.compose.foundation.layout.Box(Modifier.size(side)) {
            Canvas(Modifier.fillMaxSize().softAtmosphereEdges()) {
                val seconds = time.value
                sunlight?.let {
                    val breath = wave(seconds, 8f)
                    drawArt(it, alpha = (if (level >= 5) 0.30f else 0.54f) + breath * 0.12f)
                }
                aurora?.let {
                    // Two translucent painted layers drift at different speeds behind the crown.
                    val drift = wave(seconds, 16f)
                    translate(left = drift * size.width * 0.025f,
                        top = wave(seconds + 4f, 16f) * size.height * 0.012f) {
                        scale(1.06f, 1f, pivot = Offset(size.width / 2, 0f)) {
                            drawArt(it, alpha = 0.68f + drift * 0.08f)
                        }
                    }
                    translate(left = -drift * size.width * 0.035f, top = size.height * 0.025f) {
                        drawArt(it, alpha = 0.17f - drift * 0.04f)
                    }
                }
                if (level >= 2) drawFireflies(seconds, level, behind = true)
            }
            Image(painterResource(tree), description, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                val seconds = time.value
                flowers?.let {
                    // Anchor the breeze at the soil, so the flower bed never floats off the mound.
                    val width = size.width * 0.82f
                    rotate(wave(seconds, 6f) * 0.65f, Offset(size.width / 2, size.height * 0.94f)) {
                        drawArt(it, width, Offset((size.width - width) / 2, size.height - width))
                    }
                }
                if (level >= 2) drawFireflies(seconds, level, behind = false)
                butterfly?.let { drawButterflies(it, seconds) }
            }
        }
    }
}

/** Feather the light layers into the card instead of exposing their square PNG boundaries. */
private fun Modifier.softAtmosphereEdges(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithCache {
    val horizontal = Brush.horizontalGradient(0f to Color.Transparent,
        0.08f to Color.Black, 0.92f to Color.Black, 1f to Color.Transparent)
    val vertical = Brush.verticalGradient(0f to Color.Transparent,
        0.06f to Color.Black, 0.92f to Color.Black, 1f to Color.Transparent)
    onDrawWithContent {
        drawContent()
        drawRect(horizontal, blendMode = BlendMode.DstIn)
        drawRect(vertical, blendMode = BlendMode.DstIn)
    }
}

@Composable
private fun effectBitmap(id: Int): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember(resources, id) {
        BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply {
            inSampleSize = 2
        }).asImageBitmap()
    }
}

/** Stop clocks off screen, and provide a composed static scene when system animations are disabled. */
@Composable
private fun rememberGardenTime(hasReward: Boolean): State<Float> {
    val owner = LocalLifecycleOwner.current
    var active by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var animationsEnabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            animationsEnabled = ValueAnimator.areAnimatorsEnabled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (!hasReward || !active || !animationsEnabled) return remember { mutableStateOf(3f) }
    val transition = rememberInfiniteTransition(label = "garden atmosphere")
    // 48 seconds is a whole number of all motion periods, keeping the loop seam invisible.
    return transition.animateFloat(0f, 48f,
        infiniteRepeatable(tween(48000, easing = LinearEasing), RepeatMode.Restart),
        label = "garden time")
}

private fun wave(seconds: Float, period: Float): Float = sin(seconds * (2 * PI).toFloat() / period)

private fun DrawScope.drawArt(
    bitmap: ImageBitmap,
    side: Float = size.width,
    position: Offset = Offset.Zero,
    alpha: Float = 1f,
) {
    translate(position.x, position.y) {
        drawImage(bitmap, dstOffset = IntOffset.Zero,
            dstSize = IntSize(side.roundToInt().coerceAtLeast(1), side.roundToInt().coerceAtLeast(1)),
            alpha = alpha.coerceIn(0f, 1f), filterQuality = FilterQuality.High)
    }
}

private fun DrawScope.drawFireflies(seconds: Float, level: Int, behind: Boolean) {
    // Sparse, asynchronous paths and soft light cores; higher rewards retain a quieter lower layer.
    val count = if (level >= 4) 5 else 7
    repeat(count) { index ->
        if ((index % 3 == 0) != behind) return@repeat
        val period = if (index % 2 == 0) 12f else 16f
        val t = seconds + index * 2.17f
        val x = size.width * (0.13f + (index * 37 % 72) / 100f + wave(t, period) * 0.025f)
        val y = size.height * (0.24f + (index * 19 % 60) / 100f + wave(t + 3f, period) * 0.035f)
        val center = Offset(x, y)
        val glow = 0.32f + (wave(t, if (index % 2 == 0) 6f else 8f) + 1f) * 0.28f
        val radius = size.width * 0.026f
        drawCircle(Brush.radialGradient(listOf(
            Color(0xFFFFE6A0).copy(alpha = glow * 0.60f),
            Color(0xFFFFD46C).copy(alpha = glow * 0.12f), Color.Transparent), center, radius),
            radius, center)
        drawCircle(Color(0xFFFFF5CD).copy(alpha = glow), size.width * 0.005f, center)
        drawCircle(Color.White.copy(alpha = glow * 0.70f), size.width * 0.002f, center)
    }
}

private fun DrawScope.drawButterflies(bitmap: ImageBitmap, seconds: Float) {
    repeat(2) { index ->
        val t = seconds + index * 8f
        val period = if (index == 0) 12f else 16f
        // Eased visits slow near their perch, then trace a small curved flight.
        val flight = (1f - cos(t * (2 * PI).toFloat() / period)) / 2f
        val center = Offset(
            size.width * (if (index == 0) 0.25f else 0.76f) + wave(t, period) * size.width * 0.065f * flight,
            size.height * (if (index == 0) 0.61f else 0.40f) - flight * size.height * 0.10f,
        )
        val side = size.width * (if (index == 0) 0.135f else 0.115f)
        val flap = abs(wave(t, 0.8f))
        val wingWidth = 0.38f + 0.62f * (if (flight < 0.08f) 0.85f else flap)
        rotate(wave(t + 2f, period) * 14f, center) {
            scale(wingWidth, 1f, pivot = center) {
                drawArt(bitmap, side, center - Offset(side / 2, side / 2))
            }
        }
    }
}
