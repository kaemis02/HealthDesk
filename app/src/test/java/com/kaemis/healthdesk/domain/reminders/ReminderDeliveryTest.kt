package com.kaemis.healthdesk.domain.reminders

import com.kaemis.healthdesk.data.entity.ReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDeliveryTest {
    @Test
    fun fixedTimeReminderDisablesAfterFire() {
        val reminder = reminder(scheduleMode = "fixedTimeOnce", fixedLocalTime = "09:00")

        val decision = deliverReminder(reminder, nowMillis = 1_000L, notificationsEnabled = true)

        assertTrue(decision.shouldShowNotification)
        assertEquals("shown", decision.deliveryResult)
        assertFalse(decision.updatedReminder.isEnabled)
        assertEquals(null, decision.updatedReminder.nextScheduledAt)
    }

    @Test
    fun notificationsDisabledMarksReminderAsSkippedButKeepsScheduling() {
        val reminder = reminder(scheduleMode = "interval", intervalMinutes = 30)

        val decision = deliverReminder(reminder, nowMillis = 1_000L, notificationsEnabled = false)

        assertFalse(decision.shouldShowNotification)
        assertEquals("skipped", decision.deliveryResult)
        assertTrue(decision.updatedReminder.isEnabled)
        assertEquals(1_801_000L, decision.updatedReminder.nextScheduledAt)
    }

    private fun reminder(
        scheduleMode: String,
        intervalMinutes: Int? = null,
        fixedLocalTime: String? = null,
    ) = ReminderEntity(
        id = "reminder",
        kind = "custom",
        title = "Reminder",
        categoryId = "reminder-general",
        isEnabled = true,
        scheduleMode = scheduleMode,
        intervalMinutes = intervalMinutes,
        fixedLocalTime = fixedLocalTime,
        recurrenceUnit = null,
        recurrenceInterval = null,
        recurrenceWeekdays = null,
        recurrenceDayOfMonth = null,
        recurrenceMonth = null,
        recurrenceDay = null,
        soundKey = "ring2",
        iconKey = "notifications",
        lastFiredAt = null,
        nextScheduledAt = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
