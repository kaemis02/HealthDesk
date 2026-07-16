package com.kaemis.healthdesk.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlin.math.abs

interface ReminderAlarmScheduler {
    fun schedule(reminderId: String, triggerAtMillis: Long)
    fun cancel(reminderId: String)

    object NoOp : ReminderAlarmScheduler {
        override fun schedule(reminderId: String, triggerAtMillis: Long) = Unit
        override fun cancel(reminderId: String) = Unit
    }
}

class AndroidReminderAlarmScheduler(
    private val context: Context,
) : ReminderAlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(reminderId: String, triggerAtMillis: Long) {
        val pendingIntent = pendingIntent(reminderId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    override fun cancel(reminderId: String) {
        pendingIntent(reminderId, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    private fun pendingIntent(reminderId: String, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        requestCode(reminderId),
        ReminderReceiver.intent(context, reminderId),
        PendingIntent.FLAG_IMMUTABLE or flags,
    )

    private fun requestCode(reminderId: String): Int = 2_000 + abs(reminderId.hashCode() % 100_000)
}
