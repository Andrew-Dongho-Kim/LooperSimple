package com.pnd.android.loop.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface

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
            WaterTank(fill, blue, pouring, drop.value, Modifier.width(88.dp).height(140.dp))
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
    Canvas(modifier) {
        val inset = 5.dp.toPx()
        val top = 14.dp.toPx()
        val radius = 16.dp.toPx()
        val tankHeight = size.height - top
        val fillHeight = (tankHeight - inset * 2) * level.coerceIn(0f, 1f)
        drawRoundRect(blue.copy(alpha = 0.06f), Offset(0f, top), Size(size.width, tankHeight), CornerRadius(radius))
        val path = Path().apply { addRoundRect(RoundRect(inset, top + inset, size.width - inset,
            size.height - inset, CornerRadius(radius - inset))) }
        clipPath(path) {
            if (fillHeight > 0f) {
                val surface = size.height - inset - fillHeight
                drawRect(Brush.verticalGradient(listOf(blue.copy(alpha = 0.35f), blue.copy(alpha = 0.8f))),
                    Offset(inset, surface), Size(size.width - inset * 2, fillHeight))
                drawOval(Color.White.copy(alpha = 0.55f), Offset(inset, surface - 3.dp.toPx()),
                    Size(size.width - inset * 2, 6.dp.toPx()))
            }
            repeat(3) { i ->
                val y = top + tankHeight * (0.25f + i * 0.25f)
                drawLine(blue.copy(alpha = 0.25f), Offset(size.width - 17.dp.toPx(), y),
                    Offset(size.width - 8.dp.toPx(), y), 1.dp.toPx())
            }
        }
        drawRoundRect(blue.copy(alpha = 0.4f), Offset(0f, top), Size(size.width, tankHeight),
            CornerRadius(radius), style = Stroke(1.5.dp.toPx()))
        drawLine(Color.White.copy(alpha = 0.7f), Offset(size.width * 0.18f, top + tankHeight * 0.12f),
            Offset(size.width * 0.18f, top + tankHeight * 0.7f), 3.dp.toPx())
        if (pouring) {
            val y = top + drop * (tankHeight - fillHeight - inset).coerceAtLeast(0f)
            drawCircle(blue.copy(alpha = 1f - drop * 0.5f), 4.dp.toPx(), Offset(size.width * 0.5f, y))
        }
    }
}
