package com.kaemis.healthdesk.domain.reminders

data class ReminderEvent(
    val id: String,
    val reminderId: String,
    val titleSnapshot: String,
    val categorySnapshot: String,
    val firedAt: Long,
    val deliveryResult: String,
)
