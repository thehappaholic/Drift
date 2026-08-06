package com.example.drift

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

object DriftSoundPlayer {
    fun playFocusSuccess(context: Context) = play(context, R.raw.focus_success)

    fun playSessionEnd(context: Context) = play(context, R.raw.focus_session_end)

    private fun play(context: Context, @RawRes sound: Int) {
        val player = runCatching { MediaPlayer.create(context.applicationContext, sound) }
            .getOrNull() ?: return
        player.setOnCompletionListener(MediaPlayer::release)
        player.setOnErrorListener { mediaPlayer, _, _ ->
            mediaPlayer.release()
            true
        }
        runCatching { player.start() }.onFailure { player.release() }
    }
}
