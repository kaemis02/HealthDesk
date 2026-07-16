package com.kaemis.healthdesk.platform.service

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class FocusActionCommand {
    StartFocus,
    Pause,
    Resume,
    Stop,
    StopAlarm,
    Snooze,
    PhaseElapsed,
    WorkdayEnded,
}

/**
 * Keeps focus commands until the state machine confirms them, so widget and
 * notification actions survive a temporarily unavailable Compose ViewModel.
 */
class FocusActionBus(context: Context) {
    private val preferences = context.getSharedPreferences("healthdesk_focus_actions", Context.MODE_PRIVATE)
    private val mutableCommands = MutableSharedFlow<FocusActionCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<FocusActionCommand> = mutableCommands.asSharedFlow()

    fun send(command: FocusActionCommand) {
        enqueue(command)
        mutableCommands.tryEmit(command)
    }

    @Synchronized
    fun pendingCommands(): List<FocusActionCommand> = preferences.getString(KEY_PENDING, "")
        .orEmpty()
        .split(',')
        .mapNotNull { value -> FocusActionCommand.entries.firstOrNull { it.name == value } }

    @Synchronized
    fun acknowledge(command: FocusActionCommand) {
        val commands = pendingCommands().toMutableList()
        commands.remove(command)
        persist(commands)
    }

    fun clear() = preferences.edit().clear().apply()

    @Synchronized
    private fun enqueue(command: FocusActionCommand) {
        val commands = pendingCommands().toMutableList().apply { add(command) }
        persist(commands)
    }

    private fun persist(commands: List<FocusActionCommand>) {
        preferences.edit().putString(KEY_PENDING, commands.joinToString(",") { it.name }).commit()
    }

    private companion object {
        const val KEY_PENDING = "pending"
    }
}
