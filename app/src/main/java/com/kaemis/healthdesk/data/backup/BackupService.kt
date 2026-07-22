package com.kaemis.healthdesk.data.backup

import androidx.room.withTransaction
import com.kaemis.healthdesk.data.datastore.SettingsDataStore
import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.db.HealthDeskDatabase
import com.kaemis.healthdesk.data.entity.CategoryEntity
import com.kaemis.healthdesk.data.entity.DEFAULT_PROFILE_ID
import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import com.kaemis.healthdesk.data.entity.TaskEntity
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import com.kaemis.healthdesk.domain.focus.CustomFocusMode
import java.io.File
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class BackupService(
    private val database: HealthDeskDatabase? = null,
    private val settingsDataStore: SettingsDataStore? = null,
    private val flutterLegacyImportAdapter: FlutterLegacyImportAdapter = FlutterLegacyImportAdapter(),
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    fun export(payload: NativeBackupPayload): String = json.encodeToString(NativeBackupPayload.serializer(), payload)

    suspend fun exportNativeBackup(
        appVersionName: String,
        appVersionCode: Int,
        exportedAt: Long = System.currentTimeMillis(),
    ): String = export(createNativePayload(appVersionName, appVersionCode, exportedAt))

    suspend fun createNativePayload(
        appVersionName: String,
        appVersionCode: Int,
        exportedAt: Long = System.currentTimeMillis(),
    ): NativeBackupPayload {
        val db = requireDatabase()
        val settingsStore = requireSettingsDataStore()
        val settings = settingsStore.settings.first()
        return NativeBackupPayload(
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            exportedAt = exportedAt,
            profile = db.userProfileDao().getProfile(DEFAULT_PROFILE_ID)?.toBackupProfile(),
            settings = settings.toBackupSettings(),
            workingHours = db.workingHourRuleDao().getAll().map { it.toBackup() },
            tasks = db.taskDao().getAll().map { it.toBackup() },
            focusSessions = db.focusSessionDao().getAll().map { it.toBackup() },
            reminders = db.reminderDao().getAll().map { it.toBackup() },
            categories = db.categoryDao().getAll().map { it.toBackup() },
            reminderEvents = db.reminderEventDao().getAll().map { it.toBackup() },
        )
    }

    suspend fun importNativeBackup(
        payload: NativeBackupPayload,
        avatarDirectory: File? = null,
    ): BackupImportSummary {
        if (payload.payloadType != NATIVE_BACKUP_PAYLOAD_TYPE) {
            return BackupImportSummary.Invalid("Unsupported backup payload")
        }
        if (payload.schemaVersion > NATIVE_BACKUP_SCHEMA_VERSION) {
            return BackupImportSummary.FutureSchema(payload.schemaVersion)
        }

        val db = requireDatabase()
        db.withTransaction {
            db.reminderEventDao().clear()
            db.reminderDao().clear()
            db.focusSessionDao().clear()
            db.taskDao().clear()
            db.categoryDao().clear()
            db.workingHourRuleDao().clear()
            db.userProfileDao().clear()

            payload.profile?.toEntity(avatarDirectory)?.let { db.userProfileDao().upsert(it) }
            db.categoryDao().upsertAll(payload.categories.map { it.toEntity() })
            db.workingHourRuleDao().upsertAll(payload.workingHours.map { it.toEntity() })
            db.taskDao().upsertAll(payload.tasks.map { it.toEntity() })
            db.focusSessionDao().upsertAll(payload.focusSessions.map { it.toEntity() })
            db.reminderDao().upsertAll(payload.reminders.map { it.toEntity() })
            db.reminderEventDao().upsertAll(payload.reminderEvents.map { it.toEntity() })
        }
        payload.settings?.let { requireSettingsDataStore().replaceWith(it.toSettingsSnapshot()) }
        return BackupImportSummary.Imported(
            tasks = payload.tasks.size,
            reminders = payload.reminders.size,
            categories = payload.categories.size,
            focusSessions = payload.focusSessions.size,
            reminderEvents = payload.reminderEvents.size,
        )
    }

    fun parse(jsonText: String): BackupParseResult {
        val root = runCatching { json.parseToJsonElement(jsonText).jsonObject }.getOrNull()
        if (root != null && root["payloadType"] == null && flutterLegacyImportAdapter.canImport(root)) {
            return BackupParseResult.Valid(flutterLegacyImportAdapter.convert(root))
        }

        val payload = try {
            json.decodeFromString(NativeBackupPayload.serializer(), jsonText)
        } catch (error: SerializationException) {
            return BackupParseResult.Invalid("Invalid backup JSON")
        } catch (error: IllegalArgumentException) {
            return BackupParseResult.Invalid("Invalid backup JSON")
        }

        if (payload.payloadType != NATIVE_BACKUP_PAYLOAD_TYPE) {
            return BackupParseResult.Invalid("Unsupported backup payload")
        }
        if (payload.schemaVersion > NATIVE_BACKUP_SCHEMA_VERSION) {
            return BackupParseResult.FutureSchema(payload.schemaVersion)
        }
        return BackupParseResult.Valid(payload)
    }

    private fun requireDatabase(): HealthDeskDatabase = requireNotNull(database) {
        "BackupService requires a HealthDeskDatabase for this operation"
    }

    private fun requireSettingsDataStore(): SettingsDataStore = requireNotNull(settingsDataStore) {
        "BackupService requires a SettingsDataStore for this operation"
    }
}

sealed interface BackupParseResult {
    data class Valid(val payload: NativeBackupPayload) : BackupParseResult
    data class FutureSchema(val schemaVersion: Int) : BackupParseResult
    data class Invalid(val reason: String) : BackupParseResult
}

sealed interface BackupImportSummary {
    data class Imported(
        val tasks: Int,
        val reminders: Int,
        val categories: Int,
        val focusSessions: Int,
        val reminderEvents: Int,
    ) : BackupImportSummary

    data class FutureSchema(val schemaVersion: Int) : BackupImportSummary
    data class Invalid(val reason: String) : BackupImportSummary
}

private fun UserProfileEntity.toBackupProfile(): BackupProfile = BackupProfile(
    id = id,
    displayName = displayName,
    avatarMode = avatarMode,
    avatarLocalPath = avatarLocalPath,
    avatarBase64 = avatarLocalPath?.let { path -> runCatching { Base64.getEncoder().encodeToString(File(path).readBytes()) }.getOrNull() },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun BackupProfile.toEntity(avatarDirectory: File?): UserProfileEntity {
    val restoredAvatarPath = if (avatarMode == "localImage" && avatarBase64 != null && avatarDirectory != null) {
        runCatching {
            if (!avatarDirectory.exists()) avatarDirectory.mkdirs()
            val avatarFile = File(avatarDirectory, "profile-avatar")
            avatarFile.writeBytes(Base64.getDecoder().decode(avatarBase64))
            avatarFile.absolutePath
        }.getOrNull()
    } else {
        avatarLocalPath
    }
    return UserProfileEntity(
        id = id,
        displayName = displayName,
        avatarMode = if (restoredAvatarPath != null) avatarMode else "initials",
        avatarLocalPath = restoredAvatarPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun SettingsSnapshot.toBackupSettings(): BackupSettings = BackupSettings(
    themeMode = themeMode,
    accentKey = accentKey,
    languageCode = languageCode,
    timerMode = timerMode,
    workSessionMinutes = workSessionMinutes,
    restMinutes = restMinutes,
    snoozeMinutes = snoozeMinutes,
    pomodoroCycles = pomodoroCycles,
    pomodoroWorkMinutes = pomodoroWorkMinutes,
    pomodoroShortRestMinutes = pomodoroShortRestMinutes,
    pomodoroLongRestMinutes = pomodoroLongRestMinutes,
    multiCycleCycles = multiCycleCycles,
    customFocusModes = customFocusModes.map(CustomFocusMode::toBackup),
    workingHoursEnabled = workingHoursEnabled,
    workdayNotificationsEnabled = workdayNotificationsEnabled,
    outOfOffice = outOfOffice,
    notificationsEnabled = notificationsEnabled,
    hapticsEnabled = hapticsEnabled,
    alarmSoundKey = alarmSoundKey,
    reminderSoundKey = reminderSoundKey,
    taskSoundKey = taskSoundKey,
)

private fun BackupSettings.toSettingsSnapshot(): SettingsSnapshot = SettingsSnapshot(
    themeMode = themeMode,
    accentKey = accentKey,
    languageCode = languageCode,
    timerMode = timerMode,
    workSessionMinutes = workSessionMinutes,
    restMinutes = restMinutes,
    snoozeMinutes = snoozeMinutes,
    pomodoroCycles = pomodoroCycles,
    pomodoroWorkMinutes = pomodoroWorkMinutes,
    pomodoroShortRestMinutes = pomodoroShortRestMinutes,
    pomodoroLongRestMinutes = pomodoroLongRestMinutes,
    multiCycleCycles = multiCycleCycles,
    customFocusModes = customFocusModes.map(BackupCustomFocusMode::toDomain),
    workingHoursEnabled = workingHoursEnabled,
    workdayNotificationsEnabled = workdayNotificationsEnabled,
    outOfOffice = outOfOffice,
    notificationsEnabled = notificationsEnabled,
    hapticsEnabled = hapticsEnabled,
    alarmSoundKey = alarmSoundKey,
    reminderSoundKey = reminderSoundKey,
    taskSoundKey = taskSoundKey,
)

private fun TaskEntity.toBackup(): BackupTask = BackupTask(id, title, categoryId, isCompleted, sortOrder, createdAt, updatedAt, completedAt)
private fun BackupTask.toEntity(): TaskEntity = TaskEntity(id, title, categoryId, isCompleted, sortOrder, createdAt, updatedAt, completedAt)

private fun CustomFocusMode.toBackup(): BackupCustomFocusMode = BackupCustomFocusMode(
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
)

private fun BackupCustomFocusMode.toDomain(): CustomFocusMode = CustomFocusMode(
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
)

private fun FocusSessionEntity.toBackup(): BackupFocusSession = BackupFocusSession(
    id, startedAt, endedAt, plannedDurationMinutes, actualFocusSeconds, plannedRestMinutes, actualRestSeconds, status, endReason, snoozeCount, createdAt, updatedAt,
)

private fun BackupFocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id, startedAt, endedAt, plannedDurationMinutes, actualFocusSeconds, plannedRestMinutes, actualRestSeconds, status, endReason, snoozeCount, createdAt, updatedAt,
)

private fun ReminderEntity.toBackup(): BackupReminder = BackupReminder(
    id, kind, title, categoryId, isEnabled, scheduleMode, intervalMinutes, fixedLocalTime, recurrenceUnit, recurrenceInterval, recurrenceWeekdays,
    recurrenceDayOfMonth, recurrenceMonth, recurrenceDay, soundKey, iconKey, lastFiredAt, nextScheduledAt, createdAt, updatedAt, recurrenceEndDate,
)

private fun BackupReminder.toEntity(): ReminderEntity = ReminderEntity(
    id, kind, title, categoryId, isEnabled, scheduleMode, intervalMinutes, fixedLocalTime, recurrenceUnit, recurrenceInterval, recurrenceWeekdays,
    recurrenceDayOfMonth, recurrenceMonth, recurrenceDay, soundKey, iconKey, lastFiredAt, nextScheduledAt, createdAt, updatedAt, recurrenceEndDate,
)

private fun CategoryEntity.toBackup(): BackupCategory = BackupCategory(id, scope, name, iconKey, colorKey, sortOrder, isBuiltIn, createdAt, updatedAt)
private fun BackupCategory.toEntity(): CategoryEntity = CategoryEntity(id, scope, name, iconKey, colorKey, sortOrder, isBuiltIn, createdAt, updatedAt)

private fun ReminderEventEntity.toBackup(): BackupReminderEvent = BackupReminderEvent(id, reminderId, titleSnapshot, categorySnapshot, firedAt, deliveryResult)
private fun BackupReminderEvent.toEntity(): ReminderEventEntity = ReminderEventEntity(id, reminderId, titleSnapshot, categorySnapshot, firedAt, deliveryResult)

private fun WorkingHourRuleEntity.toBackup(): BackupWorkingHourRule = BackupWorkingHourRule(id, dayOfWeek, isEnabled, startLocalTime, endLocalTime, sortOrder, updatedAt)
private fun BackupWorkingHourRule.toEntity(): WorkingHourRuleEntity = WorkingHourRuleEntity(id, dayOfWeek, isEnabled, startLocalTime, endLocalTime, sortOrder, updatedAt)
