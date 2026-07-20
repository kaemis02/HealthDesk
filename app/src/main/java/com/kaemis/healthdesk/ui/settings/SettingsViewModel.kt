package com.kaemis.healthdesk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaemis.healthdesk.data.datastore.SettingsDataStore
import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import com.kaemis.healthdesk.data.repository.WorkingHoursRepository
import com.kaemis.healthdesk.domain.focus.CustomFocusMode
import com.kaemis.healthdesk.domain.focus.MULTI_CYCLE_MODE_ID
import com.kaemis.healthdesk.domain.focus.POMODORO_MODE_ID
import com.kaemis.healthdesk.domain.focus.isBuiltInFocusModeId
import com.kaemis.healthdesk.domain.focus.newCustomFocusMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val workingHoursRepository: WorkingHoursRepository,
    private val resetLocalDataAction: suspend () -> Unit,
    private val resyncWorkdayAlarms: suspend () -> Unit = {},
) : ViewModel() {
    val settings: StateFlow<SettingsSnapshot> = settingsDataStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsSnapshot(),
    )

    fun updateThemeMode(themeMode: String) = viewModelScope.launch {
        settingsDataStore.updateThemeMode(themeMode)
    }

    fun updateAccentKey(accentKey: String) = viewModelScope.launch {
        settingsDataStore.updateAccentKey(accentKey)
    }

    fun updateLanguageCode(languageCode: String) = viewModelScope.launch {
        settingsDataStore.updateLanguageCode(languageCode)
    }

    fun updateTimerMode(timerMode: String) = viewModelScope.launch {
        settingsDataStore.updateTimerMode(timerMode)
    }

    fun createCustomFocusMode(name: String, type: String) = viewModelScope.launch {
        val customMode = settings.value.newCustomFocusMode(name = name, type = type)
        settingsDataStore.updateCustomFocusModes(settings.value.customFocusModes + customMode)
        settingsDataStore.updateTimerMode(customMode.id)
    }

    fun deleteCustomFocusMode(modeId: String) = viewModelScope.launch {
        val updated = settings.value.customFocusModes.filterNot { it.id == modeId }
        settingsDataStore.updateCustomFocusModes(updated)
        if (settings.value.timerMode == modeId) {
            settingsDataStore.updateTimerMode("normalTimer")
        }
    }

    fun updateFocusModeName(modeId: String, name: String) = updateCustomFocusMode(modeId) {
        it.copy(name = name.trim().ifBlank { it.name })
    }

    fun updateFocusModeType(modeId: String, type: String) = updateCustomFocusMode(modeId) {
        it.copy(type = type)
    }

    fun updateWorkSessionMinutes(minutes: Int) = viewModelScope.launch {
        settingsDataStore.updateWorkSessionMinutes(minutes)
    }

    fun updateFocusWorkSessionMinutes(modeId: String, minutes: Int) {
        if (isBuiltInFocusModeId(modeId)) {
            updateWorkSessionMinutes(minutes)
        } else {
            updateCustomFocusMode(modeId) { it.copy(workMinutes = minutes.coerceAtLeast(1)) }
        }
    }

    fun updateRestMinutes(minutes: Int) = viewModelScope.launch {
        settingsDataStore.updateRestMinutes(minutes)
    }

    fun updateFocusRestMinutes(modeId: String, minutes: Int) {
        if (isBuiltInFocusModeId(modeId)) {
            updateRestMinutes(minutes)
        } else {
            updateCustomFocusMode(modeId) { it.copy(restMinutes = minutes.coerceAtLeast(0)) }
        }
    }

    fun updateSnoozeMinutes(minutes: Int) = viewModelScope.launch {
        settingsDataStore.updateSnoozeMinutes(minutes)
    }

    fun updateFocusSnoozeMinutes(modeId: String, minutes: Int) {
        if (isBuiltInFocusModeId(modeId)) {
            updateSnoozeMinutes(minutes)
        } else {
            updateCustomFocusMode(modeId) { it.copy(snoozeMinutes = minutes.coerceAtLeast(1)) }
        }
    }

    fun updatePomodoroCycles(cycles: Int) = viewModelScope.launch {
        settingsDataStore.updatePomodoroCycles(cycles)
    }

    fun updateFocusPomodoroCycles(modeId: String, cycles: Int) {
        if (isBuiltInFocusModeId(modeId)) {
            updatePomodoroCycles(cycles)
        } else {
            updateCustomFocusMode(modeId) { it.copy(pomodoroCycles = cycles.coerceAtLeast(1)) }
        }
    }

    fun updatePomodoroShortRestMinutes(minutes: Int) = viewModelScope.launch {
        settingsDataStore.updatePomodoroShortRestMinutes(minutes)
    }

    fun updateFocusPomodoroShortRestMinutes(modeId: String, minutes: Int) {
        if (isBuiltInFocusModeId(modeId)) {
            updatePomodoroShortRestMinutes(minutes)
        } else {
            updateCustomFocusMode(modeId) { it.copy(pomodoroShortRestMinutes = minutes.coerceAtLeast(0)) }
        }
    }

    fun updatePomodoroLongRestMinutes(minutes: Int) = viewModelScope.launch {
        settingsDataStore.updatePomodoroLongRestMinutes(minutes)
    }

    fun updateFocusPomodoroLongRestMinutes(modeId: String, minutes: Int) {
        if (isBuiltInFocusModeId(modeId)) {
            updatePomodoroLongRestMinutes(minutes)
        } else {
            updateCustomFocusMode(modeId) { it.copy(pomodoroLongRestMinutes = minutes.coerceAtLeast(0)) }
        }
    }

    fun updateMultiCycleCycles(cycles: Int) = viewModelScope.launch {
        settingsDataStore.updateMultiCycleCycles(cycles)
    }

    fun updateFocusMultiCycleCycles(modeId: String, cycles: Int) {
        if (isBuiltInFocusModeId(modeId)) {
            updateMultiCycleCycles(cycles)
        } else {
            updateCustomFocusMode(modeId) { it.copy(multiCycleCycles = cycles.coerceAtLeast(1)) }
        }
    }

    fun updateWorkingHoursEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.updateWorkingHoursEnabled(enabled)
        resyncWorkdayAlarms()
    }

    fun updateWorkdayNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.updateWorkdayNotificationsEnabled(enabled)
        resyncWorkdayAlarms()
    }

    fun updateOutOfOffice(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.updateOutOfOffice(enabled)
        resyncWorkdayAlarms()
    }

    fun saveWorkingHourRule(rule: WorkingHourRuleEntity) = viewModelScope.launch {
        workingHoursRepository.saveRule(rule)
        resyncWorkdayAlarms()
    }

    fun updateNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.updateNotificationsEnabled(enabled)
    }

    fun updateHapticsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.updateHapticsEnabled(enabled)
    }

    fun updateAlarmSoundKey(soundKey: String) = viewModelScope.launch {
        settingsDataStore.updateAlarmSoundKey(soundKey)
    }

    fun updateReminderSoundKey(soundKey: String) = viewModelScope.launch {
        settingsDataStore.updateReminderSoundKey(soundKey)
    }

    fun updateTaskSoundKey(soundKey: String) = viewModelScope.launch {
        settingsDataStore.updateTaskSoundKey(soundKey)
    }

    fun updateTutorialCompleted(completed: Boolean) = viewModelScope.launch {
        settingsDataStore.updateTutorialCompleted(completed)
    }

    fun resetLocalData() = viewModelScope.launch {
        resetLocalDataAction.invoke()
    }

    private fun updateCustomFocusMode(
        modeId: String,
        transform: (CustomFocusMode) -> CustomFocusMode,
    ) = viewModelScope.launch {
        val updated = settings.value.customFocusModes.map { mode ->
            if (mode.id == modeId) transform(mode) else mode
        }
        settingsDataStore.updateCustomFocusModes(updated)
    }

    companion object {
        fun factory(
            settingsDataStore: SettingsDataStore,
            workingHoursRepository: WorkingHoursRepository,
            resetLocalData: suspend () -> Unit,
            resyncWorkdayAlarms: suspend () -> Unit = {},
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(settingsDataStore, workingHoursRepository, resetLocalData, resyncWorkdayAlarms) as T
            }
    }
}
