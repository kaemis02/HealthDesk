package com.kaemis.healthdesk.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build

object HealthDeskNotificationChannels {
    const val FOCUS_SESSION_CHANNEL_ID = "healthdesk_focus_session"
    const val FOCUS_ALARM_CHANNEL_ID = "healthdesk_focus_alarm"
    const val REMINDERS_CHANNEL_ID = "healthdesk_reminders"
    const val FOCUS_SESSION_NOTIFICATION_ID = 100
    const val FOCUS_ALARM_NOTIFICATION_ID = 101

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(
                FOCUS_SESSION_CHANNEL_ID,
                "Focus session",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Persistent focus timer status"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            },
            NotificationChannel(
                FOCUS_ALARM_CHANNEL_ID,
                "Focus alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "End of focus, rest and workday alarms"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            },
            NotificationChannel(
                REMINDERS_CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Wellness reminder notifications"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            },
        )
        notificationManager.createNotificationChannels(channels)
    }
}
