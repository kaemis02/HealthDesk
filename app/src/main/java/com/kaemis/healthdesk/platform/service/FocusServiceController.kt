package com.kaemis.healthdesk.platform.service

import android.content.Context
import android.app.NotificationManager
import androidx.core.content.ContextCompat
import com.kaemis.healthdesk.ui.focus.FocusPhase
import com.kaemis.healthdesk.ui.focus.FocusUiState
import com.kaemis.healthdesk.widgets.FocusWidgetStateStore
import com.kaemis.healthdesk.widgets.HealthDeskWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface FocusServiceController {
    fun update(state: FocusUiState)

    object NoOp : FocusServiceController {
        override fun update(state: FocusUiState) = Unit
    }
}

class AndroidFocusServiceController(
    private val context: Context,
) : FocusServiceController {
    override fun update(state: FocusUiState) {
        FocusWidgetStateStore.save(context, state)
        CoroutineScope(Dispatchers.Default).launch {
            HealthDeskWidgetUpdater.updateAll(context)
        }
        if (!state.phase.requiresForegroundService()) {
            context.stopService(FocusForegroundService.stopIntent(context))
            context.getSystemService(NotificationManager::class.java).apply {
                cancel(com.kaemis.healthdesk.platform.notification.HealthDeskNotificationChannels.FOCUS_SESSION_NOTIFICATION_ID)
                cancel(com.kaemis.healthdesk.platform.notification.HealthDeskNotificationChannels.FOCUS_ALARM_NOTIFICATION_ID)
            }
            return
        }

        ContextCompat.startForegroundService(
            context,
            FocusForegroundService.updateIntent(
                context = context,
                phaseLabel = state.phase.labelForNotification(),
                remainingLabel = formatDuration(state.remainingSeconds),
                isAlarm = state.phase == FocusPhase.FocusAlarm || state.phase == FocusPhase.RestAlarm,
                isPaused = state.phase == FocusPhase.FocusPaused || state.phase == FocusPhase.RestPaused,
                isRestAlarm = state.phase == FocusPhase.RestAlarm,
            ),
        )
    }

    private fun FocusPhase.requiresForegroundService(): Boolean = when (this) {
        FocusPhase.FocusRunning,
        FocusPhase.FocusPaused,
        FocusPhase.FocusAlarm,
        FocusPhase.RestRunning,
        FocusPhase.RestPaused,
        FocusPhase.RestAlarm,
        FocusPhase.Snoozed,
        -> true
        FocusPhase.Idle,
        FocusPhase.Completed,
        FocusPhase.Stopped,
        -> false
    }

    private fun FocusPhase.labelForNotification(): String = when (this) {
        FocusPhase.FocusRunning -> "Focus running"
        FocusPhase.FocusPaused -> "Focus paused"
        FocusPhase.FocusAlarm -> "Focus complete"
        FocusPhase.RestRunning -> "Rest running"
        FocusPhase.RestPaused -> "Rest paused"
        FocusPhase.RestAlarm -> "Rest complete"
        FocusPhase.Snoozed -> "Snoozed"
        FocusPhase.Idle -> "Ready"
        FocusPhase.Completed -> "Completed"
        FocusPhase.Stopped -> "Stopped"
    }

    private fun formatDuration(seconds: Long): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        return "%02d:%02d".format(safeSeconds / 60, safeSeconds % 60)
    }
}
