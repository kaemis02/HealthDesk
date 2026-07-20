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

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val reminderId = intent?.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReminder(context.applicationContext, reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context, reminderId: String) {
        val app = context as? HealthDeskApplication ?: return
        HealthDeskNotificationChannels.create(context)
        ReminderDeliveryProcessor(
            loadSettings = { app.appContainer.settingsDataStore.settings.first() },
            getReminder = app.appContainer.reminderRepository::getReminder,
            saveReminder = app.appContainer.reminderRepository::saveReminder,
            recordEvent = app.appContainer.reminderRepository::recordEvent,
            scheduleReminder = app.appContainer.reminderAlarmScheduler::schedule,
            cancelReminder = app.appContainer.reminderAlarmScheduler::cancel,
            showNotification = { title, id -> showNotification(context, title, id) },
            feedbackController = app.appContainer.reminderFeedbackController,
            loadWorkingHourRules = app.appContainer.workingHoursRepository::getRules,
        ).handle(reminderId)
    }

    private fun showNotification(context: Context, title: String, reminderId: String) {
        val notification = NotificationCompat.Builder(context, HealthDeskNotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("HealthDesk reminder")
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    reminderId.hashCode(),
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(2_000 + reminderId.hashCode(), notification)
    }

    companion object {
        private const val EXTRA_REMINDER_ID = "reminderId"

        fun intent(context: Context, reminderId: String): Intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
    }
}
