package com.kaemis.healthdesk.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaemis.healthdesk.domain.focus.CustomFocusMode
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore(name = "healthdesk_settings")
private val settingsJson = Json { ignoreUnknownKeys = true }

data class SettingsSnapshot(
    val themeMode: String = "system",
    val accentKey: String = "sage",
    val languageCode: String = "en",
    val timerMode: String = "normalTimer",
    val workSessionMinutes: Int = 50,
    val restMinutes: Int = 10,
    val snoozeMinutes: Int = 10,
    val pomodoroCycles: Int = 4,
    val pomodoroShortRestMinutes: Int = 5,
    val pomodoroLongRestMinutes: Int = 15,
    val multiCycleCycles: Int = 3,
    val customFocusModes: List<CustomFocusMode> = emptyList(),
    val workingHoursEnabled: Boolean = true,
    val workdayNotificationsEnabled: Boolean = true,
    val outOfOffice: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val alarmSoundKey: String = "tone1",
    val reminderSoundKey: String = "ring2",
    val taskSoundKey: String = "ring1",
    val tutorialCompleted: Boolean = false,
)

class SettingsDataStore(
    private val context: Context,
) {
    val settings: Flow<SettingsSnapshot> = context.settingsDataStore.data.map { preferences ->
        SettingsSnapshot(
            themeMode = preferences[Keys.ThemeMode] ?: "system",
            accentKey = preferences[Keys.AccentKey] ?: "sage",
            languageCode = preferences[Keys.LanguageCode] ?: "en",
            timerMode = preferences[Keys.TimerMode] ?: "normalTimer",
            workSessionMinutes = preferences[Keys.WorkSessionMinutes] ?: 50,
            restMinutes = preferences[Keys.RestMinutes] ?: 10,
            snoozeMinutes = preferences[Keys.SnoozeMinutes] ?: 10,
            pomodoroCycles = preferences[Keys.PomodoroCycles] ?: 4,
            pomodoroShortRestMinutes = preferences[Keys.PomodoroShortRestMinutes] ?: 5,
            pomodoroLongRestMinutes = preferences[Keys.PomodoroLongRestMinutes] ?: 15,
            multiCycleCycles = preferences[Keys.MultiCycleCycles] ?: 3,
            customFocusModes = decodeCustomFocusModes(preferences[Keys.CustomFocusModes]),
            workingHoursEnabled = preferences[Keys.WorkingHoursEnabled] ?: true,
            workdayNotificationsEnabled = preferences[Keys.WorkdayNotificationsEnabled] ?: true,
            outOfOffice = preferences[Keys.OutOfOffice] ?: false,
            notificationsEnabled = preferences[Keys.NotificationsEnabled] ?: true,
            hapticsEnabled = preferences[Keys.HapticsEnabled] ?: true,
            alarmSoundKey = preferences[Keys.AlarmSoundKey] ?: "tone1",
            reminderSoundKey = preferences[Keys.ReminderSoundKey] ?: "ring2",
            taskSoundKey = preferences[Keys.TaskSoundKey] ?: "ring1",
            tutorialCompleted = preferences[Keys.TutorialCompleted] ?: false,
        )
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ThemeMode] = themeMode
        }
    }

    suspend fun updateAccentKey(accentKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AccentKey] = accentKey
        }
    }

    suspend fun updateLanguageCode(languageCode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.LanguageCode] = languageCode
        }
    }

    suspend fun updateTimerMode(timerMode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.TimerMode] = timerMode
        }
    }

    suspend fun updateWorkSessionMinutes(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.WorkSessionMinutes] = minutes.coerceAtLeast(1)
        }
    }

    suspend fun updateRestMinutes(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.RestMinutes] = minutes.coerceAtLeast(0)
        }
    }

    suspend fun updateSnoozeMinutes(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.SnoozeMinutes] = minutes.coerceAtLeast(1)
        }
    }

    suspend fun updatePomodoroCycles(cycles: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.PomodoroCycles] = cycles.coerceAtLeast(1)
        }
    }

    suspend fun updatePomodoroShortRestMinutes(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.PomodoroShortRestMinutes] = minutes.coerceAtLeast(0)
        }
    }

    suspend fun updatePomodoroLongRestMinutes(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.PomodoroLongRestMinutes] = minutes.coerceAtLeast(0)
        }
    }

    suspend fun updateMultiCycleCycles(cycles: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MultiCycleCycles] = cycles.coerceAtLeast(1)
        }
    }

    suspend fun updateCustomFocusModes(modes: List<CustomFocusMode>) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.CustomFocusModes] = settingsJson.encodeToString(modes)
        }
    }

    suspend fun updateWorkingHoursEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.WorkingHoursEnabled] = enabled
        }
    }

    suspend fun updateWorkdayNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.WorkdayNotificationsEnabled] = enabled
        }
    }

    suspend fun updateOutOfOffice(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.OutOfOffice] = enabled
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.NotificationsEnabled] = enabled
        }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.HapticsEnabled] = enabled
        }
    }

    suspend fun updateAlarmSoundKey(soundKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.AlarmSoundKey] = soundKey
        }
    }

    suspend fun updateReminderSoundKey(soundKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ReminderSoundKey] = soundKey
        }
    }

    suspend fun updateTaskSoundKey(soundKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.TaskSoundKey] = soundKey
        }
    }

    /** UI-only state; onboarding completion is never included in portable backups. */
    suspend fun updateTutorialCompleted(completed: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.TutorialCompleted] = completed
        }
    }

    suspend fun replaceWith(snapshot: SettingsSnapshot) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.ThemeMode] = snapshot.themeMode
            preferences[Keys.AccentKey] = snapshot.accentKey
            preferences[Keys.LanguageCode] = snapshot.languageCode
            preferences[Keys.TimerMode] = snapshot.timerMode
            preferences[Keys.WorkSessionMinutes] = snapshot.workSessionMinutes.coerceAtLeast(1)
            preferences[Keys.RestMinutes] = snapshot.restMinutes.coerceAtLeast(0)
            preferences[Keys.SnoozeMinutes] = snapshot.snoozeMinutes.coerceAtLeast(1)
            preferences[Keys.PomodoroCycles] = snapshot.pomodoroCycles.coerceAtLeast(1)
            preferences[Keys.PomodoroShortRestMinutes] = snapshot.pomodoroShortRestMinutes.coerceAtLeast(0)
            preferences[Keys.PomodoroLongRestMinutes] = snapshot.pomodoroLongRestMinutes.coerceAtLeast(0)
            preferences[Keys.MultiCycleCycles] = snapshot.multiCycleCycles.coerceAtLeast(1)
            preferences[Keys.CustomFocusModes] = settingsJson.encodeToString(snapshot.customFocusModes)
            preferences[Keys.WorkingHoursEnabled] = snapshot.workingHoursEnabled
            preferences[Keys.WorkdayNotificationsEnabled] = snapshot.workdayNotificationsEnabled
            preferences[Keys.OutOfOffice] = snapshot.outOfOffice
            preferences[Keys.NotificationsEnabled] = snapshot.notificationsEnabled
            preferences[Keys.HapticsEnabled] = snapshot.hapticsEnabled
            preferences[Keys.AlarmSoundKey] = snapshot.alarmSoundKey
            preferences[Keys.ReminderSoundKey] = snapshot.reminderSoundKey
            preferences[Keys.TaskSoundKey] = snapshot.taskSoundKey
        }
    }

    suspend fun resetToDefaults() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private object Keys {
        val ThemeMode = stringPreferencesKey("themeMode")
        val AccentKey = stringPreferencesKey("accentKey")
        val LanguageCode = stringPreferencesKey("languageCode")
        val TimerMode = stringPreferencesKey("timerMode")
        val WorkSessionMinutes = intPreferencesKey("workSessionMinutes")
        val RestMinutes = intPreferencesKey("restMinutes")
        val SnoozeMinutes = intPreferencesKey("snoozeMinutes")
        val PomodoroCycles = intPreferencesKey("pomodoroCycles")
        val PomodoroShortRestMinutes = intPreferencesKey("pomodoroShortRestMinutes")
        val PomodoroLongRestMinutes = intPreferencesKey("pomodoroLongRestMinutes")
        val MultiCycleCycles = intPreferencesKey("multiCycleCycles")
        val CustomFocusModes = stringPreferencesKey("customFocusModes")
        val WorkingHoursEnabled = booleanPreferencesKey("workingHoursEnabled")
        val WorkdayNotificationsEnabled = booleanPreferencesKey("workdayNotificationsEnabled")
        val OutOfOffice = booleanPreferencesKey("outOfOffice")
        val NotificationsEnabled = booleanPreferencesKey("notificationsEnabled")
        val HapticsEnabled = booleanPreferencesKey("hapticsEnabled")
        val AlarmSoundKey = stringPreferencesKey("alarmSoundKey")
        val ReminderSoundKey = stringPreferencesKey("reminderSoundKey")
        val TaskSoundKey = stringPreferencesKey("taskSoundKey")
        val TutorialCompleted = booleanPreferencesKey("tutorialCompleted")
    }

    private fun decodeCustomFocusModes(raw: String?): List<CustomFocusMode> = raw?.let {
        runCatching { settingsJson.decodeFromString<List<CustomFocusMode>>(it) }.getOrElse { emptyList() }
    } ?: emptyList()
}
