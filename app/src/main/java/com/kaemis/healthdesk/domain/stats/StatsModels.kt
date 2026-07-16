package com.kaemis.healthdesk.domain.stats

data class StatsSnapshot(
    val focusMinutesToday: Long = 0,
    val focusSessionsToday: Int = 0,
    val completedTasksToday: Int = 0,
    val remindersToday: Int = 0,
    val dailyStats: List<StatsDaySnapshot> = emptyList(),
    val recentCompletedTasks: List<CompletedTaskStat> = emptyList(),
)

data class StatsDaySnapshot(
    val date: String,
    val focusMinutes: Long,
    val focusSessions: Int,
    val completedTasks: Int,
    val reminders: Int,
)

data class CompletedTaskStat(
    val title: String,
    val completedAt: Long,
)
