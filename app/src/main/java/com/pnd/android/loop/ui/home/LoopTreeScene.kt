package com.pnd.android.loop.ui.home

import android.animation.ValueAnimator
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Every layer shares the tree's square, including on wide cards and small screens. */
@Composable
internal fun NativeTreeScene(stage: Int, rewardLevel: Int, description: String, modifier: Modifier,
    wiltLevel: Int, pack: TreeVisualPack) {
    val wilt = wiltLevel.coerceIn(0, 3)
    val maple = pack.skin == TreeSkin.MAPLE
    val pine = pack.skin == TreeSkin.PINE
    val level = if (wilt == 0) rewardLevel.coerceIn(0, 5) else 0
    val effectAlpha = 1f - wilt * 0.22f
    val time = rememberGardenTime(true)
    val leafPath = remember(maple, pine) {
        Path().apply {
            if (pine) {
                moveTo(0f, 7f); lineTo(-5f,-11f); lineTo(-2f,-7f); lineTo(0f,5f)
                moveTo(0f,7f); lineTo(5f,-11f); lineTo(2f,-7f); lineTo(0f,5f)
            } else if (maple) {
                val points = listOf(-8f to 4f,-6f to 0f,-10f to -2f,-4f to -3f,-6f to -8f,-1f to -5f,
                    0f to -11f,2f to -5f,7f to -8f,5f to -3f,10f to -2f,6f to 0f,8f to 4f,2f to 3f,0f to 7f,-2f to 3f)
                moveTo(points[0].first, points[0].second)
                points.drop(1).forEach { lineTo(it.first, it.second) }
            } else {
            moveTo(-8f, 0f)
            cubicTo(-4f, -6f, 4f, -6f, 8f, 0f)
            cubicTo(4f, 6f, -4f, 6f, -8f, 0f)
            }
            close()
        }
    }
    // Load only unlocked art. Downsample effect overlays rather than decoding four full-size PNGs.
    val sunlight = if (level >= 1 && !maple && !pine) rememberTreePackBitmap(pack, "sunlight", 2) else null
    val firefly = if (level >= 2 && !maple && !pine) rememberTreePackBitmap(pack, "firefly", 2) else null
    val flowers = if (level >= 3 && !maple && !pine) rememberTreePackBitmap(pack, "flowers", 2) else null
    val petal = if (level >= 3 && !maple && !pine) rememberTreePackBitmap(pack, "petal", 2) else null
    val butterfly = if (level >= 4 && !maple && !pine) rememberTreePackBitmap(pack, "butterfly", 2) else null
    val aurora = if (level >= 5 && !maple && !pine) rememberTreePackBitmap(pack, "aurora", 2) else null
    val mesh = remember { FloatArray((16 + 1) * (8 + 1) * 2) }
    val meshPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val sunlightMask = remember {
        Brush.radialGradient(0f to Color.Black, 0.50f to Color.Black, 1f to Color.Transparent)
    }
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        androidx.compose.foundation.layout.Box(Modifier.size(side)) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = effectAlpha }.softAtmosphereEdges()) {
                val seconds = time.value
                if (maple) drawMapleAtmosphere(level, seconds)
                if (pine) drawPineAtmosphere(level, seconds)
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
            Image(rememberTreePackBitmap(pack, pack.treeImageKey(stage, wilt)), description, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = effectAlpha }) {
                val seconds = time.value
                if (maple) drawMapleForeground(level, seconds, leafPath)
                if (pine) drawPineForeground(level, seconds, stage)
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
            Canvas(Modifier.fillMaxSize()) {
                // Match the Rive condition effect, including packs without a usable .riv file.
                if (ValueAnimator.areAnimatorsEnabled()) drawConditionLeaves(leafPath, time.value, stage, wilt, maple, pine)
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

private fun DrawScope.drawConditionLeaves(path: Path, seconds: Float, stage: Int, wilt: Int, maple: Boolean, pine: Boolean) {
    if (pine && stage == 1) return // A seed has no needles to shed.
    val falling = wilt > 0
    val count = if (falling) listOf(0, 3, 5, 8)[wilt] else 5
    val total = if (falling) 8 else 5
    val crown = listOf(0.75f, 0.64f, 0.53f, 0.46f, 0.40f, 0.36f, 0.34f, 0.32f, 0.30f, 0.29f)[stage.coerceIn(1, 10)-1]
    val spread = minOf(0.28f, 0.04f + stage.coerceIn(1, 10) * 0.026f)
    val palette = if (falling) listOf(Color(0xFFCCA15B), Color(0xFFB98540), Color(0xFFE0B86D), Color(0xFFA96F35))
        else if (maple) listOf(Color(0xFFF3B74E),Color(0xFFFFCB67),Color(0xFFE97438))
        else listOf(Color(0xFF96C95A), Color(0xFFB3DA71), Color(0xFF70AD4C))
    repeat(count) { i ->
        val progress = (seconds / 12f + i.toFloat() / total) % 1f
        val sourceX = 0.5f + spread * ((i * 37 % 101) / 50f - 1f)
        val sourceY = crown + 0.12f * (i % 3) / 2f
        val drift = 0.035f * sin((2 * PI * progress + i).toFloat()) +
            0.045f * progress * (if (i % 2 == 1) 1f else -1f)
        val center = Offset(size.width * (sourceX + drift),
            size.height * (if (falling) sourceY + (0.89f - sourceY) * progress else sourceY - 0.18f * progress))
        val pulse = sin((PI * progress).toFloat())
        val alpha = pulse * pulse * (if (falling) 0.9f else 0.65f)
        val leafScale = size.width / 512f * (if (falling) 1f else 0.65f)
        translate(center.x, center.y) {
            rotate((progress * 2.4f + 0.4f * sin((2 * PI * progress + i).toFloat())) * (180 / PI).toFloat(), Offset.Zero) {
                scale(leafScale * (0.65f + 0.35f * abs(cos((2 * PI * progress).toFloat()))), leafScale, Offset.Zero) {
                    drawPath(path, palette[i % palette.size].copy(alpha = alpha))
                    drawLine(Color(0xFFE8D6A0).copy(alpha = alpha),
                        if (maple || pine) Offset(0f,-7f) else Offset(-6f,0f),
                        if (maple || pine) Offset(0f,5f) else Offset(6f,0f), 0.7f)
                }
            }
        }
    }
}

private fun DrawScope.drawMapleAtmosphere(level: Int, seconds: Float) {
    if (level >= 1) drawCircle(Brush.radialGradient(listOf(Color(0xFFFFCC7A).copy(alpha = 0.08f + wave(seconds,6f)*0.02f),
        Color.Transparent), Offset(size.width*0.5f,size.height*0.3f), size.width*0.65f), size.width*0.65f,
        Offset(size.width*0.5f,size.height*0.3f))
    if (level >= 2) repeat(6) { i ->
        val t = seconds + i*2.17f
        drawCircle(Color(0xFFFFD57F).copy(alpha = 0.5f + wave(t,6f)*0.3f), size.width*0.0035f,
            Offset(size.width*(0.18f+(i*37%64)/100f+wave(t,8f)*0.06f),
                size.height*(0.24f+(i*19%55)/100f+wave(t+3f,12f)*0.08f)))
    }
    if (level >= 5) repeat(3) { i ->
        val width=size.width*(0.855f+i*0.029f)*(1+wave(seconds,8f)*0.035f)
        val height=size.height*(0.781f+i*0.029f)*(1+wave(seconds,12f)*0.025f)
        drawOval(Color(0xFFFFC965).copy(alpha = listOf(0.3f,0.15f,0.06f)[i]),
            Offset((size.width-width)/2,size.height*0.48f-height/2), androidx.compose.ui.geometry.Size(width,height),
            style=Stroke(size.width*0.004f))
    }
}

private fun DrawScope.drawMapleForeground(level: Int, seconds: Float, leaf: Path) {
    val unit=size.width/512f
    if (level >= 3) {
        repeat(5) { i ->
            translate(size.width*(0.24f+i*0.13f),size.height*(0.89f-(i%2)*0.025f)) {
                drawRect(Color(0xFFF2D5A4),Offset(-2*unit,-2*unit),androidx.compose.ui.geometry.Size(4*unit,9*unit))
                drawOval(Color(0xFFBA673A),Offset(-9*unit,-10*unit),androidx.compose.ui.geometry.Size(18*unit,10*unit))
                drawOval(Color(0xFFE6A25F),Offset(-5*unit,-9*unit),androidx.compose.ui.geometry.Size(10*unit,3*unit))
            }
        }
        repeat(4) { i ->
            val t=seconds+i*2f
            translate(size.width*(0.24f+i*0.13f+wave(t,8f)*0.04f),size.height*(0.72f-wave(t,12f)*0.075f)) {
                rotate(wave(seconds+i,8f)*28f,Offset.Zero) {
                    scale(unit,unit,Offset.Zero) { drawPath(leaf,Color(0xFFF3B74E).copy(alpha=0.25f+wave(seconds+i,12f)*0.23f)) }
                }
            }
        }
    }
    if (level >= 4) repeat(2) { i ->
        val period=if(i==0) 12f else 16f
        val t=seconds+i*8f
        translate(size.width*((if(i==0)0.25f else 0.76f)+wave(t,period)*0.05f),
            size.height*((if(i==0)0.61f else 0.4f)-0.075f*(1-cos((2*PI*t/period).toFloat()))/2)) {
            rotate(wave(t,period)*10f,Offset.Zero) {
                for (side in listOf(-1,1)) rotate(side*31f,Offset(side*4*unit,0f)) {
                    drawOval(Color(0xFFD99E56),Offset((side*11-11.5f)*unit,-8.5f*unit),androidx.compose.ui.geometry.Size(23*unit,7*unit))
                }
                drawOval(Color(0xFFA36C42),Offset(-3.5f*unit,-2.5f*unit),androidx.compose.ui.geometry.Size(7*unit,5*unit))
            }
        }
    }
}

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


private fun DrawScope.drawPineAtmosphere(level: Int, seconds: Float) {
    if (level >= 1) drawCircle(Brush.radialGradient(listOf(Color(0xFFE5EDB1).copy(alpha=0.08f+wave(seconds,6f)*0.02f),
        Color.Transparent),Offset(size.width*0.5f,size.height*0.3f),size.width*0.65f),
        size.width*0.65f,Offset(size.width*0.5f,size.height*0.3f))
    if (level >= 5) repeat(3) { i ->
        val width=size.width*(0.85f+i*0.028f)*(1+wave(seconds,8f)*0.03f)
        val height=size.height*(0.80f+i*0.024f)*(1+wave(seconds,12f)*0.02f)
        drawOval(Color(0xFFA1D6AA).copy(alpha=listOf(0.25f,0.12f,0.05f)[i]),
            Offset((size.width-width)/2,size.height*0.48f-height/2),androidx.compose.ui.geometry.Size(width,height),
            style=Stroke(size.width*0.004f))
    }
}

private fun DrawScope.drawPineForeground(level: Int, seconds: Float, stage: Int) {
    val unit=size.width/512f
    val spread=minOf(0.28f,0.04f+stage*0.026f)
    val crown=listOf(0.75f,0.64f,0.53f,0.46f,0.40f,0.36f,0.34f,0.32f,0.30f,0.29f)[stage.coerceIn(1,10)-1]
    if (level >= 2) repeat(6) { i ->
        val center=Offset(size.width*(0.5f+spread*((i*37%101)/50f-1f)+wave(seconds,8f)*0.006f),
            size.height*(crown+0.07f*(i%3)+wave(seconds,12f)*0.004f))
        val color=Color(0xFFE2FFF0).copy(alpha=0.55f+wave(seconds+i,6f)*0.35f)
        drawCircle(color,2*unit,center)
        drawLine(color,center-Offset(0f,4*unit),center+Offset(0f,4*unit),0.7f*unit)
        drawLine(color,center-Offset(3*unit,0f),center+Offset(3*unit,0f),0.7f*unit)
    }
    if (level >= 3) repeat(3) { i ->
        translate(size.width*(0.30f+i*0.20f),size.height*(0.90f-(i%2)*0.02f)) {
            drawOval(Color(0xFF845537),Offset(-7*unit,-11*unit),androidx.compose.ui.geometry.Size(14*unit,22*unit))
            repeat(4) { row -> for (side in -1..1) {
                drawOval(Color(if(row%2==1)0xFFC18A52 else 0xFFA97244),
                    Offset((side*(if(row in 1..2)4f else 2.5f)-3)*unit,(-9.5f+row*4.5f)*unit),
                    androidx.compose.ui.geometry.Size(6*unit,5*unit))
            } }
        }
    }
    if (level >= 4) repeat(8) { i ->
        drawCircle(Color(0xFFFFDA82).copy(alpha=0.4f+wave(seconds+i,8f)*0.3f),1.1f*unit,
            Offset(size.width*(0.5f+spread*wave(seconds+i*2,12f)),
                size.height*(crown+0.13f+0.09f*wave(seconds+i*2,16f))))
    }
}
