package com.kaemis.healthdesk.data.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSnapshotTest {
    @Test
    fun defaultsToSystemTheme() {
        assertEquals("system", SettingsSnapshot().themeMode)
    }
}
