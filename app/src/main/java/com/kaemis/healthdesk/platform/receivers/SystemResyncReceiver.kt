package com.kaemis.healthdesk.platform.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kaemis.healthdesk.HealthDeskApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SystemResyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as? HealthDeskApplication
                        app?.appContainer?.resyncReminderAlarms()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
