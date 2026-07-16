package com.kaemis.healthdesk.data.backup

import kotlinx.serialization.Serializable

const val NATIVE_BACKUP_PAYLOAD_TYPE = "healthdesk.native.backup"
const val NATIVE_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class NativeBackupPayload(
    val payloadType: String = NATIVE_BACKUP_PAYLOAD_TYPE,
    val schemaVersion: Int = NATIVE_BACKUP_SCHEMA_VERSION,
    val appVersionName: String,
    val appVersionCode: Int,
    val exportedAt: Long,
    val profile: BackupProfile? = null,
    val settings: BackupSettings? = null,
    val workingHours: List<BackupWorkingHourRule> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val focusSessions: List<BackupFocusSession> = emptyList(),
    val reminders: List<BackupReminder> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val reminderEvents: List<BackupReminderEvent> = emptyList(),
)

@Serializable
data class BackupProfile(
    val id: String,
    val displayName: String,
    val avatarMode: String,
    val avatarLocalPath: String? = null,
    val avatarBase64: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupSettings(
    val themeMode: String,
    val accentKey: String,
    val languageCode: String,
    val timerMode: String,
    val workSessionMinutes: Int,
    val restMinutes: Int,
    val snoozeMinutes: Int,
    val pomodoroCycles: Int = 4,
    val pomodoroShortRestMinutes: Int = 5,
    val pomodoroLongRestMinutes: Int = 15,
    val multiCycleCycles: Int = 3,
    val customFocusModes: List<BackupCustomFocusMode> = emptyList(),
    val workingHoursEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val alarmSoundKey: String,
    val reminderSoundKey: String,
    val taskSoundKey: String,
)

@Serializable
data class BackupCustomFocusMode(
    val id: String,
    val name: String,
    val type: String,
    val workMinutes: Int,
    val restMinutes: Int,
    val snoozeMinutes: Int,
    val pomodoroCycles: Int = 4,
    val pomodoroShortRestMinutes: Int = 5,
    val pomodoroLongRestMinutes: Int = 15,
    val multiCycleCycles: Int = 3,
)

@Serializable
data class BackupTask(
    val id: String,
    val title: String,
    val categoryId: String? = null,
    val isCompleted: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
)

@Serializable
data class BackupFocusSession(
    val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val plannedDurationMinutes: Int,
    val actualFocusSeconds: Long,
    val plannedRestMinutes: Int,
    val actualRestSeconds: Long,
    val status: String,
    val endReason: String? = null,
    val snoozeCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupReminder(
    val id: String,
    val kind: String,
    val title: String,
    val categoryId: String,
    val isEnabled: Boolean,
    val scheduleMode: String,
    val intervalMinutes: Int? = null,
    val fixedLocalTime: String? = null,
    val recurrenceUnit: String? = null,
    val recurrenceInterval: Int? = null,
    val recurrenceWeekdays: String? = null,
    val recurrenceDayOfMonth: Int? = null,
    val recurrenceMonth: Int? = null,
    val recurrenceDay: Int? = null,
    val soundKey: String,
    val iconKey: String = "notifications",
    val lastFiredAt: Long? = null,
    val nextScheduledAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupCategory(
    val id: String,
    val scope: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val sortOrder: Int,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupReminderEvent(
    val id: String,
    val reminderId: String,
    val titleSnapshot: String,
    val categorySnapshot: String,
    val firedAt: Long,
    val deliveryResult: String,
)

@Serializable
data class BackupWorkingHourRule(
    val id: String,
    val dayOfWeek: Int,
    val isEnabled: Boolean,
    val startLocalTime: String,
    val endLocalTime: String,
    val sortOrder: Int,
    val updatedAt: Long,
)
