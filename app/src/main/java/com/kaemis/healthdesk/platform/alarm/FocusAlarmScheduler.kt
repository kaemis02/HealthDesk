package com.kaemis.healthdesk.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

interface FocusAlarmScheduler {
    fun schedulePhaseEnd(triggerAtMillis: Long)
    fun cancelPhaseEnd()
    fun scheduleWorkdayEnd(triggerAtMillis: Long)
    fun cancelWorkdayEnd()

    object NoOp : FocusAlarmScheduler {
        override fun schedulePhaseEnd(triggerAtMillis: Long) = Unit
        override fun cancelPhaseEnd() = Unit
        override fun scheduleWorkdayEnd(triggerAtMillis: Long) = Unit
        override fun cancelWorkdayEnd() = Unit
    }
}

class AndroidFocusAlarmScheduler(
    private val context: Context,
) : FocusAlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedulePhaseEnd(triggerAtMillis: Long) {
        schedule(triggerAtMillis, phaseEndPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return)
    }

    override fun cancelPhaseEnd() {
        phaseEndPendingIntent(PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    override fun scheduleWorkdayEnd(triggerAtMillis: Long) {
        schedule(triggerAtMillis, workdayEndPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return)
    }

    override fun cancelWorkdayEnd() {
        workdayEndPendingIntent(PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    private fun schedule(triggerAtMillis: Long, pendingIntent: PendingIntent) {
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

    private fun phaseEndPendingIntent(flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        PHASE_END_REQUEST_CODE,
        Intent(context, FocusAlarmReceiver::class.java).setAction(FocusAlarmReceiver.ACTION_PHASE_ELAPSED),
        PendingIntent.FLAG_IMMUTABLE or flags,
    )

    private fun workdayEndPendingIntent(flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        WORKDAY_END_REQUEST_CODE,
        Intent(context, FocusAlarmReceiver::class.java).setAction(FocusAlarmReceiver.ACTION_WORKDAY_ENDED),
        PendingIntent.FLAG_IMMUTABLE or flags,
    )

    private companion object {
        const val PHASE_END_REQUEST_CODE = 1001
        const val WORKDAY_END_REQUEST_CODE = 1002
    }
}
