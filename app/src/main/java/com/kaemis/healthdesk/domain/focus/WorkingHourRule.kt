package com.kaemis.healthdesk.domain.focus

data class WorkingHourRule(
    val id: String,
    val dayOfWeek: Int,
    val isEnabled: Boolean,
    val startLocalTime: String,
    val endLocalTime: String,
    val sortOrder: Int,
    val updatedAt: Long,
)
