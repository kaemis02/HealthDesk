package com.kaemis.healthdesk.platform.alarm

import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import com.kaemis.healthdesk.domain.reminders.deliverReminder
import com.kaemis.healthdesk.platform.audio.ReminderFeedbackController
import java.util.UUID

class ReminderDeliveryProcessor(
    private val loadSettings: suspend () -> SettingsSnapshot,
    private val getReminder: suspend (String) -> ReminderEntity?,
    private val saveReminder: suspend (ReminderEntity) -> Unit,
    private val recordEvent: suspend (ReminderEventEntity) -> Unit,
    private val scheduleReminder: (String, Long) -> Unit,
    private val cancelReminder: (String) -> Unit,
    private val showNotification: (String, String) -> Unit,
    private val feedbackController: ReminderFeedbackController,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    suspend fun handle(reminderId: String) {
        val settings = loadSettings()
        val reminder = getReminder(reminderId) ?: return
        if (!reminder.isEnabled) return

        val now = nowProvider()
        val delivery = deliverReminder(
            reminder = reminder,
            nowMillis = now,
            notificationsEnabled = settings.notificationsEnabled,
        )
        if (delivery.shouldShowNotification) {
            showNotification(reminder.title, reminderId)
        }
        feedbackController.playReminder(reminder.soundKey, settings.hapticsEnabled)
        recordEvent(
            ReminderEventEntity(
                id = UUID.randomUUID().toString(),
                reminderId = reminder.id,
                titleSnapshot = reminder.title,
                categorySnapshot = reminder.categoryId,
                firedAt = now,
                deliveryResult = delivery.deliveryResult,
            ),
        )
        saveReminder(delivery.updatedReminder)
        if (delivery.updatedReminder.isEnabled && delivery.updatedReminder.nextScheduledAt != null) {
            scheduleReminder(delivery.updatedReminder.id, delivery.updatedReminder.nextScheduledAt)
        } else {
            cancelReminder(delivery.updatedReminder.id)
        }
    }
}
