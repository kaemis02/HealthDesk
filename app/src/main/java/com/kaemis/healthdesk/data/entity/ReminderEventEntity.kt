package com.kaemis.healthdesk.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable reminder delivery event used by local stats and backup.
 */
@Entity(
    tableName = "reminder_events",
    indices = [Index("reminderId"), Index("firedAt")],
)
data class ReminderEventEntity(
    @PrimaryKey val id: String,
    val reminderId: String,
    val titleSnapshot: String,
    val categorySnapshot: String,
    val firedAt: Long,
    val deliveryResult: String,
)
