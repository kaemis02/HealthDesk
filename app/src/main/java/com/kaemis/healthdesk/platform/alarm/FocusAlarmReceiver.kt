package com.kaemis.healthdesk.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaemis.healthdesk.HealthDeskApplication
import com.kaemis.healthdesk.platform.service.FocusActionCommand

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val command = when (intent?.action) {
            ACTION_PHASE_ELAPSED -> FocusActionCommand.PhaseElapsed
            ACTION_WORKDAY_ENDED -> FocusActionCommand.WorkdayEnded
            else -> return
        }
        val app = context.applicationContext as? HealthDeskApplication ?: return
        app.appContainer.focusActionBus.send(command)
    }

    companion object {
        const val ACTION_PHASE_ELAPSED = "com.kaemis.healthdesk.focus.alarm.PHASE_ELAPSED"
        const val ACTION_WORKDAY_ENDED = "com.kaemis.healthdesk.focus.alarm.WORKDAY_ENDED"
    }
}
