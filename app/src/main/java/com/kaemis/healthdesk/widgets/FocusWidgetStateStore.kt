package com.kaemis.healthdesk.widgets

import android.content.Context
import com.kaemis.healthdesk.ui.focus.FocusPhase
import com.kaemis.healthdesk.ui.focus.FocusUiState

data class FocusWidgetState(
    val phase: String = FocusPhase.Idle.name,
    val remainingSeconds: Long = 0,
)

object FocusWidgetStateStore {
    private const val PREFS = "healthdesk_widget_state"
    private const val KEY_PHASE = "phase"
    private const val KEY_REMAINING = "remaining"

    fun save(context: Context, state: FocusUiState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHASE, state.phase.name)
            .putLong(KEY_REMAINING, state.remainingSeconds)
            .apply()
    }

    fun load(context: Context): FocusWidgetState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return FocusWidgetState(
            phase = prefs.getString(KEY_PHASE, FocusPhase.Idle.name) ?: FocusPhase.Idle.name,
            remainingSeconds = prefs.getLong(KEY_REMAINING, 0L),
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
