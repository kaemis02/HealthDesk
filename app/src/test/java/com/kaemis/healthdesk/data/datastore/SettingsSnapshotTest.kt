package com.kaemis.healthdesk.data.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSnapshotTest {
    @Test
    fun defaultsToSystemTheme() {
        assertEquals("system", SettingsSnapshot().themeMode)
    }

    @Test
    fun enablesWorkdayNoticesAndDisablesOutOfOfficeByDefault() {
        val settings = SettingsSnapshot()

        assertEquals(true, settings.workdayNotificationsEnabled)
        assertEquals(false, settings.outOfOffice)
    }

    @Test
    fun showsTutorialUntilTheUserCompletesIt() {
        assertEquals(false, SettingsSnapshot().tutorialCompleted)
        assertEquals(true, SettingsSnapshot(tutorialCompleted = true).tutorialCompleted)
    }
}
