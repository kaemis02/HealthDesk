package com.kaemis.healthdesk.platform.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.kaemis.healthdesk.R

interface AlarmFeedbackController {
    fun startAlarm(soundKey: String, hapticsEnabled: Boolean)
    fun stopAlarm()

    object NoOp : AlarmFeedbackController {
        override fun startAlarm(soundKey: String, hapticsEnabled: Boolean) = Unit
        override fun stopAlarm() = Unit
    }
}

class AndroidAlarmFeedbackController(
    private val context: Context,
) : AlarmFeedbackController {
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)
    private var alarmRunning = false

    override fun startAlarm(soundKey: String, hapticsEnabled: Boolean) {
        // Restoring the app must not restart an alarm loop that is already audible.
        if (alarmRunning) return
        alarmRunning = true
        if (soundKey != "silent") {
            runCatching {
                mediaPlayer = MediaPlayer.create(context, soundResource(soundKey))?.apply {
                    isLooping = true
                    start()
                }
            }
        }
        if (hapticsEnabled) {
            runCatching {
                val pattern = longArrayOf(0, 500, 300, 500, 1_000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        }
    }

    override fun stopAlarm() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        alarmRunning = false
        runCatching { vibrator?.cancel() }
    }

    private fun soundResource(soundKey: String): Int = when (soundKey) {
        "tone2" -> R.raw.tone2
        "tone3" -> R.raw.tone3
        "tone4" -> R.raw.tone4
        else -> R.raw.tone1
    }
}
