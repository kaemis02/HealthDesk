package com.kaemis.healthdesk.data.backup

import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FlutterLegacyImportAdapter {
    fun canImport(root: JsonObject): Boolean =
        root["payloadType"] == null && (root["settings"] != null || root["customTimers"] != null || root["completedTasks"] != null)

    fun convert(root: JsonObject, importedAt: Long = System.currentTimeMillis()): NativeBackupPayload {
        val settings = root.objectOrNull("settings") ?: JsonObject(emptyMap())
        val exportedAt = root.stringOrNull("exportedAt")?.toEpochMillisOrNull() ?: importedAt
        val activeTasks = root.arrayOrEmpty("tasks").mapIndexed { index, item -> item.jsonObject.toBackupTask(index, completed = false, importedAt) }
        val completedTasks = root.arrayOrEmpty("completedTasks").mapIndexed { index, item -> item.jsonObject.toBackupTask(index, completed = true, importedAt) }
        val customReminders = root.arrayOrEmpty("customTimers").map { item -> item.jsonObject.toBackupReminder(importedAt) }
        val reminderEvents = root.arrayOrEmpty("reminders").map { item -> item.jsonObject.toBackupReminderEvent(importedAt) }
        val waterReminder = BackupReminder(
            id = "reminder-water-built-in",
            kind = "builtIn",
            title = "Water",
            categoryId = "reminder-water",
            isEnabled = settings.booleanOrDefault("waterEnabled", false),
            scheduleMode = "interval",
            intervalMinutes = settings.intOrDefault("waterIntervalMinutes", 60).coerceAtLeast(1),
            fixedLocalTime = null,
            recurrenceUnit = null,
            recurrenceInterval = null,
            recurrenceWeekdays = null,
            recurrenceDayOfMonth = null,
            recurrenceMonth = null,
            recurrenceDay = null,
            soundKey = settings.stringOrNull("reminderSound") ?: "ring2",
            iconKey = "water_drop",
            lastFiredAt = null,
            nextScheduledAt = null,
            createdAt = exportedAt,
            updatedAt = exportedAt,
        )

        return NativeBackupPayload(
            appVersionName = "flutter-legacy",
            appVersionCode = 0,
            exportedAt = exportedAt,
            settings = settings.toBackupSettings(),
            workingHours = settings.toWorkingHours(exportedAt),
            tasks = activeTasks + completedTasks,
            reminders = listOf(waterReminder) + customReminders,
            categories = legacyCategories(exportedAt),
            reminderEvents = reminderEvents,
        )
    }

    private fun JsonObject.toBackupSettings(): BackupSettings = BackupSettings(
        themeMode = stringOrNull("themeMode") ?: if (booleanOrDefault("darkMode", false)) "dark" else "light",
        accentKey = "#6697FF",
        languageCode = stringOrNull("languageCode") ?: "en",
        timerMode = "normalTimer",
        workSessionMinutes = intOrDefault("fullIntervalMinutes", 50).coerceAtLeast(1),
        restMinutes = 10,
        snoozeMinutes = intOrDefault("snoozeMinutes", 10).coerceAtLeast(1),
        workingHoursEnabled = booleanOrDefault("workingHoursEnabled", false),
        notificationsEnabled = booleanOrDefault("pushNotificationsEnabled", true),
        hapticsEnabled = booleanOrDefault("hapticsEnabled", true),
        alarmSoundKey = stringOrNull("alarmSound") ?: "tone1",
        reminderSoundKey = stringOrNull("reminderSound") ?: "ring2",
        taskSoundKey = stringOrNull("taskSound") ?: "ring1",
    )

    private fun JsonObject.toWorkingHours(updatedAt: Long): List<BackupWorkingHourRule> {
        val enabled = booleanOrDefault("workingHoursEnabled", false)
        val start = stringOrNull("workStart") ?: "09:00"
        val end = stringOrNull("workEnd") ?: "18:00"
        return (1..7).map { day ->
            BackupWorkingHourRule(
                id = "working-hours-$day",
                dayOfWeek = day,
                isEnabled = enabled && day in 1..5,
                startLocalTime = start,
                endLocalTime = end,
                sortOrder = day,
                updatedAt = updatedAt,
            )
        }
    }

    private fun JsonObject.toBackupTask(index: Int, completed: Boolean, importedAt: Long): BackupTask {
        val updatedAt = stringOrNull("updatedAt")?.toEpochMillisOrNull() ?: importedAt
        val isCompleted = booleanOrDefault("completed", completed)
        return BackupTask(
            id = stringOrNull("id") ?: "legacy-task-$index-$updatedAt",
            title = stringOrNull("title") ?: "Task",
            categoryId = "task-general",
            isCompleted = isCompleted,
            sortOrder = intOrDefault("sortOrder", index),
            createdAt = updatedAt,
            updatedAt = updatedAt,
            completedAt = if (isCompleted) updatedAt else null,
        )
    }

    private fun JsonObject.toBackupReminder(importedAt: Long): BackupReminder {
        val updatedAt = stringOrNull("updatedAt")?.toEpochMillisOrNull() ?: importedAt
        val mode = stringOrNull("mode") ?: "interval"
        return BackupReminder(
            id = stringOrNull("id") ?: "legacy-reminder-$updatedAt",
            kind = "custom",
            title = stringOrNull("name") ?: "Reminder",
            categoryId = "reminder-general",
            isEnabled = booleanOrDefault("enabled", false),
            scheduleMode = if (mode == "fixedTime") "fixedTimeOnce" else "interval",
            intervalMinutes = if (mode == "fixedTime") null else intOrDefault("intervalMinutes", 30).coerceAtLeast(1),
            fixedLocalTime = if (mode == "fixedTime") stringOrNull("fixedTime") ?: "12:00" else null,
            recurrenceUnit = null,
            recurrenceInterval = null,
            recurrenceWeekdays = null,
            recurrenceDayOfMonth = null,
            recurrenceMonth = null,
            recurrenceDay = null,
            soundKey = stringOrNull("sound") ?: "ring2",
            iconKey = "notifications",
            lastFiredAt = null,
            nextScheduledAt = null,
            createdAt = updatedAt,
            updatedAt = updatedAt,
        )
    }

    private fun JsonObject.toBackupReminderEvent(importedAt: Long): BackupReminderEvent {
        val firedAt = stringOrNull("createdAt")?.toEpochMillisOrNull() ?: importedAt
        val title = stringOrNull("type") ?: "Reminder"
        return BackupReminderEvent(
            id = stringOrNull("id") ?: "legacy-reminder-event-$firedAt",
            reminderId = "legacy-history",
            titleSnapshot = title,
            categorySnapshot = "reminder-general",
            firedAt = firedAt,
            deliveryResult = "shown",
        )
    }

    private fun legacyCategories(createdAt: Long): List<BackupCategory> = listOf(
        BackupCategory("task-general", "task", "General", "label", "sage", 0, true, createdAt, createdAt),
        BackupCategory("reminder-general", "reminder", "General", "bell", "sage", 0, true, createdAt, createdAt),
        BackupCategory("reminder-water", "reminder", "Water", "water", "sky", 1, true, createdAt, createdAt),
    )

    private fun JsonObject.arrayOrEmpty(key: String): JsonArray = this[key]?.jsonArray ?: JsonArray(emptyList())
    private fun JsonObject.objectOrNull(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.content
    private fun JsonObject.intOrDefault(key: String, default: Int): Int = this[key]?.jsonPrimitive?.intOrNull ?: default
    private fun JsonObject.booleanOrDefault(key: String, default: Boolean): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: default
    private fun String.toEpochMillisOrNull(): Long? = runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
}
