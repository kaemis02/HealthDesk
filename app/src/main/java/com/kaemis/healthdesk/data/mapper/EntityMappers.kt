package com.kaemis.healthdesk.data.mapper

import com.kaemis.healthdesk.data.entity.CategoryEntity
import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import com.kaemis.healthdesk.data.entity.TaskEntity
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import com.kaemis.healthdesk.domain.categories.Category
import com.kaemis.healthdesk.domain.focus.FocusSession
import com.kaemis.healthdesk.domain.focus.WorkingHourRule
import com.kaemis.healthdesk.domain.profile.UserProfile
import com.kaemis.healthdesk.domain.reminders.Reminder
import com.kaemis.healthdesk.domain.reminders.ReminderEvent
import com.kaemis.healthdesk.domain.tasks.TaskItem

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    displayName = displayName,
    avatarMode = avatarMode,
    avatarLocalPath = avatarLocalPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    displayName = displayName,
    avatarMode = avatarMode,
    avatarLocalPath = avatarLocalPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TaskEntity.toDomain(): TaskItem = TaskItem(id, title, categoryId, isCompleted, sortOrder, createdAt, updatedAt, completedAt)

fun TaskItem.toEntity(): TaskEntity = TaskEntity(id, title, categoryId, isCompleted, sortOrder, createdAt, updatedAt, completedAt)

fun CategoryEntity.toDomain(): Category = Category(id, scope, name, iconKey, colorKey, sortOrder, isBuiltIn, createdAt, updatedAt)

fun Category.toEntity(): CategoryEntity = CategoryEntity(id, scope, name, iconKey, colorKey, sortOrder, isBuiltIn, createdAt, updatedAt)

fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    plannedDurationMinutes = plannedDurationMinutes,
    actualFocusSeconds = actualFocusSeconds,
    plannedRestMinutes = plannedRestMinutes,
    actualRestSeconds = actualRestSeconds,
    status = status,
    endReason = endReason,
    snoozeCount = snoozeCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    plannedDurationMinutes = plannedDurationMinutes,
    actualFocusSeconds = actualFocusSeconds,
    plannedRestMinutes = plannedRestMinutes,
    actualRestSeconds = actualRestSeconds,
    status = status,
    endReason = endReason,
    snoozeCount = snoozeCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    kind = kind,
    title = title,
    categoryId = categoryId,
    isEnabled = isEnabled,
    scheduleMode = scheduleMode,
    intervalMinutes = intervalMinutes,
    fixedLocalTime = fixedLocalTime,
    recurrenceUnit = recurrenceUnit,
    recurrenceInterval = recurrenceInterval,
    recurrenceWeekdays = recurrenceWeekdays,
    recurrenceDayOfMonth = recurrenceDayOfMonth,
    recurrenceMonth = recurrenceMonth,
    recurrenceDay = recurrenceDay,
    soundKey = soundKey,
    iconKey = iconKey,
    lastFiredAt = lastFiredAt,
    nextScheduledAt = nextScheduledAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    kind = kind,
    title = title,
    categoryId = categoryId,
    isEnabled = isEnabled,
    scheduleMode = scheduleMode,
    intervalMinutes = intervalMinutes,
    fixedLocalTime = fixedLocalTime,
    recurrenceUnit = recurrenceUnit,
    recurrenceInterval = recurrenceInterval,
    recurrenceWeekdays = recurrenceWeekdays,
    recurrenceDayOfMonth = recurrenceDayOfMonth,
    recurrenceMonth = recurrenceMonth,
    recurrenceDay = recurrenceDay,
    soundKey = soundKey,
    iconKey = iconKey,
    lastFiredAt = lastFiredAt,
    nextScheduledAt = nextScheduledAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ReminderEventEntity.toDomain(): ReminderEvent = ReminderEvent(id, reminderId, titleSnapshot, categorySnapshot, firedAt, deliveryResult)

fun ReminderEvent.toEntity(): ReminderEventEntity = ReminderEventEntity(id, reminderId, titleSnapshot, categorySnapshot, firedAt, deliveryResult)

fun WorkingHourRuleEntity.toDomain(): WorkingHourRule = WorkingHourRule(id, dayOfWeek, isEnabled, startLocalTime, endLocalTime, sortOrder, updatedAt)

fun WorkingHourRule.toEntity(): WorkingHourRuleEntity = WorkingHourRuleEntity(id, dayOfWeek, isEnabled, startLocalTime, endLocalTime, sortOrder, updatedAt)
