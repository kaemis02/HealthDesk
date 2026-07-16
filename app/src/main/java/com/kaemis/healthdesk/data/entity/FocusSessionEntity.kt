package com.kaemis.healthdesk.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent focus session history and active-session record.
 */
@Entity(
    tableName = "focus_sessions",
    indices = [Index("startedAt"), Index("status"), Index("endReason")],
)
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val plannedDurationMinutes: Int,
    val actualFocusSeconds: Long,
    val plannedRestMinutes: Int,
    val actualRestSeconds: Long,
    val status: String,
    val endReason: String?,
    val snoozeCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
