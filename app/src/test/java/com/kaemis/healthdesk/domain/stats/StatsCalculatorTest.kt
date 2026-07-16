package com.kaemis.healthdesk.domain.stats

import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import com.kaemis.healthdesk.data.entity.TaskEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCalculatorTest {
    private val zoneId = ZoneId.systemDefault()

    @Test
    fun calculatesTodayAndDailyMonthStats() {
        val today = LocalDate.of(2026, 7, 13)
        val now = at(today, 12, 0)
        val stats = StatsCalculator.calculate(
            focusSessions = listOf(focusSession(at(today, 9, 0), 3_000), focusSession(at(today.minusDays(1), 9, 0), 1_200)),
            completedTasks = listOf(task("Done today", completedAt = at(today, 10, 0)), task("Done yesterday", completedAt = at(today.minusDays(1), 10, 0))),
            pendingTasks = listOf(task("Pending", categoryId = "task-work"), task("Pending 2", categoryId = "task-work")),
            reminderEvents = listOf(reminderEvent(at(today, 11, 0)), reminderEvent(at(today.minusDays(1), 11, 0))),
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(50, stats.focusMinutesToday)
        assertEquals(1, stats.focusSessionsToday)
        assertEquals(1, stats.completedTasksToday)
        assertEquals(1, stats.remindersToday)
        assertEquals(31, stats.dailyStats.size)
        assertEquals(20, stats.dailyStats.first { it.date == today.minusDays(1).toString() }.focusMinutes)
        assertEquals(50, stats.dailyStats.first { it.date == today.toString() }.focusMinutes)
        assertEquals(listOf("Done today", "Done yesterday"), stats.recentCompletedTasks.map { it.title })
    }

    private fun focusSession(startedAt: Long, actualFocusSeconds: Long): FocusSessionEntity = FocusSessionEntity(
        id = startedAt.toString(),
        startedAt = startedAt,
        endedAt = startedAt + actualFocusSeconds * 1000,
        plannedDurationMinutes = 50,
        actualFocusSeconds = actualFocusSeconds,
        plannedRestMinutes = 10,
        actualRestSeconds = 0,
        status = "completed",
        endReason = "timerElapsed",
        snoozeCount = 0,
        createdAt = startedAt,
        updatedAt = startedAt,
    )

    private fun task(
        title: String,
        categoryId: String? = "task-general",
        completedAt: Long? = null,
    ): TaskEntity = TaskEntity(
        id = title,
        title = title,
        categoryId = categoryId,
        isCompleted = completedAt != null,
        sortOrder = 0,
        createdAt = completedAt ?: 1000L,
        updatedAt = completedAt ?: 1000L,
        completedAt = completedAt,
    )

    private fun reminderEvent(firedAt: Long): ReminderEventEntity = ReminderEventEntity(
        id = firedAt.toString(),
        reminderId = "reminder",
        titleSnapshot = "Reminder",
        categorySnapshot = "General",
        firedAt = firedAt,
        deliveryResult = "shown",
    )

    private fun at(date: LocalDate, hour: Int, minute: Int): Long =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zoneId).toInstant().toEpochMilli()
}
