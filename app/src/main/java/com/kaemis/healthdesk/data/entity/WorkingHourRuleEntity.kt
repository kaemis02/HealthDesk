package com.kaemis.healthdesk.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One enabled/disabled working-hours rule per weekday.
 *
 * Times are stored as local `HH:mm` strings to avoid timezone conversion for wall-clock schedules.
 */
@Entity(
    tableName = "working_hour_rules",
    indices = [Index(value = ["dayOfWeek"], unique = true), Index("sortOrder")],
)
data class WorkingHourRuleEntity(
    @PrimaryKey val id: String,
    val dayOfWeek: Int,
    val isEnabled: Boolean,
    val startLocalTime: String,
    val endLocalTime: String,
    val sortOrder: Int,
    val updatedAt: Long,
)
