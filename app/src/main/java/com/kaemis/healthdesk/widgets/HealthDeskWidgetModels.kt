package com.kaemis.healthdesk.widgets

import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.domain.focus.resolveFocusMode

data class HealthDeskWidgetSnapshot(
    val title: String,
    val subtitle: String,
    val accentHex: String,
    val primaryAction: String,
    val secondaryAction: String?,
    val primaryActionCommand: String,
    val secondaryActionCommand: String?,
)

object HealthDeskWidgetSnapshotBuilder {
    fun build(profile: UserProfileEntity?, settings: SettingsSnapshot, focusState: FocusWidgetState = FocusWidgetState()): HealthDeskWidgetSnapshot {
        val focusMode = settings.resolveFocusMode()
        val title = profile?.displayName
            ?.takeIf { it.isNotBlank() && it != "User" }
            ?.let { "Hi, $it" }
            ?: "HealthDesk"
        val subtitle = if (focusState.phase == "Idle" || focusState.phase == "Completed" || focusState.phase == "Stopped") {
            "Mode: ${focusMode.name}"
        } else {
            "${formatWidgetTime(focusState.remainingSeconds)} - ${focusState.phase}"
        }
        val primaryAction = when (focusState.phase) {
            "FocusRunning", "RestRunning", "Snoozed" -> "Stop"
            "FocusAlarm", "RestAlarm" -> "Stop alarm"
            else -> "Start"
        }
        val primaryCommand = when (focusState.phase) {
            "FocusRunning", "RestRunning", "Snoozed" -> "com.kaemis.healthdesk.focus.action.STOP"
            "FocusAlarm", "RestAlarm" -> "com.kaemis.healthdesk.focus.action.STOP_ALARM"
            else -> "com.kaemis.healthdesk.focus.action.START_FOCUS"
        }
        val secondaryAction = when (focusState.phase) {
            "FocusAlarm", "RestAlarm" -> "Snooze"
            "FocusRunning", "RestRunning" -> "Pause"
            "FocusPaused", "RestPaused" -> "Resume"
            else -> null
        }
        val secondaryCommand = when (focusState.phase) {
            "FocusAlarm", "RestAlarm" -> "com.kaemis.healthdesk.focus.action.SNOOZE"
            "FocusRunning", "RestRunning" -> "com.kaemis.healthdesk.focus.action.PAUSE"
            "FocusPaused", "RestPaused" -> "com.kaemis.healthdesk.focus.action.RESUME"
            else -> null
        }
        return HealthDeskWidgetSnapshot(title, subtitle, settings.accentKey, primaryAction, secondaryAction, primaryCommand, secondaryCommand)
    }

    private fun formatWidgetTime(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
}
