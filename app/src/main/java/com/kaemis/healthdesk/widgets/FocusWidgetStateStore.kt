package com.kaemis.healthdesk.widgets

import android.content.Context
import com.kaemis.healthdesk.ui.focus.FocusPhase
import com.kaemis.healthdesk.ui.focus.FocusUiState

data class FocusWidgetState(
    val phase: String = FocusPhase.Idle.name,
    val remainingSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val activeSessionId: String? = null,
    val currentCycle: Int = 1,
    val totalCycles: Int = 1,
)

object FocusWidgetStateStore {
    private const val PREFS = "healthdesk_widget_state"
    private const val KEY_PHASE = "phase"
    private const val KEY_REMAINING = "remaining"
    private const val KEY_TOTAL = "total"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_CURRENT_CYCLE = "current_cycle"
    private const val KEY_TOTAL_CYCLES = "total_cycles"

    fun save(context: Context, state: FocusUiState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHASE, state.phase.name)
            .putLong(KEY_REMAINING, state.remainingSeconds)
            .putLong(KEY_TOTAL, state.totalSeconds)
            .putString(KEY_SESSION_ID, state.activeSessionId)
            .putInt(KEY_CURRENT_CYCLE, state.currentCycle)
            .putInt(KEY_TOTAL_CYCLES, state.totalCycles)
            .apply()
    }

    fun load(context: Context): FocusWidgetState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return FocusWidgetState(
            phase = prefs.getString(KEY_PHASE, FocusPhase.Idle.name) ?: FocusPhase.Idle.name,
            remainingSeconds = prefs.getLong(KEY_REMAINING, 0L),
            totalSeconds = prefs.getLong(KEY_TOTAL, 0L),
            activeSessionId = prefs.getString(KEY_SESSION_ID, null),
            currentCycle = prefs.getInt(KEY_CURRENT_CYCLE, 1),
            totalCycles = prefs.getInt(KEY_TOTAL_CYCLES, 1),
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
