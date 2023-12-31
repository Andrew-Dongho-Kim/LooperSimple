package com.pnd.android.loop.ui.common

import android.view.View
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.pnd.android.loop.databinding.LoopNativeAdLayoutBinding
import com.pnd.android.loop.ui.theme.RoundShapes

private enum class LoadState {
    READY, LOADING, FAILED, FINISHED
}

const val ANIMATION_DURATION = 500

@Composable
fun NativeAd(
    modifier: Modifier = Modifier,
    adId: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    var loadState by remember { mutableStateOf(LoadState.READY) }

    val alpha by animateFloatAsState(
        targetValue = if (loadState == LoadState.FINISHED) 1f else 0f,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        ),
        label = "cardAlpha"
    )

    Card(
        modifier = if (loadState == LoadState.FINISHED) {
            modifier.animateContentSize(
                animationSpec = tween(
                    durationMillis = ANIMATION_DURATION,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            modifier.height(0.dp)
        }
            .clip(shape = RoundShapes.medium)
            .clickable {
                isExpanded = !isExpanded
            }
            .graphicsLayer {
                this.alpha = alpha
            },
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        )
    ) {
        Column {
            NativeAdContent(
                adId = adId,
                isExpanded = isExpanded,
                onLoadStateChanged = { state -> loadState = state }
            )

            ExpandableImage(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(24.dp),
                isExpanded = isExpanded,
            )
        }
    }
}

@Composable
private fun ExpandableImage(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Image(
            modifier = Modifier
                .aspectRatio(2f)
                .align(Alignment.Center)
                .graphicsLayer { rotationX = if (isExpanded) 180f else 0f },
            imageVector = Icons.Filled.KeyboardArrowDown,
            colorFilter = ColorFilter.tint(
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
            ),
            contentDescription = ""
        )
    }
}

@Composable
private fun NativeAdContent(
    modifier: Modifier = Modifier,
    adId: String,
    isExpanded: Boolean,
    onLoadStateChanged: (LoadState) -> Unit
) {
    AndroidViewBinding(
        modifier = modifier,
        factory = { inflater, parent, attachToParent ->
            onLoadStateChanged(LoadState.LOADING)
            LoopNativeAdLayoutBinding.inflate(inflater, parent, attachToParent).apply {
                val adView = root.apply {
                    advertiserView = adAdvertiser
                    bodyView = adBody
                    callToActionView = adCallToAction
                    headlineView = adHeadline
                    iconView = adAppIcon
                    priceView = adPrice
                    starRatingView = adStars
                    storeView = adStore
                    mediaView = adMedia
                }
                adView.visibility = View.GONE
                adMedia.visibility = View.GONE

                val adLoader = AdLoader.Builder(
                    adView.context,
                    adId
                ).forNativeAd { nativeAd ->
                    nativeAd.advertiser?.let { advertiser -> adAdvertiser.text = advertiser }
                    nativeAd.body?.let { body -> adBody.text = body }
                    nativeAd.callToAction?.let { cta -> adCallToAction.text = cta }
                    nativeAd.headline?.let { headline -> adHeadline.text = headline }
                    nativeAd.icon?.let { icon -> adAppIcon.setImageDrawable(icon.drawable) }
                    nativeAd.price?.let { price -> adPrice.text = price }
                    nativeAd.starRating?.let { rating -> adStars.rating = rating.toFloat() }
                    nativeAd.store?.let { store -> adStore.text = store }
                    nativeAd.mediaContent?.let { media -> adMedia.mediaContent = media }

                    adView.visibility = View.VISIBLE
                    adView.setNativeAd(nativeAd)
                    onLoadStateChanged(LoadState.FINISHED)
                }.withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                        .setRequestCustomMuteThisAd(true)
                        .build()
                ).withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onLoadStateChanged(LoadState.FAILED)
                    }
                }).build()

                adLoader.loadAd(AdRequest.Builder().build())
            }

        }
    ) {
        adMedia.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }
}
