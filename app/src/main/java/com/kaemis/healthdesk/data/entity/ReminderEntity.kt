package com.kaemis.healthdesk.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable wellness reminder scheduled through Android alarms.
 */
@Entity(
    tableName = "reminders",
    indices = [Index("categoryId"), Index("isEnabled"), Index("nextScheduledAt")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
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
