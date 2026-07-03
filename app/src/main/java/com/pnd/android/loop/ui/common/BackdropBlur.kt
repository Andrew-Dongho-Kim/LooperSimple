package com.pnd.android.loop.ui.common

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.surface

/** Blur radius applied behind the floating surfaces — a frosted-glass "50%" look. */
private val BackdropBlurRadius = 12.dp

/** Surface tint over the blurred backdrop. Kept light (10%) so the blur itself does the work. */
private const val BlurredTintAlpha = 0.1f

/** Surface tint used when real blur is unavailable (API < 31); more opaque so it still reads. */
private const val FallbackTintAlpha = 0.72f

/** Shadow cast by a fully collapsed floating surface, lifting it off the content behind it. */
private val FloatingElevation = 6.dp

/** True only where a real backdrop blur (RenderEffect) is available. */
val supportsBackdropBlur: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Shared handle that lets floating surfaces paint a blurred copy of the content scrolling behind
 * them. The content records itself into [layer] via [Modifier.backdropSource]; each floating
 * surface then re-draws that same layer through a blur, offset so the right region lines up.
 */
@Stable
class BackdropState(val layer: GraphicsLayer) {
    /** Top-left of the captured content in root coordinates, used to align the re-drawn layer. */
    var originInRoot by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberBackdropState(): BackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { BackdropState(layer) }
}

/**
 * Marks this content as the backdrop: it is recorded into [state]'s layer (and still drawn
 * normally) so floating surfaces above it can sample a blurred version.
 */
fun Modifier.backdropSource(state: BackdropState): Modifier = this
    .onGloballyPositioned { state.originInRoot = it.positionInRoot() }
    .drawWithContent {
        // Record into the shared layer, then draw it as usual so the content still shows.
        state.layer.record { this@drawWithContent.drawContent() }
        drawLayer(state.layer)
    }

/**
 * A rounded surface that floats above the scrolling list. On API 31+ it shows a real blurred
 * copy of the content behind it (via [backdrop]); otherwise it falls back to translucent white.
 * Both fade in with [progress] so nothing shows while the header is expanded.
 */
@Composable
fun FloatingSurface(
    progress: Float,
    shape: Shape,
    backdrop: BackdropState?,
    modifier: Modifier = Modifier,
    contentHorizontalPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                // Shadow grows in with the collapse so the pill lifts off the content behind it.
                .shadow(elevation = FloatingElevation * progress, shape = shape)
                .floatingSurfaceBackground(backdrop = backdrop, shape = shape, progress = progress)
        )
        // Inset the content so it doesn't sit flush against the pill's rounded edges.
        Box(Modifier.padding(horizontal = contentHorizontalPadding)) {
            content()
        }
    }
}

/**
 * The frosted-glass fill shared by every floating surface (the header pills and the add-loop
 * button): a real blurred copy of the content behind it on API 31+ (via [backdrop]), or a
 * translucent tint where blur is unavailable. [progress] fades it in — `1f` for a surface that is
 * always shown, or the collapse fraction for one that grows in with the header.
 */
@Composable
fun Modifier.floatingSurfaceBackground(
    backdrop: BackdropState?,
    shape: Shape,
    progress: Float = 1f,
): Modifier {
    // Theme surface (white in light, near-black in dark) keeps the surface from clashing in dark mode.
    val tint = AppColor.surface
    return if (backdrop != null) {
        blurredBackdrop(backdrop, progress, shape, tint)
    } else {
        background(color = tint.copy(alpha = FallbackTintAlpha * progress), shape = shape)
    }
}

/**
 * Draws the blurred backdrop plus a white tint, clipped to [shape] and faded by [progress].
 * The blur is applied by this node's own graphics layer, so only the backdrop is blurred — the
 * surface's foreground content (a sibling) stays crisp.
 */
private fun Modifier.blurredBackdrop(
    backdrop: BackdropState,
    progress: Float,
    shape: Shape,
    tint: Color,
): Modifier = composed {
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }
    val radiusPx = with(LocalDensity.current) { BackdropBlurRadius.toPx() }

    this
        .onGloballyPositioned { positionInRoot = it.positionInRoot() }
        .graphicsLayer {
            alpha = progress.coerceAtLeast(0.7f)
            clip = true
            this.shape = shape
            renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
        }
        .drawBehind {
            // Shift the captured layer so the region beneath this surface lands at its origin.
            val offset = backdrop.originInRoot - positionInRoot
            translate(offset.x, offset.y) {
                drawLayer(backdrop.layer)
            }
        }
        .background(color = tint.copy(alpha = BlurredTintAlpha), shape = shape)
}
