package com.kaemis.healthdesk.widgets

import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.domain.focus.CustomFocusMode
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthDeskWidgetSnapshotBuilderTest {
    @Test
    fun usesPersonalGreetingForNamedProfile() {
        val snapshot = HealthDeskWidgetSnapshotBuilder.build(
            profile = UserProfileEntity(
                id = "user",
                displayName = "Kae",
                avatarMode = "initials",
                avatarLocalPath = null,
                createdAt = 0L,
                updatedAt = 0L,
            ),
            settings = SettingsSnapshot(),
        )

        assertEquals("Hi, Kae", snapshot.title)
        assertEquals("Mode: Normal", snapshot.subtitle)
    }

    @Test
    fun usesCustomModeNameWhenSelected() {
        val snapshot = HealthDeskWidgetSnapshotBuilder.build(
            profile = null,
            settings = SettingsSnapshot(
                timerMode = "custom-deep-work",
                customFocusModes = listOf(
                    CustomFocusMode(id = "custom-deep-work", name = "Deep work"),
                ),
            ),
        )

        assertEquals("HealthDesk", snapshot.title)
        assertEquals("Mode: Deep work", snapshot.subtitle)
    }
}
