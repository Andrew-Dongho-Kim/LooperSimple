package com.pnd.android.loop.ui.common

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.pnd.android.loop.common.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

class AdViewModel(
    private val application: Application
) : AndroidViewModel(application) {

    private val logger = Logger("AdViewModel")

    var nativeAd by mutableStateOf<NativeAd?>(null)

    suspend fun loadAd(adId: String) {
        if (nativeAd != null) return

        logger.d { "start loadAd:$adId" }
        suspendCancellableCoroutine { continuation ->
            val adLoader = AdLoader.Builder(
                application,
                adId
            ).forNativeAd { nativeAd ->
                this.nativeAd = nativeAd
                continuation.resumeWith(Result.success(nativeAd))
                logger.d { "success to load ad:$adId" }
            }.withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .setRequestCustomMuteThisAd(true)
                    .build()
            ).withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    continuation.resumeWith(Result.success(nativeAd))
                }
            }).build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }
}