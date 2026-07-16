package com.kaemis.healthdesk.platform.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.kaemis.healthdesk.R

interface TaskFeedbackController {
    fun playTaskComplete(soundKey: String, hapticsEnabled: Boolean)

    object NoOp : TaskFeedbackController {
        override fun playTaskComplete(soundKey: String, hapticsEnabled: Boolean) = Unit
    }
}

class AndroidTaskFeedbackController(
    private val context: Context,
) : TaskFeedbackController {
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)

    override fun playTaskComplete(soundKey: String, hapticsEnabled: Boolean) {
        if (soundKey != "silent") {
            runCatching {
                MediaPlayer.create(context, soundResource(soundKey))?.apply {
                    setOnCompletionListener { player -> player.release() }
                    start()
                }
            }
        }
        if (hapticsEnabled) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45)
                }
            }
        }
    }

    private fun soundResource(soundKey: String): Int = when (soundKey) {
        "ring2" -> R.raw.ring2
        "ring3" -> R.raw.ring3
        "ring4" -> R.raw.ring4
        else -> R.raw.ring1
    }
}
