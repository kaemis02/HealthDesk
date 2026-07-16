package com.kaemis.healthdesk.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Simple local task. MVP tasks deliberately avoid priority, due dates and recurrence.
 */
@Entity(
    tableName = "tasks",
    indices = [Index("categoryId"), Index("isCompleted"), Index("sortOrder")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val categoryId: String?,
    val isCompleted: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
)
