package com.kaemis.healthdesk.domain.tasks

data class TaskItem(
    val id: String,
    val title: String,
    val categoryId: String?,
    val isCompleted: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
)
