package com.pnd.android.loop.alarm

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class AlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
) {

    private var mediaPlayer: MediaPlayer? = null

    fun play(rawResId: Int) {
        play(rawToUri(context, rawResId))
    }

    private fun play(uri: Uri?) {
        uri ?: return
        stop()
        mediaPlayer = MediaPlayer.create(context, uri).apply {
            setOnCompletionListener {
                stop()
            }
            start()
        }

    }

    fun stop() {
        mediaPlayer = mediaPlayer?.let {
            it.stop()
            it.release()
            null
        }
    }

}