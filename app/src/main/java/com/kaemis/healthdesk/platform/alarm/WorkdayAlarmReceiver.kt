package com.kaemis.healthdesk.platform.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kaemis.healthdesk.HealthDeskApplication
import com.kaemis.healthdesk.MainActivity
import com.kaemis.healthdesk.R
import com.kaemis.healthdesk.platform.notification.HealthDeskNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorkdayAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? HealthDeskApplication ?: return@launch
                val settings = app.appContainer.settingsDataStore.settings.first()
                if (settings.workdayNotificationsEnabled && !settings.outOfOffice) {
                    showNotification(context, action == ACTION_WORKDAY_ENDED)
                }
                app.appContainer.resyncWorkdayAlarms()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, isEnd: Boolean) {
        val openApp = PendingIntent.getActivity(
            context,
            5000 + if (isEnd) 1 else 0,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SHOW_OVER_LOCK_SCREEN, isEnd),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, HealthDeskNotificationChannels.FOCUS_ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(if (isEnd) "Workday ended" else "Workday started")
            .setContentText(if (isEnd) "Your working hours have ended." else "Your working hours have started.")
            .setContentIntent(openApp)
            .setCategory(if (isEnd) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (isEnd) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setSilent(!isEnd)
            .apply { if (isEnd) setFullScreenIntent(openApp, true) }
            .build()
        context.getSystemService(NotificationManager::class.java).notify(102, notification)
    }

    companion object {
        const val ACTION_WORKDAY_STARTED = "com.kaemis.healthdesk.workday.STARTED"
        const val ACTION_WORKDAY_ENDED = "com.kaemis.healthdesk.workday.ENDED"
    }
}
