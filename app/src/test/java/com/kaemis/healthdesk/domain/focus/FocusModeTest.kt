package com.kaemis.healthdesk.domain.focus

import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusModeTest {
    @Test
    fun resolveFocusModeReturnsSelectedCustomMode() {
        val settings = SettingsSnapshot(
            timerMode = "custom-deep-work",
            customFocusModes = listOf(
                CustomFocusMode(
                    id = "custom-deep-work",
                    name = "Deep work",
                    type = POMODORO_MODE_ID,
                    workMinutes = 45,
                    restMinutes = 10,
                    snoozeMinutes = 5,
                    pomodoroCycles = 3,
                    pomodoroShortRestMinutes = 5,
                    pomodoroLongRestMinutes = 15,
                ),
            ),
        )

        val resolved = settings.resolveFocusMode()

        assertEquals("custom-deep-work", resolved.id)
        assertTrue(resolved.isCustom)
        assertEquals(45, resolved.workMinutes)
        assertEquals(3, resolved.totalCycles())
    }

    @Test
    fun resolveFocusModeFallsBackToNormalWhenCustomModeIsMissing() {
        val settings = SettingsSnapshot(timerMode = "missing-custom", workSessionMinutes = 30)

        val resolved = settings.resolveFocusMode()

        assertEquals(NORMAL_TIMER_MODE_ID, resolved.id)
        assertEquals(30, resolved.workMinutes)
        assertTrue(!resolved.isCustom)
    }

    @Test
    fun newCustomFocusModeClonesCurrentModeShape() {
        val settings = SettingsSnapshot(
            timerMode = POMODORO_MODE_ID,
            workSessionMinutes = 40,
            snoozeMinutes = 7,
            pomodoroCycles = 2,
            pomodoroShortRestMinutes = 4,
            pomodoroLongRestMinutes = 12,
        )

        val created = settings.newCustomFocusMode(name = "Team sprint", type = POMODORO_MODE_ID)

        assertEquals("Team sprint", created.name)
        assertEquals(POMODORO_MODE_ID, created.type)
        assertEquals(40, created.workMinutes)
        assertEquals(7, created.snoozeMinutes)
        assertEquals(2, created.pomodoroCycles)
        assertEquals(4, created.pomodoroShortRestMinutes)
        assertEquals(12, created.pomodoroLongRestMinutes)
    }

    @Test
    fun pomodoroRestMinutesUsesLongRestOnLastCycle() {
        val mode = ResolvedFocusMode(
            id = POMODORO_MODE_ID,
            name = "Pomodoro",
            type = POMODORO_MODE_ID,
            workMinutes = 25,
            restMinutes = 5,
            snoozeMinutes = 5,
            pomodoroCycles = 3,
            pomodoroShortRestMinutes = 5,
            pomodoroLongRestMinutes = 15,
            multiCycleCycles = 3,
            isCustom = false,
        )

        assertEquals(5, mode.restMinutesForCycle(1))
        assertEquals(5, mode.restMinutesForCycle(2))
        assertEquals(15, mode.restMinutesForCycle(3))
    }
}
