package com.kaemis.healthdesk.domain.reminders

data class Reminder(
    val id: String,
    val kind: String,
    val title: String,
    val categoryId: String,
    val isEnabled: Boolean,
    val scheduleMode: String,
    val intervalMinutes: Int?,
    val fixedLocalTime: String?,
    val recurrenceUnit: String?,
    val recurrenceInterval: Int?,
    val recurrenceWeekdays: String?,
    val recurrenceDayOfMonth: Int?,
    val recurrenceMonth: Int?,
    val recurrenceDay: Int?,
    val soundKey: String,
    val iconKey: String,
    val lastFiredAt: Long?,
    val nextScheduledAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
