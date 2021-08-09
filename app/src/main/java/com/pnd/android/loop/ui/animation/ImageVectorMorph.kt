//package com.pnd.android.loop.ui.animation
//
//import androidx.compose.animation.core.LinearEasing
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Image
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.*
//import androidx.compose.ui.graphics.vector.*
//import androidx.compose.ui.unit.dp
//
//
//@Composable
//fun ImageVectorMorph(
//    modifier: Modifier = Modifier,
//    vectorImages: List<ImageVector>,
//    imageColor:Color,
//    durationMillis: Int = 2500
//) {
//    val animatedProgress = animatedFloat(0f)
//    onActive {
//        animatedProgress.animateTo(
//            targetValue = 1f,
//            anim = infiniteRepeatable(
//                animation = tween(durationMillis = durationMillis, easing = LinearEasing),
//            ),
//        )
//    }
//
//    val animalColors = Array(vectorImages.size) { imageColor }
//
//    val vectorImagePathNodes = remember {
//        mutableListOf<List<PathNode>>().apply {
//            vectorImages.forEach { add(it.root.clipPathData) }
//        }
//    }
//
//    val t = animatedProgress.value
//    val startIndex = (t * 3).toInt()
//    val endIndex = (startIndex + 1) % vectorImagePathNodes.size
//    val tt = t * 3 - (if (t < 1f / 3f) 0 else if (t < 2f / 3f) 1 else 2)
//    val color = lerp(animalColors[startIndex], animalColors[endIndex], ease(tt, 3f))
//    val pathNodes =
//        lerp(vectorImagePathNodes[startIndex], vectorImagePathNodes[endIndex], ease(tt, 3f))
//
//    Image(
//        painter = rememberVectorPainter(
//            defaultWidth = 409.6.dp,
//            defaultHeight = 280.6.dp,
//            viewportWidth = 409.6f,
//            viewportHeight = 280.6f,
//        ) { vw, vh ->
//            // Draw a white background rect.
//            Path(
//                pathData = remember { addPathNodes("h $vw v $vh h -$vw v -$vh") },
//                fill = SolidColor(Color.White),
//            )
//            // Draw the morphed animal path.
//            Path(
//                pathData = pathNodes,
//                fill = SolidColor(color),
//            )
//        },
//        contentDescription = "",
//        modifier = modifier,
//    )
//}
