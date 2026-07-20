package com.kaemis.healthdesk.data.repository

import com.kaemis.healthdesk.domain.stats.StatsCalculator
import com.kaemis.healthdesk.domain.stats.StatsSnapshot
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class StatsRepository(
    private val focusSessionRepository: FocusSessionRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    fun observeStats(): Flow<StatsSnapshot> {
        val today = Instant.ofEpochMilli(nowProvider()).atZone(ZoneId.systemDefault()).toLocalDate()
        val monthStart = today.withDayOfMonth(1)
        val queryStart = minOf(monthStart, today.minusDays(6))
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return combine(
            focusSessionRepository.observeSessionsFrom(queryStart),
            taskRepository.observeCompletedTasksFrom(queryStart),
            taskRepository.observePendingTasks(),
            reminderRepository.observeEventsFrom(queryStart),
        ) { focusSessions, completedTasks, pendingTasks, reminderEvents ->
            StatsCalculator.calculate(
                focusSessions = focusSessions,
                completedTasks = completedTasks,
                pendingTasks = pendingTasks,
                reminderEvents = reminderEvents,
                nowMillis = nowProvider(),
            )
        }
    }
}
