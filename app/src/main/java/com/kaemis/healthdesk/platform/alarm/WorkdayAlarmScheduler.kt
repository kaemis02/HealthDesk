package com.kaemis.healthdesk.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Schedules the next configured workday start and end independently from Focus sessions. */
class WorkdayAlarmScheduler(
    private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun resync(rules: List<WorkingHourRuleEntity>, enabled: Boolean, outOfOffice: Boolean, now: Long = System.currentTimeMillis()) {
        cancel()
        if (!enabled || outOfOffice) return
        nextBoundaries(rules, now).forEach { (action, triggerAt) -> schedule(action, triggerAt) }
    }

    fun cancel() {
        listOf(WorkdayAlarmReceiver.ACTION_WORKDAY_STARTED, WorkdayAlarmReceiver.ACTION_WORKDAY_ENDED).forEach { action ->
            pendingIntent(action, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
        }
    }

    private fun schedule(action: String, triggerAt: Long) {
        val pendingIntent = pendingIntent(action, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun pendingIntent(action: String, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, WorkdayAlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or flags,
    )

    private fun nextBoundaries(rules: List<WorkingHourRuleEntity>, now: Long): List<Pair<String, Long>> {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (0L..7L).mapNotNull { offset ->
            val currentDate = date.plusDays(offset)
            val rule = rules.firstOrNull { it.isEnabled && it.dayOfWeek == currentDate.dayOfWeek.value } ?: return@mapNotNull null
            val start = LocalDateTime.of(currentDate, LocalTime.parse(rule.startLocalTime)).atZone(zone).toInstant().toEpochMilli()
            val endTime = LocalTime.parse(rule.endLocalTime)
            val endDate = if (endTime.isBefore(LocalTime.parse(rule.startLocalTime))) currentDate.plusDays(1) else currentDate
            val end = LocalDateTime.of(endDate, endTime).atZone(zone).toInstant().toEpochMilli()
            listOfNotNull(
                start.takeIf { it > now }?.let { WorkdayAlarmReceiver.ACTION_WORKDAY_STARTED to it },
                end.takeIf { it > now && end != start }?.let { WorkdayAlarmReceiver.ACTION_WORKDAY_ENDED to it },
            )
        }.flatten().groupBy { it.first }.mapValues { (_, values) -> values.minBy { it.second } }.values.toList()
    }
}
