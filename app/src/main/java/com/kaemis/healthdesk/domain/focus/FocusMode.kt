package com.kaemis.healthdesk.domain.focus

import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import java.util.UUID
import kotlinx.serialization.Serializable

const val NORMAL_TIMER_MODE_ID = "normalTimer"
const val POMODORO_MODE_ID = "pomodoro"
const val MULTI_CYCLE_MODE_ID = "multiCycle"

@Serializable
data class CustomFocusMode(
    val id: String,
    val name: String,
    val type: String = NORMAL_TIMER_MODE_ID,
    val workMinutes: Int = 50,
    val restMinutes: Int = 10,
    val snoozeMinutes: Int = 10,
    val pomodoroCycles: Int = 4,
    val pomodoroShortRestMinutes: Int = 5,
    val pomodoroLongRestMinutes: Int = 15,
    val multiCycleCycles: Int = 3,
)

data class ResolvedFocusMode(
    val id: String,
    val name: String,
    val type: String,
    val workMinutes: Int,
    val restMinutes: Int,
    val snoozeMinutes: Int,
    val pomodoroCycles: Int,
    val pomodoroShortRestMinutes: Int,
    val pomodoroLongRestMinutes: Int,
    val multiCycleCycles: Int,
    val isCustom: Boolean,
) {
    fun totalCycles(): Int = when (type) {
        POMODORO_MODE_ID -> pomodoroCycles.coerceAtLeast(1)
        MULTI_CYCLE_MODE_ID -> multiCycleCycles.coerceAtLeast(1)
        else -> 1
    }

    fun restMinutesForCycle(cycle: Int): Int = when (type) {
        POMODORO_MODE_ID -> if (cycle >= totalCycles()) {
            pomodoroLongRestMinutes.coerceAtLeast(0)
        } else {
            pomodoroShortRestMinutes.coerceAtLeast(0)
        }
        else -> restMinutes.coerceAtLeast(0)
    }
}

fun isBuiltInFocusModeId(modeId: String): Boolean = modeId == NORMAL_TIMER_MODE_ID || modeId == POMODORO_MODE_ID || modeId == MULTI_CYCLE_MODE_ID

fun SettingsSnapshot.resolveFocusMode(): ResolvedFocusMode {
    val custom = customFocusModes.firstOrNull { it.id == timerMode }
    if (custom != null) return custom.toResolvedFocusMode()
    return builtInFocusMode(timerMode)
}

fun SettingsSnapshot.focusModeOptions(): List<ResolvedFocusMode> = listOf(
    builtInFocusMode(NORMAL_TIMER_MODE_ID),
    builtInFocusMode(POMODORO_MODE_ID),
    builtInFocusMode(MULTI_CYCLE_MODE_ID),
) + customFocusModes.map(CustomFocusMode::toResolvedFocusMode)

fun SettingsSnapshot.newCustomFocusMode(name: String, type: String): CustomFocusMode {
    val trimmedName = name.trim().ifBlank { "Custom mode" }
    val base = when (type) {
        POMODORO_MODE_ID -> builtInFocusMode(POMODORO_MODE_ID)
        MULTI_CYCLE_MODE_ID -> builtInFocusMode(MULTI_CYCLE_MODE_ID)
        else -> resolveFocusMode().copy(type = NORMAL_TIMER_MODE_ID)
    }
    return CustomFocusMode(
        id = "custom-${UUID.randomUUID()}",
        name = trimmedName,
        type = type,
        workMinutes = base.workMinutes,
        restMinutes = base.restMinutes,
        snoozeMinutes = base.snoozeMinutes,
        pomodoroCycles = base.pomodoroCycles,
        pomodoroShortRestMinutes = base.pomodoroShortRestMinutes,
        pomodoroLongRestMinutes = base.pomodoroLongRestMinutes,
        multiCycleCycles = base.multiCycleCycles,
    )
}

private fun SettingsSnapshot.builtInFocusMode(modeId: String): ResolvedFocusMode = when (modeId) {
    POMODORO_MODE_ID -> ResolvedFocusMode(
        id = POMODORO_MODE_ID,
        name = "Pomodoro",
        type = POMODORO_MODE_ID,
        workMinutes = pomodoroWorkMinutes,
        restMinutes = restMinutes,
        snoozeMinutes = snoozeMinutes,
        pomodoroCycles = pomodoroCycles,
        pomodoroShortRestMinutes = pomodoroShortRestMinutes,
        pomodoroLongRestMinutes = pomodoroLongRestMinutes,
        multiCycleCycles = multiCycleCycles,
        isCustom = false,
    )
    MULTI_CYCLE_MODE_ID -> ResolvedFocusMode(
        id = MULTI_CYCLE_MODE_ID,
        name = "Multi-cycle",
        type = MULTI_CYCLE_MODE_ID,
        workMinutes = workSessionMinutes,
        restMinutes = restMinutes,
        snoozeMinutes = snoozeMinutes,
        pomodoroCycles = pomodoroCycles,
        pomodoroShortRestMinutes = pomodoroShortRestMinutes,
        pomodoroLongRestMinutes = pomodoroLongRestMinutes,
        multiCycleCycles = multiCycleCycles,
        isCustom = false,
    )
    else -> ResolvedFocusMode(
        id = NORMAL_TIMER_MODE_ID,
        name = "Normal",
        type = NORMAL_TIMER_MODE_ID,
        workMinutes = workSessionMinutes,
        restMinutes = restMinutes,
        snoozeMinutes = snoozeMinutes,
        pomodoroCycles = pomodoroCycles,
        pomodoroShortRestMinutes = pomodoroShortRestMinutes,
        pomodoroLongRestMinutes = pomodoroLongRestMinutes,
        multiCycleCycles = multiCycleCycles,
        isCustom = false,
    )
}

private fun CustomFocusMode.toResolvedFocusMode(): ResolvedFocusMode = ResolvedFocusMode(
    id = id,
    name = name,
    type = type,
    workMinutes = workMinutes,
    restMinutes = restMinutes,
    snoozeMinutes = snoozeMinutes,
    pomodoroCycles = pomodoroCycles,
    pomodoroShortRestMinutes = pomodoroShortRestMinutes,
    pomodoroLongRestMinutes = pomodoroLongRestMinutes,
    multiCycleCycles = multiCycleCycles,
    isCustom = true,
)
