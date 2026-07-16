package com.kaemis.healthdesk.domain.stats

import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import com.kaemis.healthdesk.data.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object StatsCalculator {
    fun calculate(
        focusSessions: List<FocusSessionEntity>,
        completedTasks: List<TaskEntity>,
        pendingTasks: List<TaskEntity>,
        reminderEvents: List<ReminderEventEntity>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StatsSnapshot {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val monthStart = today.withDayOfMonth(1)
        val monthDays = (0 until today.lengthOfMonth()).map { monthStart.plusDays(it.toLong()) }
        val sessionsToday = focusSessions.filter { millisToDate(it.startedAt, zoneId) == today && it.status == "completed" }
        val completedToday = completedTasks.filter { it.completedAt?.let { completedAt -> millisToDate(completedAt, zoneId) == today } == true }
        val remindersToday = reminderEvents.filter { millisToDate(it.firedAt, zoneId) == today }

        return StatsSnapshot(
            focusMinutesToday = sessionsToday.sumOf { it.actualFocusSeconds } / 60,
            focusSessionsToday = sessionsToday.size,
            completedTasksToday = completedToday.size,
            remindersToday = remindersToday.size,
            dailyStats = monthDays.map { day ->
                StatsDaySnapshot(
                    date = day.toString(),
                    focusMinutes = focusSessions.filter { millisToDate(it.startedAt, zoneId) == day }.sumOf { it.actualFocusSeconds } / 60,
                    focusSessions = focusSessions.count { millisToDate(it.startedAt, zoneId) == day && it.status == "completed" },
                    completedTasks = completedTasks.count { it.completedAt?.let { completedAt -> millisToDate(completedAt, zoneId) == day } == true },
                    reminders = reminderEvents.count { millisToDate(it.firedAt, zoneId) == day },
                )
            },
            recentCompletedTasks = completedTasks
                .sortedByDescending { it.completedAt }
                .take(12)
                .mapNotNull { task -> task.completedAt?.let { CompletedTaskStat(task.title, it) } },
        )
    }

    private fun millisToDate(millis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
}
