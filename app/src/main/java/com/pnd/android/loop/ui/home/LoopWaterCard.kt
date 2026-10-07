package com.pnd.android.loop.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import kotlin.math.roundToInt

@Composable
internal fun waterCardColor(): Color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    Color(0xFF172A33) else Color(0xFFEDF6FC)

@Composable
fun LoopWaterCard(rates: LoopRates) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val blue = if (dark) Color(0xFF84D5F5) else Color(0xFF187BA4)
    val subtle = AppColor.onSurface.copy(alpha = 0.68f)
    val done = rates.counts.done
    val total = rates.counts.total
    val fraction = if (total > 0) done.toFloat() / total else 0f
    val fill by animateFloatAsState(fraction, tween(650), label = "water level")
    val drop = remember { Animatable(1f) }
    var previousDone by remember { mutableIntStateOf(-1) }
    var pouring by remember { mutableStateOf(false) }
    LaunchedEffect(done, total) {
        val increased = previousDone >= 0 && done > previousDone
        previousDone = done
        pouring = false
        if (increased) {
            pouring = true
            drop.snapTo(0f)
            drop.animateTo(1f, tween(850))
            pouring = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.water_title), Modifier.weight(1f),
                style = AppTypography.titleMedium.copy(color = AppColor.onSurface, fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.water_completed, done, total),
                Modifier.clip(RoundedCornerShape(20.dp)).background(blue.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                style = AppTypography.labelMedium.copy(color = blue))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            WaterTank(fill, blue, pouring, drop.value, Modifier.size(128.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.tree_today_water), style = AppTypography.bodySmall.copy(color = subtle))
                Text(stringResource(R.string.tree_water_percent, rates.donePercent ?: 0),
                    style = MaterialTheme.typography.displayMedium.copy(color = blue, fontWeight = FontWeight.Bold))
                Text(stringResource(when {
                    total == 0 -> R.string.tree_no_tasks
                    pouring -> R.string.tree_water_added
                    done == total -> R.string.tree_water_full
                    else -> R.string.tree_water_hint
                }), style = AppTypography.bodySmall.copy(color = subtle))
            }
        }
        Text(stringResource(R.string.water_tree_connection),
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(blue.copy(alpha = 0.06f)).padding(12.dp),
            style = AppTypography.bodySmall.copy(color = blue))
    }
}

@Composable
private fun WaterTank(level: Float, blue: Color, pouring: Boolean, drop: Float, modifier: Modifier) {
    val resources = LocalContext.current.resources
    val shell = remember(resources) {
        BitmapFactory.decodeResource(resources, R.drawable.loop_toy_water_tank,
            BitmapFactory.Options().apply { inSampleSize = 2 }).asImageBitmap()
    }
    Canvas(modifier) {
        // Match the generated jar's inner silhouette; retain a real, data-driven water level.
        val left = size.width * 0.085f
        val right = size.width * 0.915f
        val top = size.height * 0.29f
        val bottom = size.height * 0.84f
        val radius = size.width * 0.20f
        val fillHeight = (bottom - top) * level.coerceIn(0f, 1f)
        val surface = bottom - fillHeight
        val path = Path().apply { addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(radius))) }
        clipPath(path) {
            if (fillHeight > 0f) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF9DE5F8), Color(0xFF42ADD4)),
                    startY = surface, endY = bottom), Offset(left, surface), Size(right - left, fillHeight))
                drawOval(Color(0xFFCAFAFF), Offset(left, surface - 2.dp.toPx()),
                    Size(right - left, 4.dp.toPx()))
                if (pouring) {
                    val ripple = drop.coerceIn(0f, 1f) * size.width * 0.26f
                    drawOval(Color.White.copy(alpha = 1f - drop),
                        Offset(size.width / 2 - ripple, surface - ripple * 0.18f),
                        Size(ripple * 2, ripple * 0.36f), style = Stroke(1.dp.toPx()))
                }
            }
        }
        drawImage(shell, dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.High)
        if (pouring) {
            val y = size.height * 0.025f + drop * (surface - size.height * 0.025f)
            val center = Offset(size.width / 2, y)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFD5FBFF), blue),
                center - Offset(1.dp.toPx(), 1.dp.toPx()), 5.dp.toPx()), 4.dp.toPx(), center,
                alpha = 1f - drop * 0.75f)
        }
    }
}
