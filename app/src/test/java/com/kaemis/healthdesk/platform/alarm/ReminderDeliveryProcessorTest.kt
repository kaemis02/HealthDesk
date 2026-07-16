package com.kaemis.healthdesk.platform.alarm

import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import com.kaemis.healthdesk.platform.audio.ReminderFeedbackController
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDeliveryProcessorTest {
    @Test
    fun notificationsDisabledSkipsSystemNotificationButStillPlaysFeedbackAndSchedulesNext() = runTest {
        val reminder = reminder(scheduleMode = "interval", intervalMinutes = 30)
        var savedReminder: ReminderEntity? = null
        var recordedEvent: ReminderEventEntity? = null
        var scheduled: Pair<String, Long>? = null
        var cancelledId: String? = null
        var shownNotification: Pair<String, String>? = null
        val feedback = RecordingReminderFeedbackController()

        ReminderDeliveryProcessor(
            loadSettings = { SettingsSnapshot(notificationsEnabled = false, hapticsEnabled = true) },
            getReminder = { reminder },
            saveReminder = { savedReminder = it },
            recordEvent = { recordedEvent = it },
            scheduleReminder = { id, at -> scheduled = id to at },
            cancelReminder = { cancelledId = it },
            showNotification = { title, id -> shownNotification = title to id },
            feedbackController = feedback,
            nowProvider = { 1_000L },
        ).handle(reminder.id)

        assertEquals(null, shownNotification)
        assertEquals("skipped", recordedEvent?.deliveryResult)
        assertEquals("ring2", feedback.soundKey)
        assertEquals(true, feedback.hapticsEnabled)
        assertEquals(reminder.id, savedReminder?.id)
        assertTrue(savedReminder?.isEnabled == true)
        assertEquals(reminder.id to 1_801_000L, scheduled)
        assertEquals(null, cancelledId)
    }

    @Test
    fun fixedTimeReminderShowsNotificationAndCancelsFutureSchedule() = runTest {
        val reminder = reminder(scheduleMode = "fixedTimeOnce", fixedLocalTime = "09:00")
        var savedReminder: ReminderEntity? = null
        var scheduled = false
        var cancelledId: String? = null
        var shownNotification: Pair<String, String>? = null

        ReminderDeliveryProcessor(
            loadSettings = { SettingsSnapshot(notificationsEnabled = true, hapticsEnabled = false) },
            getReminder = { reminder },
            saveReminder = { savedReminder = it },
            recordEvent = {},
            scheduleReminder = { _, _ -> scheduled = true },
            cancelReminder = { cancelledId = it },
            showNotification = { title, id -> shownNotification = title to id },
            feedbackController = RecordingReminderFeedbackController(),
            nowProvider = { 1_000L },
        ).handle(reminder.id)

        assertEquals("Reminder" to reminder.id, shownNotification)
        assertFalse(savedReminder?.isEnabled ?: true)
        assertEquals(null, savedReminder?.nextScheduledAt)
        assertFalse(scheduled)
        assertEquals(reminder.id, cancelledId)
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

private class RecordingReminderFeedbackController : ReminderFeedbackController {
    var soundKey: String? = null
    var hapticsEnabled: Boolean? = null

    override fun playReminder(soundKey: String, hapticsEnabled: Boolean) {
        this.soundKey = soundKey
        this.hapticsEnabled = hapticsEnabled
    }
}
