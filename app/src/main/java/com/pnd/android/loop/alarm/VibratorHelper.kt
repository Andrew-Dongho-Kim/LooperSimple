package com.pnd.android.loop.alarm

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import dagger.hilt.android.qualifiers.ApplicationContext

class VibratorHelper(
    @ApplicationContext private val context: Context
) {

    private val audioManager by lazy(LazyThreadSafetyMode.NONE) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val vibrator by lazy(LazyThreadSafetyMode.NONE) {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibe() {
        val msTime = 10L
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    msTime,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(msTime)
        }
    }
}