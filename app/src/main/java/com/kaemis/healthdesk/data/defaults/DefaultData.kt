package com.kaemis.healthdesk.data.defaults

import com.kaemis.healthdesk.data.entity.CategoryEntity
import com.kaemis.healthdesk.data.entity.DEFAULT_PROFILE_ID
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity

object DefaultData {
    fun profile(now: Long): UserProfileEntity = UserProfileEntity(
        id = DEFAULT_PROFILE_ID,
        displayName = "User",
        avatarMode = "initials",
        avatarLocalPath = null,
        createdAt = now,
        updatedAt = now,
    )

    fun taskCategories(now: Long): List<CategoryEntity> = listOf(
        category("task-general", "task", "General", "label", "sage", 0, now),
        category("task-work", "task", "Work", "work", "sky", 1, now),
        category("task-personal", "task", "Personal", "person", "lavender", 2, now),
        category("task-health", "task", "Health", "favorite", "mint", 3, now),
        category("task-admin", "task", "Admin", "folder", "amber", 4, now),
    )

    fun reminderCategories(now: Long): List<CategoryEntity> = listOf(
        category("reminder-general", "reminder", "General", "notifications", "sage", 0, now),
        category("reminder-water", "reminder", "Water", "water_drop", "sky", 1, now),
        category("reminder-stretch", "reminder", "Stretch", "self_improvement", "mint", 2, now),
        category("reminder-eye-rest", "reminder", "Eye rest", "visibility", "lavender", 3, now),
        category("reminder-walk", "reminder", "Walk", "directions_walk", "clay", 4, now),
        category("reminder-breathing", "reminder", "Breathing", "air", "sage", 5, now),
        category("reminder-coffee", "reminder", "Coffee", "local_cafe", "amber", 6, now),
        category("reminder-medicine", "reminder", "Medicine", "medication", "clay", 7, now),
    )

    fun workingHourRules(now: Long): List<WorkingHourRuleEntity> = listOf(
        workingHourRule("working-hours-monday", 1, true, "08:00", "18:00", 0, now),
        workingHourRule("working-hours-tuesday", 2, true, "08:00", "18:00", 1, now),
        workingHourRule("working-hours-wednesday", 3, true, "08:00", "18:00", 2, now),
        workingHourRule("working-hours-thursday", 4, true, "08:00", "18:00", 3, now),
        workingHourRule("working-hours-friday", 5, true, "08:00", "15:00", 4, now),
        workingHourRule("working-hours-saturday", 6, false, "08:00", "18:00", 5, now),
        workingHourRule("working-hours-sunday", 7, false, "08:00", "18:00", 6, now),
    )

    fun waterReminder(now: Long): ReminderEntity = ReminderEntity(
        id = "reminder-water-built-in",
        kind = "water",
        title = "Water",
        categoryId = "reminder-water",
        isEnabled = false,
        scheduleMode = "interval",
        intervalMinutes = 60,
        fixedLocalTime = null,
        recurrenceUnit = null,
        recurrenceInterval = null,
        recurrenceWeekdays = null,
        recurrenceDayOfMonth = null,
        recurrenceMonth = null,
        recurrenceDay = null,
        soundKey = "ring2",
        iconKey = "water_drop",
        lastFiredAt = null,
        nextScheduledAt = null,
        createdAt = now,
        updatedAt = now,
    )

    private fun category(
        id: String,
        scope: String,
        name: String,
        iconKey: String,
        colorKey: String,
        sortOrder: Int,
        now: Long,
    ): CategoryEntity = CategoryEntity(
        id = id,
        scope = scope,
        name = name,
        iconKey = iconKey,
        colorKey = colorKey,
        sortOrder = sortOrder,
        isBuiltIn = true,
        createdAt = now,
        updatedAt = now,
    )

    private fun workingHourRule(
        id: String,
        dayOfWeek: Int,
        isEnabled: Boolean,
        startLocalTime: String,
        endLocalTime: String,
        sortOrder: Int,
        now: Long,
    ): WorkingHourRuleEntity = WorkingHourRuleEntity(
        id = id,
        dayOfWeek = dayOfWeek,
        isEnabled = isEnabled,
        startLocalTime = startLocalTime,
        endLocalTime = endLocalTime,
        sortOrder = sortOrder,
        updatedAt = now,
    )
}
