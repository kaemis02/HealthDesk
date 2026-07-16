package com.kaemis.healthdesk.platform.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaemis.healthdesk.HealthDeskApplication

class FocusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val command = when (intent?.action) {
            ACTION_START_FOCUS -> FocusActionCommand.StartFocus
            ACTION_PAUSE -> FocusActionCommand.Pause
            ACTION_RESUME -> FocusActionCommand.Resume
            ACTION_STOP -> FocusActionCommand.Stop
            ACTION_STOP_ALARM -> FocusActionCommand.StopAlarm
            ACTION_SNOOZE -> FocusActionCommand.Snooze
            else -> return
        }
        val app = context.applicationContext as? HealthDeskApplication ?: return
        app.appContainer.focusActionBus.send(command)
    }

    companion object {
        const val ACTION_START_FOCUS = "com.kaemis.healthdesk.focus.action.START_FOCUS"
        const val ACTION_PAUSE = "com.kaemis.healthdesk.focus.action.PAUSE"
        const val ACTION_RESUME = "com.kaemis.healthdesk.focus.action.RESUME"
        const val ACTION_STOP = "com.kaemis.healthdesk.focus.action.STOP"
        const val ACTION_STOP_ALARM = "com.kaemis.healthdesk.focus.action.STOP_ALARM"
        const val ACTION_SNOOZE = "com.kaemis.healthdesk.focus.action.SNOOZE"

        fun intent(context: Context, action: String): Intent = Intent(context, FocusActionReceiver::class.java)
            .setAction(action)
    }
}
