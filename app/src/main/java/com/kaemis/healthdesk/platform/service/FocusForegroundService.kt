package com.kaemis.healthdesk.platform.service

import android.app.PendingIntent
import android.app.Service
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kaemis.healthdesk.MainActivity
import com.kaemis.healthdesk.R
import com.kaemis.healthdesk.platform.notification.HealthDeskNotificationChannels

class FocusForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        HealthDeskNotificationChannels.create(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startForeground(
                notificationId(intent),
                buildNotification(
                    phaseLabel = intent?.getStringExtra(EXTRA_PHASE_LABEL) ?: "Focus",
                    remainingLabel = intent?.getStringExtra(EXTRA_REMAINING_LABEL) ?: "--:--",
                    isAlarm = intent?.getBooleanExtra(EXTRA_IS_ALARM, false) ?: false,
                    isPaused = intent?.getBooleanExtra(EXTRA_IS_PAUSED, false) ?: false,
                    isRestAlarm = intent?.getBooleanExtra(EXTRA_IS_REST_ALARM, false) ?: false,
                ),
            )
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(
        phaseLabel: String,
        remainingLabel: String,
        isAlarm: Boolean,
        isPaused: Boolean,
        isRestAlarm: Boolean,
    ) = NotificationCompat.Builder(
        this,
        if (isAlarm) {
            HealthDeskNotificationChannels.FOCUS_ALARM_CHANNEL_ID
        } else {
            HealthDeskNotificationChannels.FOCUS_SESSION_CHANNEL_ID
        },
    )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("HealthDesk")
        .setContentText("$phaseLabel - $remainingLabel")
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_SERVICE)
        .setPriority(if (isAlarm) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        .setSilent(true)
        .setOngoing(!isAlarm)
        .setOnlyAlertOnce(!isAlarm)
        .setContentIntent(openAppPendingIntent())
        .apply {
            if (isAlarm) {
                addAction(0, "Stop alarm", actionPendingIntent(FocusActionReceiver.ACTION_STOP_ALARM))
                addAction(0, "Snooze", actionPendingIntent(FocusActionReceiver.ACTION_SNOOZE))
                if (isRestAlarm) {
                    addAction(0, "Start focus", actionPendingIntent(FocusActionReceiver.ACTION_START_FOCUS))
                }
            } else {
                addAction(
                    0,
                    if (isPaused) "Resume" else "Pause",
                    actionPendingIntent(
                        if (isPaused) FocusActionReceiver.ACTION_RESUME else FocusActionReceiver.ACTION_PAUSE,
                    ),
                )
                addAction(0, "Stop", actionPendingIntent(FocusActionReceiver.ACTION_STOP))
            }
        }
        .build()

    private fun openAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun actionPendingIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        this,
        action.hashCode(),
        FocusActionReceiver.intent(this, action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notificationId(intent: Intent?): Int = if (intent?.getBooleanExtra(EXTRA_IS_ALARM, false) == true) {
        HealthDeskNotificationChannels.FOCUS_ALARM_NOTIFICATION_ID
    } else {
        HealthDeskNotificationChannels.FOCUS_SESSION_NOTIFICATION_ID
    }

    companion object {
        const val ACTION_UPDATE = "com.kaemis.healthdesk.focus.UPDATE"
        const val ACTION_STOP_SERVICE = "com.kaemis.healthdesk.focus.STOP_SERVICE"
        const val EXTRA_PHASE_LABEL = "phaseLabel"
        const val EXTRA_REMAINING_LABEL = "remainingLabel"
        const val EXTRA_IS_ALARM = "isAlarm"
        const val EXTRA_IS_PAUSED = "isPaused"
        const val EXTRA_IS_REST_ALARM = "isRestAlarm"

        fun updateIntent(
            context: Context,
            phaseLabel: String,
        remainingLabel: String,
        isAlarm: Boolean,
        isPaused: Boolean,
        isRestAlarm: Boolean,
        ): Intent = Intent(context, FocusForegroundService::class.java)
            .setAction(ACTION_UPDATE)
            .putExtra(EXTRA_PHASE_LABEL, phaseLabel)
            .putExtra(EXTRA_REMAINING_LABEL, remainingLabel)
            .putExtra(EXTRA_IS_ALARM, isAlarm)
            .putExtra(EXTRA_IS_PAUSED, isPaused)
            .putExtra(EXTRA_IS_REST_ALARM, isRestAlarm)

        fun stopIntent(context: Context): Intent = Intent(context, FocusForegroundService::class.java)
            .setAction(ACTION_STOP_SERVICE)
    }
}
