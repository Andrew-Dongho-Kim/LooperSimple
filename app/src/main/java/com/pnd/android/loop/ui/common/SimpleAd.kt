package com.pnd.android.loop.ui.common

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView


@Composable
fun SimpleAd(
    modifier: Modifier = Modifier,
    adId: String
) {
    val isAdLoaded = remember { mutableStateOf(false) }
    val transition = updateTransition(
        targetState = isAdLoaded.value,
        label = "updateAdFadeTransition"
    )
    val alpha by transition.animateFloat(
        label = "fadeAlpha",
        transitionSpec = { tween(durationMillis = 500) }) {
        if (it) 1f else 0f
    }

    AndroidView(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = adId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        isAdLoaded.value = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        })
}
