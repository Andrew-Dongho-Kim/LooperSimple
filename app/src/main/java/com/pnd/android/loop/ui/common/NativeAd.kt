package com.pnd.android.loop.ui.common

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.isVisible
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.nativead.NativeAd
import com.pnd.android.loop.R
import com.pnd.android.loop.databinding.LoopNativeAdLayoutBinding
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceElevated

private val CardShape = RoundedCornerShape(20.dp)
private val IconCornerRadius = 14.dp

/**
 * A native ad rendered as a card that matches the app's surfaces in both light
 * and dark mode. The ad headline, icon and call-to-action are always shown; the
 * (optional) media preview can be revealed with the toggle at the bottom.
 */
@Composable
fun ExpandableNativeAd(
    modifier: Modifier = Modifier,
    adId: String,
) {
    val adViewModel = viewModel<AdViewModel>()
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = adId) {
        adViewModel.loadAd(adId)
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = adViewModel.nativeAd != null,
        enter = fadeIn() + slideInVertically { height -> height / 2 },
    ) {
        val adDescription = stringResource(R.string.desc_native_ad)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .semantics { contentDescription = adDescription },
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = AppColor.surfaceElevated),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(width = 1.dp, color = AppColor.outlineVariant),
        ) {
            NativeAdBody(
                adViewModel = adViewModel,
                isExpanded = isExpanded,
            )
            MediaToggle(
                isExpanded = isExpanded,
                onToggle = { isExpanded = !isExpanded },
            )
        }
    }
}

/**
 * Footer row that reveals/hides the ad's media preview. A dedicated control is
 * used instead of making the whole card clickable so taps never compete with
 * the ad's own call-to-action.
 */
@Composable
private fun MediaToggle(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "MediaToggleChevron",
    )
    val label = stringResource(if (isExpanded) R.string.ad_collapse else R.string.ad_expand)
    val contentColor = AppColor.onSurface.copy(alpha = 0.6f)

    HorizontalDivider(color = AppColor.outlineVariant)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTypography.labelMedium,
            color = contentColor,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            modifier = Modifier
                .size(18.dp)
                .rotate(chevronRotation),
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = contentColor,
        )
    }
}

/**
 * Hosts the GMA [com.google.android.gms.ads.nativead.NativeAdView] inflated from
 * XML. The layout carries no colors of its own; they are resolved from the
 * Compose theme here and pushed onto the views so the ad always matches the
 * active light/dark palette.
 */
@Composable
private fun NativeAdBody(
    adViewModel: AdViewModel,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = rememberNativeAdColors()
    val iconCornerRadiusPx = with(LocalDensity.current) { IconCornerRadius.toPx() }

    AndroidViewBinding(
        modifier = modifier,
        factory = { inflater, parent, attachToParent ->
            LoopNativeAdLayoutBinding.inflate(inflater, parent, attachToParent).apply {
                registerAssetViews()
                adAppIcon.shapeAppearanceModel = adAppIcon.shapeAppearanceModel
                    .toBuilder()
                    .setAllCornerSizes(iconCornerRadiusPx)
                    .build()
            }
        },
    ) {
        applyColors(colors)
        adViewModel.nativeAd?.let(::bindNativeAd)
        adMedia.isVisible = isExpanded
    }
}

/** Theme-resolved colors pushed onto the inflated ad views. */
private data class NativeAdColors(
    val headline: Int,
    val secondaryText: Int,
    val callToActionBackground: Int,
    val callToActionText: Int,
    val attributionBackground: Int,
    val attributionText: Int,
)

@Composable
private fun rememberNativeAdColors(): NativeAdColors {
    val headline = AppColor.onSurface
    val secondary = AppColor.onSurface.copy(alpha = 0.6f)
    val primary = AppColor.primary
    val onPrimary = AppColor.onPrimary
    val attributionBackground = primary.compositeOverSurface(alpha = 0.14f)

    return remember(headline, secondary, primary, onPrimary, attributionBackground) {
        NativeAdColors(
            headline = headline.toArgb(),
            secondaryText = secondary.toArgb(),
            callToActionBackground = primary.toArgb(),
            callToActionText = onPrimary.toArgb(),
            attributionBackground = attributionBackground.toArgb(),
            attributionText = primary.toArgb(),
        )
    }
}

/** Wires each asset view to the [NativeAdView] so the SDK can track clicks. */
private fun LoopNativeAdLayoutBinding.registerAssetViews() {
    root.iconView = adAppIcon
    root.headlineView = adHeadline
    root.advertiserView = adAdvertiser
    root.starRatingView = adStars
    root.bodyView = adBody
    root.callToActionView = adCallToAction
    root.storeView = adStore
    root.priceView = adPrice
    root.mediaView = adMedia
}

private fun LoopNativeAdLayoutBinding.applyColors(colors: NativeAdColors) {
    adHeadline.setTextColor(colors.headline)
    adAdvertiser.setTextColor(colors.secondaryText)
    adBody.setTextColor(colors.secondaryText)
    adStore.setTextColor(colors.secondaryText)
    adPrice.setTextColor(colors.secondaryText)

    adAttribution.setTextColor(colors.attributionText)
    adAttribution.backgroundTintList = ColorStateList.valueOf(colors.attributionBackground)

    adCallToAction.setTextColor(colors.callToActionText)
    adCallToAction.backgroundTintList = ColorStateList.valueOf(colors.callToActionBackground)
}

private fun LoopNativeAdLayoutBinding.bindNativeAd(nativeAd: NativeAd) {
    root.setNativeAd(nativeAd)

    adHeadline.text = nativeAd.headline
    adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
    adAppIcon.isVisible = nativeAd.icon != null

    adAdvertiser.setTextOrHide(nativeAd.advertiser)
    adBody.setTextOrHide(nativeAd.body)
    adStore.setTextOrHide(nativeAd.store)
    adPrice.setTextOrHide(nativeAd.price)
    adCallToAction.setTextOrHide(nativeAd.callToAction)

    val rating = nativeAd.starRating?.toFloat()
    adStars.isVisible = rating != null
    rating?.let { adStars.rating = it }

    nativeAd.mediaContent?.let { adMedia.mediaContent = it }
}

private fun TextView.setTextOrHide(value: String?) {
    text = value
    isVisible = !value.isNullOrBlank()
}
