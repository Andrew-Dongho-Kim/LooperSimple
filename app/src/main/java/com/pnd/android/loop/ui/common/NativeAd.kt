package com.pnd.android.loop.ui.common

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pnd.android.loop.databinding.LoopNativeAdLayoutBinding
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.Black99
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface

@Composable
fun ExpandableNativeAd(
    modifier: Modifier = Modifier,
    adId: String
) {
    val adViewModel = viewModel<AdViewModel>()
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = adId) {
        adViewModel.loadAd(adId)
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = adViewModel.nativeAd != null,
        enter = fadeIn() + slideInVertically()
    ) {
        Card(
            modifier = Modifier
                .animateContentSize()
                .clip(shape = RoundShapes.medium)
                .clickable {
                    isExpanded = !isExpanded
                }
                .graphicsLayer {
                    this.alpha = alpha
                },
            border = BorderStroke(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.2f)
            )
        ) {
            Column {
                NativeAdContent(
                    adViewModel = adViewModel,
                    isExpanded = isExpanded,
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
                color = Black99.copy(alpha = 0.7f)
            ),
            contentDescription = ""
        )
    }
}

@Composable
private fun NativeAdContent(
    modifier: Modifier = Modifier,
    adViewModel: AdViewModel,
    isExpanded: Boolean
) {
    AndroidViewBinding(
        modifier = modifier,
        factory = { inflater, parent, attachToParent ->
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
                adMedia.visibility = View.GONE
                adViewModel.nativeAd?.let { adView.setNativeAd(it) }
            }
        }
    ) {
        adViewModel.nativeAd?.let { nativeAd ->
            root.setNativeAd(nativeAd)
            nativeAd.advertiser?.let { advertiser -> adAdvertiser.text = advertiser }
            nativeAd.body?.let { body -> adBody.text = body }
            nativeAd.callToAction?.let { cta -> adCallToAction.text = cta }
            nativeAd.headline?.let { headline -> adHeadline.text = headline }
            nativeAd.icon?.let { icon -> adAppIcon.setImageDrawable(icon.drawable) }
            nativeAd.price?.let { price -> adPrice.text = price }
            nativeAd.starRating?.let { rating -> adStars.rating = rating.toFloat() }
            nativeAd.store?.let { store -> adStore.text = store }
            nativeAd.mediaContent?.let { media -> adMedia.mediaContent = media }
        }
        adMedia.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }
}
