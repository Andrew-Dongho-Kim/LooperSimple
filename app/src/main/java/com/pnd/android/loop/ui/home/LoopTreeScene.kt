package com.pnd.android.loop.ui.home

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.graphics.Paint
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
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
    val sunlight = if (level >= 1) effectBitmap(R.drawable.loop_toy_effect_sunlight) else null
    val firefly = if (level >= 2) effectBitmap(R.drawable.loop_toy_effect_firefly) else null
    val flowers = if (level >= 3) effectBitmap(R.drawable.loop_toy_effect_flowers) else null
    val petal = if (level >= 3) effectBitmap(R.drawable.loop_toy_effect_petal) else null
    val butterfly = if (level >= 4) effectBitmap(R.drawable.loop_toy_effect_butterfly) else null
    val aurora = if (level >= 5) effectBitmap(R.drawable.loop_toy_effect_aurora) else null
    val mesh = remember { FloatArray((16 + 1) * (8 + 1) * 2) }
    val meshPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val sunlightMask = remember {
        Brush.radialGradient(0f to Color.Black, 0.50f to Color.Black, 1f to Color.Transparent)
    }
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        androidx.compose.foundation.layout.Box(Modifier.size(side)) {
            Canvas(Modifier.fillMaxSize().softAtmosphereEdges()) {
                val seconds = time.value
                sunlight?.let {
                    // Feather only the sunbeams. Aurora must remain visible around the outer crown.
                    val canvas = drawContext.canvas.nativeCanvas
                    val layer = canvas.saveLayer(0f, 0f, size.width, size.height, null)
                    rotate(wave(seconds, 8f) * 5f, Offset.Zero) {
                        translate(wave(seconds, 12f) * size.width * 0.045f,
                            wave(seconds + 2f, 8f) * size.height * 0.025f) {
                            drawArt(it, alpha = (if (level >= 5) 0.30f else 0.54f) + wave(seconds, 6f) * 0.20f)
                        }
                    }
                    drawRect(sunlightMask, blendMode = BlendMode.DstIn)
                    canvas.restoreToCount(layer)
                    drawPollen(seconds)
                }
                aurora?.let {
                    // Two translucent painted layers drift at different speeds behind the crown.
                    val drift = wave(seconds, 8f)
                    drawLivingArt(it, seconds, mesh, meshPaint, flowers = false,
                        alpha = 0.65f + drift * 0.16f)
                    translate(left = -drift * size.width * 0.06f, top = size.height * 0.02f) {
                        drawLivingArt(it, seconds + 4f, mesh, meshPaint, flowers = false,
                            alpha = 0.22f - drift * 0.08f)
                    }
                }
                firefly?.let { drawFireflies(it, seconds, level, behind = true) }
            }
            Image(painterResource(tree), description, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                val seconds = time.value
                flowers?.let {
                    // Anchor the breeze at the soil, so the flower bed never floats off the mound.
                    val side = size.width * 0.82f
                    translate((size.width - side) / 2, size.height - side) {
                        drawLivingArt(it, seconds, mesh, meshPaint, flowers = true, side = side)
                    }
                    petal?.let { sprite -> drawPetals(sprite, seconds) }
                }
                firefly?.let { drawFireflies(it, seconds, level, behind = false) }
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

/** Keep sculpted flowers grounded; only the translucent aurora uses a wider travelling wave. */
private fun DrawScope.drawLivingArt(
    bitmap: ImageBitmap, seconds: Float, vertices: FloatArray, paint: Paint,
    flowers: Boolean, side: Float = size.width, alpha: Float = 1f,
) {
    var index = 0
    for (row in 0..8) for (column in 0..16) {
        val u = column / 16f
        val v = row / 8f
        // Root pixels stay planted; each stem bends with a different travelling breeze.
        val weight = if (flowers) ((0.94f - v) / 0.32f).coerceIn(0f, 1f) else 1f
        val phase = seconds + u * (if (flowers) 3f else 5f)
        val dx = if (flowers) wave(phase, 4f) * 0.018f else
            wave(phase + v * 2f, 8f) * 0.055f
        val dy = if (flowers) wave(phase, 6f) * 0.005f else
            wave(phase + v * 3f, 12f) * 0.045f
        vertices[index++] = side * (u + dx * weight)
        vertices[index++] = side * (v + dy * weight)
    }
    paint.alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
    drawContext.canvas.nativeCanvas.drawBitmapMesh(bitmap.asAndroidBitmap(), 16, 8,
        vertices, 0, null, 0, paint)
}

private fun DrawScope.drawPollen(seconds: Float) {
    repeat(9) { i ->
        val progress = ((seconds + i * 1.73f) % 12f) / 12f
        val center = Offset(size.width * (0.06f + progress * 0.70f + wave(seconds + i, 6f) * 0.025f),
            size.height * (0.10f + (i % 3) * 0.15f + progress * 0.25f))
        val alpha = sin(progress * PI).toFloat() * 0.65f
        drawCircle(Color(0xFFFFE8A9).copy(alpha = alpha), size.width * 0.003f, center)
    }
}

private fun DrawScope.drawPetals(bitmap: ImageBitmap, seconds: Float) {
    repeat(6) { i ->
        val period = if (i % 2 == 0) 8f else 12f
        val progress = ((seconds + i * 2.3f) % period) / period
        val center = Offset(size.width * (0.18f + i * 0.11f + progress * 0.16f + wave(seconds + i, 4f) * 0.045f),
            size.height * (0.80f - sin(progress * PI).toFloat() * 0.27f + progress * 0.08f))
        val alpha = sin(progress * PI).toFloat() * 0.78f
        translate(center.x, center.y) {
            rotate(progress * 270f + i * 35f, Offset.Zero) {
                scale(0.65f + abs(wave(seconds + i, 2f)) * 0.35f, 1f, Offset.Zero) {
                    val side = size.width * 0.045f
                    drawArt(bitmap, side, Offset(-side / 2, -side / 2), alpha)
                }
            }
        }
    }
}

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

private fun DrawScope.drawFireflies(bitmap: ImageBitmap, seconds: Float, level: Int, behind: Boolean) {
    // Sparse, asynchronous paths and soft light cores; higher rewards retain a quieter lower layer.
    val count = if (level >= 4) 5 else 7
    repeat(count) { index ->
        if ((index % 3 == 0) != behind) return@repeat
        val period = if (index % 2 == 0) 8f else 12f
        val t = seconds + index * 2.17f
        val x = size.width * (0.17f + (index * 37 % 64) / 100f + wave(t, period) * 0.075f
            + wave(t + 2f, 4f) * 0.018f)
        val y = size.height * (0.24f + (index * 19 % 55) / 100f + wave(t + 3f, period) * 0.09f)
        val center = Offset(x, y)
        val glow = 0.32f + (wave(t, if (index % 2 == 0) 6f else 8f) + 1f) * 0.28f
        val radius = size.width * 0.045f
        drawCircle(Brush.radialGradient(listOf(
            Color(0xFFFFE6A0).copy(alpha = glow * 0.60f),
            Color(0xFFFFD46C).copy(alpha = glow * 0.12f), Color.Transparent), center, radius),
            radius, center)
        val side = size.width * 0.065f
        rotate(wave(t + 1f, period) * 18f, center) {
            drawArt(bitmap, side, center - Offset(side / 2, side / 2), 0.65f + glow * 0.35f)
        }
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
        val side = size.width * (if (index == 0) 0.16f else 0.14f)
        val flap = abs(wave(t, 0.8f))
        val wingWidth = 0.40f + 0.60f * (if (flight < 0.08f) 0.85f else flap)
        rotate(wave(t + 2f, period) * 14f, center) {
            // Pin the body: each wing hinges at its inner edge, preserving the toy's proportions.
            drawButterflyWings(bitmap, side, center, wingWidth)
        }
    }
}

private fun DrawScope.drawButterflyWings(bitmap: ImageBitmap, side: Float, center: Offset, flap: Float) {
    val bodyLeft = (bitmap.width * 0.42f).roundToInt()
    val bodyRight = (bitmap.width * 0.58f).roundToInt()
    fun part(left: Int, right: Int, x: Float, width: Float) {
        translate(x, center.y - side / 2) {
            drawImage(bitmap, srcOffset = IntOffset(left, 0), srcSize = IntSize(right - left, bitmap.height),
                dstOffset = IntOffset.Zero, dstSize = IntSize(width.roundToInt().coerceAtLeast(1), side.roundToInt()),
                filterQuality = FilterQuality.High)
        }
    }
    val leftWidth = side * bodyLeft / bitmap.width * flap
    val rightWidth = side * (bitmap.width - bodyRight) / bitmap.width * flap
    part(0, bodyLeft, center.x - side * 0.08f - leftWidth, leftWidth)
    part(bodyRight, bitmap.width, center.x + side * 0.08f, rightWidth)
    part(bodyLeft, bodyRight, center.x - side * 0.08f, side * 0.16f)
}
