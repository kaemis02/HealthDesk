package com.kaemis.healthdesk.domain.focus

data class FocusSession(
    val id: String,
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
