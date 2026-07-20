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
        // Future calendar days must not become the apparent "today" in the UI.
        val monthDays = (0 until today.dayOfMonth).map { monthStart.plusDays(it.toLong()) }
        val weekDays = (6L downTo 0L).map { today.minusDays(it) }
        val sessionsToday = focusSessions.filter { millisToDate(it.startedAt, zoneId) == today && it.actualFocusSeconds > 0 }
        val completedToday = completedTasks.filter { it.completedAt?.let { completedAt -> millisToDate(completedAt, zoneId) == today } == true }
        val remindersToday = reminderEvents.filter { millisToDate(it.firedAt, zoneId) == today }

        return StatsSnapshot(
            focusMinutesToday = sessionsToday.sumOf { it.actualFocusSeconds } / 60,
            focusSessionsToday = sessionsToday.size,
            completedTasksToday = completedToday.size,
            remindersToday = remindersToday.size,
            dailyStats = monthDays.map { day -> daySnapshot(day, focusSessions, completedTasks, reminderEvents, zoneId) },
            weeklyStats = weekDays.map { day -> daySnapshot(day, focusSessions, completedTasks, reminderEvents, zoneId) },
            recentCompletedTasks = completedTasks
                .sortedByDescending { it.completedAt }
                .take(12)
                .mapNotNull { task -> task.completedAt?.let { CompletedTaskStat(task.title, it) } },
            focusSessions = focusSessions.map { session ->
                FocusSessionStat(session.startedAt, session.endedAt, session.actualFocusSeconds, session.status, session.endReason)
            },
            completedTaskDetails = completedTasks.mapNotNull { task -> task.completedAt?.let { CompletedTaskStat(task.title, it) } },
            reminderEvents = reminderEvents.map { event ->
                ReminderEventStat(event.titleSnapshot, event.firedAt, event.deliveryResult)
            },
        )
    }

    private fun millisToDate(millis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()

    private fun daySnapshot(
        day: LocalDate,
        focusSessions: List<FocusSessionEntity>,
        completedTasks: List<TaskEntity>,
        reminderEvents: List<ReminderEventEntity>,
        zoneId: ZoneId,
    ) = StatsDaySnapshot(
        date = day.toString(),
        focusMinutes = focusSessions.filter { millisToDate(it.startedAt, zoneId) == day }.sumOf { it.actualFocusSeconds } / 60,
        focusSessions = focusSessions.count { millisToDate(it.startedAt, zoneId) == day && it.actualFocusSeconds > 0 },
        completedTasks = completedTasks.count { it.completedAt?.let { completedAt -> millisToDate(completedAt, zoneId) == day } == true },
        reminders = reminderEvents.count { millisToDate(it.firedAt, zoneId) == day },
    )
}
