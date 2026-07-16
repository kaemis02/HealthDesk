package com.kaemis.healthdesk.domain.reminders

import com.kaemis.healthdesk.data.entity.ReminderEntity
import java.time.ZoneId

data class ReminderDeliveryDecision(
    val shouldShowNotification: Boolean,
    val deliveryResult: String,
    val updatedReminder: ReminderEntity,
)

fun deliverReminder(
    reminder: ReminderEntity,
    nowMillis: Long,
    notificationsEnabled: Boolean,
    zoneId: ZoneId = ZoneId.systemDefault(),
): ReminderDeliveryDecision {
    val firedReminder = reminder.copy(lastFiredAt = nowMillis)
    val next = if (reminder.scheduleMode == "fixedTimeOnce") {
        null
    } else {
        ReminderScheduler.nextScheduledAt(firedReminder, nowMillis, zoneId)
    }
    val updatedReminder = firedReminder.copy(
        isEnabled = reminder.scheduleMode != "fixedTimeOnce",
        nextScheduledAt = next,
        updatedAt = nowMillis,
    )
    return ReminderDeliveryDecision(
        shouldShowNotification = notificationsEnabled,
        deliveryResult = if (notificationsEnabled) "shown" else "skipped",
        updatedReminder = updatedReminder,
    )
}
