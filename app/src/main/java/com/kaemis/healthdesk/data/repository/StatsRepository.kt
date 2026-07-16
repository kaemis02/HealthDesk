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
        val monthStart = Instant.ofEpochMilli(nowProvider())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return combine(
            focusSessionRepository.observeSessionsFrom(monthStart),
            taskRepository.observeCompletedTasksFrom(monthStart),
            taskRepository.observePendingTasks(),
            reminderRepository.observeEventsFrom(monthStart),
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
