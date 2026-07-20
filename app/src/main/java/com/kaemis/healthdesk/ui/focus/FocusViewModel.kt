package com.kaemis.healthdesk.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import com.kaemis.healthdesk.data.repository.FocusSessionRepository
import com.kaemis.healthdesk.data.repository.WorkingHoursRepository
import com.kaemis.healthdesk.domain.focus.resolveFocusMode
import com.kaemis.healthdesk.domain.focus.POMODORO_MODE_ID
import com.kaemis.healthdesk.domain.focus.MULTI_CYCLE_MODE_ID
import com.kaemis.healthdesk.platform.alarm.FocusAlarmScheduler
import com.kaemis.healthdesk.platform.audio.AlarmFeedbackController
import com.kaemis.healthdesk.platform.service.FocusActionCommand
import com.kaemis.healthdesk.platform.service.FocusServiceController
import java.util.UUID
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

enum class FocusPhase {
    Idle,
    FocusRunning,
    FocusPaused,
    FocusAlarm,
    RestRunning,
    RestPaused,
    RestAlarm,
    Snoozed,
    Completed,
    Stopped,
}

data class FocusUiState(
    val phase: FocusPhase = FocusPhase.Idle,
    val remainingSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val timerMode: String = "normalTimer",
    val workSessionMinutes: Int = 50,
    val restMinutes: Int = 10,
    val snoozeMinutes: Int = 10,
    val currentCycle: Int = 1,
    val totalCycles: Int = 1,
    val activeSessionId: String? = null,
    val actualFocusSeconds: Long = 0,
    val actualRestSeconds: Long = 0,
    val snoozeCount: Int = 0,
    val pendingOutsideWorkingHoursConfirmation: Boolean = false,
)

data class FocusCompletionEvent(val modeName: String)

class FocusViewModel(
    private val settingsFlow: Flow<SettingsSnapshot>,
    private val focusSessionRepository: FocusSessionRepository,
    private val workingHoursRepository: WorkingHoursRepository,
    private val focusServiceController: FocusServiceController = FocusServiceController.NoOp,
    private val commandsFlow: Flow<FocusActionCommand> = emptyFlow(),
    private val runtimeStateProvider: () -> FocusUiState? = { null },
    private val pendingCommands: () -> List<FocusActionCommand> = { emptyList() },
    private val acknowledgeCommand: (FocusActionCommand) -> Unit = {},
    private val focusAlarmScheduler: FocusAlarmScheduler = FocusAlarmScheduler.NoOp,
    private val alarmFeedbackController: AlarmFeedbackController = AlarmFeedbackController.NoOp,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FocusUiState())
    val state: StateFlow<FocusUiState> = mutableState
    private val mutableCompletionEvents = MutableSharedFlow<FocusCompletionEvent>(extraBufferCapacity = 1)
    val completionEvents = mutableCompletionEvents.asSharedFlow()

    val dashboardSummary: StateFlow<FocusDashboardSummary> = combine(
        mutableState,
        focusSessionRepository.observeSessionsFrom(startOfTodayMillis()),
    ) { state, sessions ->
        FocusDashboardSummary(
            activePhase = state.phase,
            activeRemainingSeconds = state.remainingSeconds,
            activeSessionId = state.activeSessionId,
            completedFocusSeconds = sessions.sumOf { it.actualFocusSeconds },
            completedSessions = sessions.count { it.status == "completed" },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusDashboardSummary(),
    )

    private var settings = SettingsSnapshot()
    private var workingHourRules: List<WorkingHourRuleEntity> = emptyList()
    private var session: FocusSessionEntity? = null
    private var phaseStartedAt = 0L
    private var phaseEndAt = 0L
    private var phaseBaseFocusSeconds = 0L
    private var phaseBaseRestSeconds = 0L
    private var currentCycle = 1
    private var totalCycles = 1
    private var activeModeType = "normalTimer"
    private var activeModeName = "Normal"
    // A confirmed outside-hours session is valid; being outside is not itself a workday-end event.
    private var allowOutsideWorkingHoursForSession = false
    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            settingsFlow.collect { snapshot ->
                val outOfOfficeWasEnabled = settings.outOfOffice
                settings = snapshot
                val resolvedMode = snapshot.resolveFocusMode()
                mutableState.update {
                    it.copy(
                        timerMode = snapshot.timerMode,
                        workSessionMinutes = resolvedMode.workMinutes,
                        restMinutes = resolvedMode.restMinutes,
                        snoozeMinutes = resolvedMode.snoozeMinutes,
                        currentCycle = currentCycle,
                        totalCycles = resolvedMode.totalCycles(),
                    )
                }
                if (!outOfOfficeWasEnabled && snapshot.outOfOffice) {
                    endForOutOfOffice()
                }
            }
        }
        viewModelScope.launch {
            workingHoursRepository.observeRules().collect { workingHourRules = it }
        }
        viewModelScope.launch {
            restoreActiveSession()
            pendingCommands().forEach { command ->
                handleExternalCommand(command)
                acknowledgeCommand(command)
            }
            commandsFlow.collect { command ->
                handleExternalCommand(command)
                acknowledgeCommand(command)
            }
        }
    }

    private suspend fun restoreActiveSession() {
        // A user action may have created a new runtime phase while Room was loading.
        if (mutableState.value.phase != FocusPhase.Idle) return
        val activeSession = focusSessionRepository.observeActiveSession().first() ?: return
        val runtime = runtimeStateProvider() ?: return
        if (runtime.activeSessionId != activeSession.id || runtime.phase == FocusPhase.Idle) return
        if (mutableState.value.phase != FocusPhase.Idle) return

        session = activeSession
        currentCycle = runtime.currentCycle
        totalCycles = runtime.totalCycles
        mutableState.value = runtime.copy(
            actualFocusSeconds = activeSession.actualFocusSeconds,
            actualRestSeconds = activeSession.actualRestSeconds,
            snoozeCount = activeSession.snoozeCount,
            pendingOutsideWorkingHoursConfirmation = false,
        )
        when (runtime.phase) {
            FocusPhase.FocusAlarm,
            FocusPhase.RestAlarm,
            -> {
                mutableState.update { it.copy(remainingSeconds = 0) }
                focusServiceController.update(mutableState.value)
                alarmFeedbackController.startAlarm(settings.alarmSoundKey, settings.hapticsEnabled)
            }
            FocusPhase.FocusRunning,
            FocusPhase.RestRunning,
            FocusPhase.Snoozed,
            -> startPhase(runtime.phase, runtime.remainingSeconds.coerceAtLeast(1))
            FocusPhase.FocusPaused,
            FocusPhase.RestPaused,
            -> {
                focusServiceController.update(mutableState.value)
                startTicker(checkOnly = true)
            }
            else -> Unit
        }
    }

    private fun handleExternalCommand(command: FocusActionCommand) {
        when (command) {
            FocusActionCommand.StartFocus -> {
                if (mutableState.value.phase == FocusPhase.RestAlarm) stopAlarm()
                start()
            }
            FocusActionCommand.Pause -> pause()
            FocusActionCommand.Resume -> resume()
            FocusActionCommand.Stop -> stop()
            FocusActionCommand.StopAlarm -> stopAlarm()
            FocusActionCommand.Snooze -> snooze()
            FocusActionCommand.PhaseElapsed -> expireCurrentPhaseFromAlarm()
            FocusActionCommand.WorkdayEnded -> endForWorkday()
        }
    }

    private fun expireCurrentPhaseFromAlarm() {
        when (mutableState.value.phase) {
            FocusPhase.FocusRunning,
            FocusPhase.RestRunning,
            FocusPhase.Snoozed,
            -> handlePhaseElapsed()
            else -> Unit
        }
    }

    private fun endForWorkday() {
        val phase = mutableState.value.phase
        if (phase == FocusPhase.Idle || phase == FocusPhase.Completed || phase == FocusPhase.Stopped) return
        updateElapsedForCurrentPhase()
        finishSession(status = "completed", endReason = "workdayEnded", targetPhase = FocusPhase.Completed)
    }

    private fun endForOutOfOffice() {
        val phase = mutableState.value.phase
        if (phase == FocusPhase.Idle || phase == FocusPhase.Completed || phase == FocusPhase.Stopped) return
        updateElapsedForCurrentPhase()
        finishSession(status = "stopped", endReason = "outOfOffice", targetPhase = FocusPhase.Stopped)
    }

    fun start() {
        if (mutableState.value.phase != FocusPhase.Idle && mutableState.value.phase != FocusPhase.Completed && mutableState.value.phase != FocusPhase.Stopped) {
            resume()
            return
        }
        if (settings.outOfOffice) return
        if (settings.workingHoursEnabled && workingHourRules.isNotEmpty() && !isInsideWorkingHours()) {
            mutableState.update { it.copy(pendingOutsideWorkingHoursConfirmation = true) }
            return
        }
        startNewSession(allowOutsideWorkingHours = false)
    }

    fun startConfirmed() {
        if (settings.outOfOffice) return
        startNewSession(allowOutsideWorkingHours = true)
    }

    private fun startNewSession(allowOutsideWorkingHours: Boolean) {
        alarmFeedbackController.stopAlarm()
        allowOutsideWorkingHoursForSession = allowOutsideWorkingHours
        mutableState.update { it.copy(pendingOutsideWorkingHoursConfirmation = false) }
        val resolvedMode = settings.resolveFocusMode()
        activeModeType = resolvedMode.type
        activeModeName = resolvedMode.name
        val now = nowProvider()
        currentCycle = 1
        totalCycles = resolvedMode.totalCycles()
        val newSession = FocusSessionEntity(
            id = UUID.randomUUID().toString(),
            startedAt = now,
            endedAt = null,
            plannedDurationMinutes = resolvedMode.workMinutes,
            actualFocusSeconds = 0,
            plannedRestMinutes = restMinutesForCurrentCycle(),
            actualRestSeconds = 0,
            status = "active",
            endReason = null,
            snoozeCount = 0,
            createdAt = now,
            updatedAt = now,
        )
        session = newSession
        viewModelScope.launch { focusSessionRepository.saveSession(newSession) }
        startPhase(FocusPhase.FocusRunning, resolvedMode.workMinutes * 60L)
    }

    fun cancelOutsideWorkingHoursStart() {
        mutableState.update { it.copy(pendingOutsideWorkingHoursConfirmation = false) }
    }

    fun pause() {
        when (mutableState.value.phase) {
            FocusPhase.FocusRunning -> pauseRunningPhase(FocusPhase.FocusPaused)
            FocusPhase.RestRunning -> pauseRunningPhase(FocusPhase.RestPaused)
            FocusPhase.Snoozed -> pauseRunningPhase(FocusPhase.FocusPaused)
            else -> Unit
        }
    }

    fun resume() {
        when (mutableState.value.phase) {
            FocusPhase.FocusPaused -> startPhase(FocusPhase.FocusRunning, mutableState.value.remainingSeconds)
            FocusPhase.RestPaused -> startPhase(FocusPhase.RestRunning, mutableState.value.remainingSeconds)
            else -> Unit
        }
    }

    /** Advances a paused block without treating the skipped remaining time as completed work or rest. */
    fun next() {
        when (mutableState.value.phase) {
            FocusPhase.FocusPaused -> {
                focusAlarmScheduler.cancelPhaseEnd()
                val restMinutes = restMinutesForCurrentCycle()
                if (restMinutes > 0) startPhase(FocusPhase.RestRunning, restMinutes * 60L)
                else completeOrStartNextCycle(endReason = "timerElapsed")
            }
            FocusPhase.RestPaused -> {
                focusAlarmScheduler.cancelPhaseEnd()
                completeOrStartNextCycle(endReason = "restElapsed")
            }
            else -> Unit
        }
    }

    fun stop() {
        val current = mutableState.value
        if (current.phase == FocusPhase.Idle) return
        alarmFeedbackController.stopAlarm()
        updateElapsedForCurrentPhase()
        finishSession(status = "stopped", endReason = "userStopped", targetPhase = FocusPhase.Stopped)
    }

    fun stopAlarm() {
        alarmFeedbackController.stopAlarm()
        when (mutableState.value.phase) {
            FocusPhase.FocusAlarm -> {
                val restMinutes = restMinutesForCurrentCycle()
                if (restMinutes > 0) {
                    startPhase(FocusPhase.RestRunning, restMinutes * 60L)
                } else {
                    completeOrStartNextCycle(endReason = "timerElapsed")
                }
            }
            // Rest completion returns to ready; the next focus block is always explicit.
            FocusPhase.RestAlarm -> finishSession(status = "completed", endReason = "restElapsed", targetPhase = FocusPhase.Completed)
            else -> Unit
        }
    }

    fun snooze() {
        if (mutableState.value.phase != FocusPhase.FocusAlarm && mutableState.value.phase != FocusPhase.RestAlarm) return
        alarmFeedbackController.stopAlarm()
        val updated = session?.copy(
            snoozeCount = (session?.snoozeCount ?: 0) + 1,
            updatedAt = nowProvider(),
        )
        if (updated != null) {
            session = updated
            viewModelScope.launch { focusSessionRepository.saveSession(updated) }
        }
        val snoozePhase = if (mutableState.value.phase == FocusPhase.RestAlarm) FocusPhase.RestRunning else FocusPhase.Snoozed
        startPhase(snoozePhase, settings.resolveFocusMode().snoozeMinutes * 60L)
    }

    private fun startPhase(phase: FocusPhase, durationSeconds: Long) {
        val now = nowProvider()
        alarmFeedbackController.stopAlarm()
        tickerJob?.cancel()
        phaseStartedAt = now
        phaseEndAt = now + durationSeconds * 1_000L
        phaseBaseFocusSeconds = session?.actualFocusSeconds ?: 0
        phaseBaseRestSeconds = session?.actualRestSeconds ?: 0
        mutableState.update {
            it.copy(
                phase = phase,
                remainingSeconds = durationSeconds,
                totalSeconds = durationSeconds,
                currentCycle = currentCycle,
                totalCycles = totalCycles,
                activeSessionId = session?.id,
                actualFocusSeconds = session?.actualFocusSeconds ?: 0,
                actualRestSeconds = session?.actualRestSeconds ?: 0,
                snoozeCount = session?.snoozeCount ?: 0,
            )
        }
        focusServiceController.update(mutableState.value)
        focusAlarmScheduler.schedulePhaseEnd(phaseEndAt)
        scheduleWorkdayEndIfNeeded()
        startTicker()
    }

    private fun pauseRunningPhase(pausedPhase: FocusPhase) {
        updateElapsedForCurrentPhase()
        val remaining = remainingSeconds()
        tickerJob?.cancel()
        focusAlarmScheduler.cancelPhaseEnd()
        mutableState.update { it.copy(phase = pausedPhase, remainingSeconds = remaining) }
        focusServiceController.update(mutableState.value)
        scheduleWorkdayEndIfNeeded()
        startTicker(checkOnly = true)
    }

    private fun startTicker(checkOnly: Boolean = false) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (shouldEndForWorkday()) {
                    updateElapsedForCurrentPhase()
                    finishSession(status = "completed", endReason = "workdayEnded", targetPhase = FocusPhase.Completed)
                    break
                }
                if (checkOnly) continue
                val remaining = remainingSeconds()
                mutableState.update { it.copy(remainingSeconds = remaining) }
                focusServiceController.update(mutableState.value)
                if (remaining <= 0) {
                    handlePhaseElapsed()
                    break
                }
            }
        }
    }

    private fun handlePhaseElapsed() {
        updateElapsedForCurrentPhase(forceFullPhase = true)
        focusAlarmScheduler.cancelPhaseEnd()
        when (mutableState.value.phase) {
            FocusPhase.FocusRunning,
            FocusPhase.Snoozed,
            -> mutableState.update { it.copy(phase = FocusPhase.FocusAlarm, remainingSeconds = 0) }
            FocusPhase.RestRunning -> mutableState.update { it.copy(phase = FocusPhase.RestAlarm, remainingSeconds = 0) }
            else -> Unit
        }
        focusServiceController.update(mutableState.value)
        if (mutableState.value.phase == FocusPhase.FocusAlarm || mutableState.value.phase == FocusPhase.RestAlarm) {
            alarmFeedbackController.startAlarm(settings.alarmSoundKey, settings.hapticsEnabled)
        }
    }

    private fun updateElapsedForCurrentPhase(forceFullPhase: Boolean = false) {
        val currentSession = session ?: return
        val elapsed = if (forceFullPhase) {
            mutableState.value.totalSeconds
        } else {
            ((nowProvider() - phaseStartedAt) / 1_000L).coerceAtLeast(0)
                .coerceAtMost(mutableState.value.totalSeconds)
        }
        val now = nowProvider()
        val updated = when (mutableState.value.phase) {
            FocusPhase.FocusRunning,
            FocusPhase.FocusPaused,
            FocusPhase.Snoozed,
            -> currentSession.copy(actualFocusSeconds = phaseBaseFocusSeconds + elapsed, updatedAt = now)
            FocusPhase.RestRunning,
            FocusPhase.RestPaused,
            -> currentSession.copy(actualRestSeconds = phaseBaseRestSeconds + elapsed, updatedAt = now)
            else -> currentSession.copy(updatedAt = now)
        }
        session = updated
        mutableState.update {
            it.copy(
                actualFocusSeconds = updated.actualFocusSeconds,
                actualRestSeconds = updated.actualRestSeconds,
                snoozeCount = updated.snoozeCount,
            )
        }
        viewModelScope.launch { focusSessionRepository.saveSession(updated) }
    }

    private fun finishSession(
        status: String,
        endReason: String,
        targetPhase: FocusPhase,
    ) {
        tickerJob?.cancel()
        allowOutsideWorkingHoursForSession = false
        focusAlarmScheduler.cancelPhaseEnd()
        focusAlarmScheduler.cancelWorkdayEnd()
        alarmFeedbackController.stopAlarm()
        val now = nowProvider()
        val currentSession = session
        if (currentSession != null) {
            val finished = currentSession.copy(
                endedAt = now,
                status = status,
                endReason = endReason,
                updatedAt = now,
            )
            session = finished
            viewModelScope.launch { focusSessionRepository.saveSession(finished) }
        }
        mutableState.update {
            it.copy(
                phase = targetPhase,
                remainingSeconds = 0,
                totalSeconds = 0,
                activeSessionId = null,
                pendingOutsideWorkingHoursConfirmation = false,
            )
        }
        focusServiceController.update(mutableState.value)
    }

    private fun completeOrStartNextCycle(endReason: String) {
        if (currentCycle < totalCycles) {
            currentCycle += 1
            startPhase(FocusPhase.FocusRunning, settings.resolveFocusMode().workMinutes * 60L)
        } else {
            finishSession(status = "completed", endReason = endReason, targetPhase = FocusPhase.Completed)
            if (activeModeType == POMODORO_MODE_ID || activeModeType == MULTI_CYCLE_MODE_ID) {
                mutableCompletionEvents.tryEmit(FocusCompletionEvent(activeModeName))
            }
        }
    }

    private fun restMinutesForCurrentCycle(): Int = settings.resolveFocusMode().restMinutesForCycle(currentCycle)

    private fun scheduleWorkdayEndIfNeeded() {
        focusAlarmScheduler.cancelWorkdayEnd()
        if (allowOutsideWorkingHoursForSession || !settings.workingHoursEnabled || workingHourRules.isEmpty()) return
        val endAt = WorkingHoursEvaluator.currentWorkingPeriodEndMillis(workingHourRules, nowProvider()) ?: return
        focusAlarmScheduler.scheduleWorkdayEnd(endAt)
    }

    private fun remainingSeconds(): Long = ((phaseEndAt - nowProvider() + 999L) / 1_000L).coerceAtLeast(0)

    private fun shouldEndForWorkday(): Boolean {
        val phase = mutableState.value.phase
        if (!settings.workingHoursEnabled || workingHourRules.isEmpty()) return false
        if (allowOutsideWorkingHoursForSession) return false
        if (phase == FocusPhase.Idle || phase == FocusPhase.Completed || phase == FocusPhase.Stopped) return false
        if (phase == FocusPhase.FocusAlarm || phase == FocusPhase.RestAlarm) return false
        return !isInsideWorkingHours()
    }

    private fun isInsideWorkingHours(): Boolean = WorkingHoursEvaluator.isWithinWorkingHours(
        rules = workingHourRules,
        epochMillis = nowProvider(),
    )

    private fun startOfTodayMillis(): Long {
        val now = java.time.Instant.ofEpochMilli(nowProvider()).atZone(java.time.ZoneId.systemDefault())
        return now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
    }

    internal fun cancelJobsForTest() {
        tickerJob?.cancel()
        viewModelScope.coroutineContext.cancelChildren()
    }

    companion object {
        fun factory(
            settingsFlow: Flow<SettingsSnapshot>,
            focusSessionRepository: FocusSessionRepository,
            workingHoursRepository: WorkingHoursRepository,
            focusServiceController: FocusServiceController = FocusServiceController.NoOp,
            commandsFlow: Flow<FocusActionCommand> = emptyFlow(),
            runtimeStateProvider: () -> FocusUiState? = { null },
            pendingCommands: () -> List<FocusActionCommand> = { emptyList() },
            acknowledgeCommand: (FocusActionCommand) -> Unit = {},
            focusAlarmScheduler: FocusAlarmScheduler = FocusAlarmScheduler.NoOp,
            alarmFeedbackController: AlarmFeedbackController = AlarmFeedbackController.NoOp,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FocusViewModel(
                settingsFlow = settingsFlow,
                focusSessionRepository = focusSessionRepository,
                workingHoursRepository = workingHoursRepository,
                focusServiceController = focusServiceController,
                commandsFlow = commandsFlow,
                runtimeStateProvider = runtimeStateProvider,
                pendingCommands = pendingCommands,
                acknowledgeCommand = acknowledgeCommand,
                focusAlarmScheduler = focusAlarmScheduler,
                alarmFeedbackController = alarmFeedbackController,
            ) as T
        }
    }
}

data class FocusDashboardSummary(
    val activePhase: FocusPhase = FocusPhase.Idle,
    val activeRemainingSeconds: Long = 0,
    val activeSessionId: String? = null,
    val completedFocusSeconds: Long = 0,
    val completedSessions: Int = 0,
)
