package com.kaemis.healthdesk.data.defaults

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDataTest {
    @Test
    fun profileUsesUserInitialsByDefault() {
        val profile = DefaultData.profile(now = 1000L)

        assertEquals("User", profile.displayName)
        assertEquals("initials", profile.avatarMode)
        assertEquals(null, profile.avatarLocalPath)
    }

    @Test
    fun workingHoursDefaultToWeekdaysWithShortFriday() {
        val rules = DefaultData.workingHourRules(now = 1000L)

        assertEquals(7, rules.size)
        assertTrue(rules.first { it.dayOfWeek == 1 }.isEnabled)
        assertEquals("18:00", rules.first { it.dayOfWeek == 4 }.endLocalTime)
        assertEquals("15:00", rules.first { it.dayOfWeek == 5 }.endLocalTime)
        assertFalse(rules.first { it.dayOfWeek == 6 }.isEnabled)
        assertFalse(rules.first { it.dayOfWeek == 7 }.isEnabled)
    }

    @Test
    fun waterReminderStartsDisabledWithSixtyMinuteInterval() {
        val waterReminder = DefaultData.waterReminder(now = 1000L)

        assertEquals("water", waterReminder.kind)
        assertFalse(waterReminder.isEnabled)
        assertEquals("interval", waterReminder.scheduleMode)
        assertEquals(60, waterReminder.intervalMinutes)
        assertEquals("ring2", waterReminder.soundKey)
    }

    @Test
    fun taskAndReminderCategoriesIncludeGeneral() {
        val taskCategories = DefaultData.taskCategories(now = 1000L)
        val reminderCategories = DefaultData.reminderCategories(now = 1000L)

        assertTrue(taskCategories.any { it.scope == "task" && it.name == "General" })
        assertTrue(reminderCategories.any { it.scope == "reminder" && it.name == "General" })
    }
}
