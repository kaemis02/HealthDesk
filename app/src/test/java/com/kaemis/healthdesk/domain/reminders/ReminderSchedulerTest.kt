package com.kaemis.healthdesk.domain.reminders

import com.kaemis.healthdesk.data.entity.ReminderEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSchedulerTest {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    @Test
    fun intervalUsesLastFiredWhenFuture() {
        val now = at(2026, 7, 13, 10, 0)
        val reminder = reminder(scheduleMode = "interval", intervalMinutes = 30, lastFiredAt = at(2026, 7, 13, 9, 45))

        assertEquals(at(2026, 7, 13, 10, 15), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun intervalSkipsPastOccurrenceOnResync() {
        val now = at(2026, 7, 13, 10, 0)
        val reminder = reminder(scheduleMode = "interval", intervalMinutes = 30, lastFiredAt = at(2026, 7, 13, 8, 0))

        assertEquals(at(2026, 7, 13, 10, 30), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun fixedTimeOnceRollsToTomorrowWhenTimePassed() {
        val now = at(2026, 7, 13, 10, 0)
        val reminder = reminder(scheduleMode = "fixedTimeOnce", fixedLocalTime = "09:00")

        assertEquals(at(2026, 7, 14, 9, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun fixedTimeOnceReturnsTodayWhenFuture() {
        val now = at(2026, 7, 13, 10, 0)
        val reminder = reminder(scheduleMode = "fixedTimeOnce", fixedLocalTime = "11:00")

        assertEquals(at(2026, 7, 13, 11, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun dailyRecurringFindsNextFutureOccurrence() {
        val now = at(2026, 7, 13, 10, 0)
        val reminder = reminder(scheduleMode = "recurring", fixedLocalTime = "09:00", recurrenceUnit = "daily", recurrenceInterval = 1)

        assertEquals(at(2026, 7, 14, 9, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun weeklyRecurringRequiresSelectedDay() {
        val now = at(2026, 7, 13, 10, 0)
        val reminder = reminder(scheduleMode = "recurring", fixedLocalTime = "12:00", recurrenceUnit = "weekly", recurrenceInterval = 1, recurrenceWeekdays = "1,3")

        assertEquals(at(2026, 7, 13, 12, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun weeklyRecurringWithoutDaysIsInvalid() {
        val now = at(2026, 7, 13, 10, 0)
        val reminder = reminder(scheduleMode = "recurring", fixedLocalTime = "12:00", recurrenceUnit = "weekly", recurrenceInterval = 1)

        assertNull(ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun monthlyByDayUsesLastValidDayForShortMonths() {
        val now = at(2026, 2, 1, 10, 0)
        val reminder = reminder(scheduleMode = "recurring", fixedLocalTime = "12:00", recurrenceUnit = "monthlyDay", recurrenceInterval = 1, recurrenceDayOfMonth = 31)

        assertEquals(at(2026, 2, 28, 12, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun monthlyMultipleDaysUsesTheFirstFutureSelectedDay() {
        val now = at(2026, 7, 13, 10, 0)
        val reminder = reminder(
            scheduleMode = "recurring",
            fixedLocalTime = "12:00",
            recurrenceUnit = "monthly",
            recurrenceInterval = 1,
            recurrenceWeekdays = "10,15,31",
        )

        assertEquals(at(2026, 7, 15, 12, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun monthlyMultipleDaysClampsThirtyFirstToFebruaryLastDay() {
        val now = at(2026, 2, 1, 10, 0)
        val reminder = reminder(
            scheduleMode = "recurring",
            fixedLocalTime = "12:00",
            recurrenceUnit = "monthly",
            recurrenceInterval = 1,
            recurrenceWeekdays = "29,31",
        )

        assertEquals(at(2026, 2, 28, 12, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun monthlyNthWeekdayFindsSecondMonday() {
        val now = at(2026, 7, 1, 10, 0)
        val reminder = reminder(scheduleMode = "recurring", fixedLocalTime = "09:00", recurrenceUnit = "monthlyWeekday", recurrenceInterval = 1, recurrenceWeekdays = "1", recurrenceDayOfMonth = 2)

        assertEquals(at(2026, 7, 13, 9, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun monthlyLastWeekdayFindsLastFriday() {
        val now = at(2026, 7, 1, 10, 0)
        val reminder = reminder(scheduleMode = "recurring", fixedLocalTime = "09:00", recurrenceUnit = "monthlyWeekday", recurrenceInterval = 1, recurrenceWeekdays = "5", recurrenceDayOfMonth = -1)

        assertEquals(at(2026, 7, 31, 9, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    @Test
    fun yearlyRecurringHandlesShortMonthDay() {
        val now = at(2026, 1, 1, 10, 0)
        val reminder = reminder(scheduleMode = "recurring", fixedLocalTime = "09:00", recurrenceUnit = "yearly", recurrenceInterval = 1, recurrenceMonth = 2, recurrenceDay = 31)

        assertEquals(at(2026, 2, 28, 9, 0), ReminderScheduler.nextScheduledAt(reminder, now, zoneId))
    }

    private fun reminder(
        scheduleMode: String,
        intervalMinutes: Int? = null,
        fixedLocalTime: String? = null,
        recurrenceUnit: String? = null,
        recurrenceInterval: Int? = null,
        recurrenceWeekdays: String? = null,
        recurrenceDayOfMonth: Int? = null,
        recurrenceMonth: Int? = null,
        recurrenceDay: Int? = null,
        lastFiredAt: Long? = null,
    ): ReminderEntity = ReminderEntity(
        id = "reminder",
        kind = "custom",
        title = "Reminder",
        categoryId = "reminder-general",
        isEnabled = true,
        scheduleMode = scheduleMode,
        intervalMinutes = intervalMinutes,
        fixedLocalTime = fixedLocalTime,
        recurrenceUnit = recurrenceUnit,
        recurrenceInterval = recurrenceInterval,
        recurrenceWeekdays = recurrenceWeekdays,
        recurrenceDayOfMonth = recurrenceDayOfMonth,
        recurrenceMonth = recurrenceMonth,
        recurrenceDay = recurrenceDay,
        soundKey = "ring2",
        iconKey = "notifications",
        lastFiredAt = lastFiredAt,
        nextScheduledAt = null,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
}
