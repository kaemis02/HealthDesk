package com.kaemis.healthdesk.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.repository.ReminderRepository
import com.kaemis.healthdesk.domain.reminders.ReminderScheduler
import com.kaemis.healthdesk.platform.alarm.ReminderAlarmScheduler
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReminderDraft(
    val title: String = "",
    val scheduleMode: String = "interval",
    val intervalMinutes: Int = 30,
    val fixedLocalTime: String = "09:00",
    val recurrenceUnit: String = "daily",
    val recurrenceInterval: Int = 1,
    val recurrenceWeekdays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val recurrenceDayOfMonth: Int = 1,
    val monthlyDays: Set<Int> = setOf(1),
    val recurrenceMonth: Int = 1,
    val recurrenceDay: Int = 1,
    val monthlyWeekday: Int = 1,
    val monthlyWeekOrdinal: Int = 1,
    val soundKey: String = "ring2",
    val iconKey: String = "notifications",
)

fun ReminderEntity.toDraft(): ReminderDraft = ReminderDraft(
    title = title,
    scheduleMode = scheduleMode,
    intervalMinutes = intervalMinutes ?: 30,
    fixedLocalTime = fixedLocalTime ?: "09:00",
    recurrenceUnit = recurrenceUnit ?: "daily",
    recurrenceInterval = recurrenceInterval ?: 1,
    recurrenceWeekdays = recurrenceWeekdays
        ?.split(",")
        ?.mapNotNull(String::toIntOrNull)
        ?.toSet()
        ?.ifEmpty { setOf(1, 2, 3, 4, 5) }
        ?: setOf(1, 2, 3, 4, 5),
    recurrenceDayOfMonth = recurrenceDayOfMonth ?: 1,
    monthlyDays = if (recurrenceUnit == "monthly") {
        recurrenceWeekdays?.split(",")?.mapNotNull(String::toIntOrNull)?.toSet()?.ifEmpty { setOf(1) } ?: setOf(1)
    } else {
        setOf(recurrenceDayOfMonth ?: 1)
    },
    recurrenceMonth = recurrenceMonth ?: 1,
    recurrenceDay = recurrenceDay ?: 1,
    monthlyWeekday = recurrenceWeekdays?.split(",")?.firstOrNull()?.toIntOrNull() ?: 1,
    monthlyWeekOrdinal = recurrenceDayOfMonth ?: 1,
    soundKey = soundKey,
    iconKey = iconKey,
)

class RemindersViewModel(
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmScheduler: ReminderAlarmScheduler = ReminderAlarmScheduler.NoOp,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    val reminders: StateFlow<List<ReminderEntity>> = reminderRepository.observeReminders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    fun toggleReminder(reminder: ReminderEntity) {
        val now = nowProvider()
        val enabled = !reminder.isEnabled
        val updated = reminder.copy(
            isEnabled = enabled,
            nextScheduledAt = if (enabled) ReminderScheduler.nextScheduledAt(reminder.copy(isEnabled = true), now) else null,
            updatedAt = now,
        )
        viewModelScope.launch {
            reminderRepository.saveReminder(updated)
            if (enabled && updated.nextScheduledAt != null) {
                reminderAlarmScheduler.schedule(updated.id, updated.nextScheduledAt)
            } else {
                reminderAlarmScheduler.cancel(updated.id)
            }
        }
    }

    fun createReminder(draft: ReminderDraft) {
        saveReminder(existing = null, draft = draft)
    }

    fun saveReminder(existing: ReminderEntity?, draft: ReminderDraft) {
        val trimmedTitle = draft.title.trim()
        if (trimmedTitle.isEmpty()) return
        val now = nowProvider()
        val reminder = (existing ?: newReminderSkeleton(trimmedTitle, now)).copy(
            title = trimmedTitle,
            categoryId = if ((existing?.kind ?: "custom") == "water") "reminder-water" else "reminder-general",
            scheduleMode = draft.scheduleMode,
            intervalMinutes = if (draft.scheduleMode == "interval") draft.intervalMinutes.coerceAtLeast(1) else null,
            fixedLocalTime = if (draft.scheduleMode == "interval") null else draft.fixedLocalTime,
            recurrenceUnit = if (draft.scheduleMode == "recurring") draft.recurrenceUnit else null,
            recurrenceInterval = if (draft.scheduleMode == "recurring") draft.recurrenceInterval.coerceAtLeast(1) else null,
            recurrenceWeekdays = when {
                draft.scheduleMode != "recurring" -> null
                draft.recurrenceUnit == "weekly" -> draft.recurrenceWeekdays.sorted().joinToString(",")
                draft.recurrenceUnit == "monthly" -> draft.monthlyDays.sorted().joinToString(",")
                draft.recurrenceUnit == "monthlyWeekday" -> draft.monthlyWeekday.toString()
                else -> null
            },
            recurrenceDayOfMonth = when {
                draft.scheduleMode != "recurring" -> null
                draft.recurrenceUnit == "monthly" -> draft.monthlyDays.sorted().firstOrNull()?.coerceIn(1, 31)
                draft.recurrenceUnit == "monthlyDay" -> draft.recurrenceDayOfMonth.coerceIn(1, 31)
                draft.recurrenceUnit == "monthlyWeekday" -> draft.monthlyWeekOrdinal
                else -> null
            },
            recurrenceMonth = if (draft.scheduleMode == "recurring" && draft.recurrenceUnit == "yearly") {
                draft.recurrenceMonth.coerceIn(1, 12)
            } else {
                null
            },
            recurrenceDay = if (draft.scheduleMode == "recurring" && draft.recurrenceUnit == "yearly") {
                draft.recurrenceDay.coerceIn(1, 31)
            } else {
                null
            },
            soundKey = draft.soundKey,
            iconKey = draft.iconKey,
            nextScheduledAt = if ((existing?.isEnabled ?: false)) {
                ReminderScheduler.nextScheduledAt(
                    reminder = (existing ?: newReminderSkeleton(trimmedTitle, now)).copy(
                        title = trimmedTitle,
                        scheduleMode = draft.scheduleMode,
                        intervalMinutes = if (draft.scheduleMode == "interval") draft.intervalMinutes.coerceAtLeast(1) else null,
                        fixedLocalTime = if (draft.scheduleMode == "interval") null else draft.fixedLocalTime,
                        recurrenceUnit = if (draft.scheduleMode == "recurring") draft.recurrenceUnit else null,
                        recurrenceInterval = if (draft.scheduleMode == "recurring") draft.recurrenceInterval.coerceAtLeast(1) else null,
                        recurrenceWeekdays = when {
                            draft.scheduleMode != "recurring" -> null
                            draft.recurrenceUnit == "weekly" -> draft.recurrenceWeekdays.sorted().joinToString(",")
                            draft.recurrenceUnit == "monthly" -> draft.monthlyDays.sorted().joinToString(",")
                            draft.recurrenceUnit == "monthlyWeekday" -> draft.monthlyWeekday.toString()
                            else -> null
                        },
                        recurrenceDayOfMonth = when {
                            draft.scheduleMode != "recurring" -> null
                            draft.recurrenceUnit == "monthly" -> draft.monthlyDays.sorted().firstOrNull()?.coerceIn(1, 31)
                            draft.recurrenceUnit == "monthlyDay" -> draft.recurrenceDayOfMonth.coerceIn(1, 31)
                            draft.recurrenceUnit == "monthlyWeekday" -> draft.monthlyWeekOrdinal
                            else -> null
                        },
                        recurrenceMonth = if (draft.scheduleMode == "recurring" && draft.recurrenceUnit == "yearly") draft.recurrenceMonth.coerceIn(1, 12) else null,
                        recurrenceDay = if (draft.scheduleMode == "recurring" && draft.recurrenceUnit == "yearly") draft.recurrenceDay.coerceIn(1, 31) else null,
                        soundKey = draft.soundKey,
                        iconKey = draft.iconKey,
                    ),
                    nowMillis = now,
                )
            } else {
                null
            },
            updatedAt = now,
        )

        viewModelScope.launch {
            reminderRepository.saveReminder(reminder)
            if (reminder.isEnabled && reminder.nextScheduledAt != null) {
                reminderAlarmScheduler.schedule(reminder.id, reminder.nextScheduledAt)
            } else {
                reminderAlarmScheduler.cancel(reminder.id)
            }
        }
    }

    fun updateReminder(reminder: ReminderEntity, title: String, categoryId: String = reminder.categoryId) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        if (trimmedTitle == reminder.title && categoryId == reminder.categoryId) return
        val now = nowProvider()
        val updatedReminder = reminder.copy(
            title = trimmedTitle,
            categoryId = categoryId,
            nextScheduledAt = if (reminder.isEnabled) ReminderScheduler.nextScheduledAt(reminder, now) else null,
            updatedAt = now,
        )
        viewModelScope.launch {
            reminderRepository.saveReminder(updatedReminder)
            if (updatedReminder.isEnabled && updatedReminder.nextScheduledAt != null) {
                reminderAlarmScheduler.schedule(updatedReminder.id, updatedReminder.nextScheduledAt)
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderAlarmScheduler.cancel(reminder.id)
            reminderRepository.deleteReminder(reminder)
        }
    }

    fun createIntervalReminder(title: String, categoryId: String = "reminder-general", intervalMinutes: Int = 30) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val now = nowProvider()
        viewModelScope.launch {
            reminderRepository.saveReminder(
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    kind = "custom",
                    title = trimmedTitle,
                    categoryId = categoryId,
                    isEnabled = false,
                    scheduleMode = "interval",
                    intervalMinutes = intervalMinutes.coerceAtLeast(1),
                    fixedLocalTime = null,
                    recurrenceUnit = null,
                    recurrenceInterval = null,
                    recurrenceWeekdays = null,
                    recurrenceDayOfMonth = null,
                    recurrenceMonth = null,
                    recurrenceDay = null,
                    soundKey = "ring2",
                    iconKey = "notifications",
                    lastFiredAt = null,
                    nextScheduledAt = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun createFixedTimeReminder(title: String, fixedLocalTime: String, categoryId: String = "reminder-general") {
        createCustomReminder(title = title, categoryId = categoryId, scheduleMode = "fixedTimeOnce", fixedLocalTime = fixedLocalTime)
    }

    fun createDailyRecurringReminder(title: String, fixedLocalTime: String, categoryId: String = "reminder-general") {
        createCustomReminder(
            title = title,
            categoryId = categoryId,
            scheduleMode = "recurring",
            fixedLocalTime = fixedLocalTime,
            recurrenceUnit = "daily",
            recurrenceInterval = 1,
        )
    }

    fun createWeeklyRecurringReminder(title: String, fixedLocalTime: String, weekdays: String) {
        createCustomReminder(
            title = title,
            scheduleMode = "recurring",
            fixedLocalTime = fixedLocalTime,
            recurrenceUnit = "weekly",
            recurrenceInterval = 1,
            recurrenceWeekdays = weekdays,
        )
    }

    fun createMonthlyByDayReminder(title: String, fixedLocalTime: String, dayOfMonth: Int) {
        createCustomReminder(
            title = title,
            scheduleMode = "recurring",
            fixedLocalTime = fixedLocalTime,
            recurrenceUnit = "monthlyDay",
            recurrenceInterval = 1,
            recurrenceDayOfMonth = dayOfMonth,
        )
    }

    fun createMonthlyWeekdayReminder(title: String, fixedLocalTime: String, weekday: Int, nth: Int) {
        createCustomReminder(
            title = title,
            scheduleMode = "recurring",
            fixedLocalTime = fixedLocalTime,
            recurrenceUnit = "monthlyWeekday",
            recurrenceInterval = 1,
            recurrenceWeekdays = weekday.toString(),
            recurrenceDayOfMonth = nth,
        )
    }

    fun createYearlyReminder(title: String, fixedLocalTime: String, month: Int, day: Int) {
        createCustomReminder(
            title = title,
            scheduleMode = "recurring",
            fixedLocalTime = fixedLocalTime,
            recurrenceUnit = "yearly",
            recurrenceInterval = 1,
            recurrenceMonth = month,
            recurrenceDay = day,
        )
    }

    private fun createCustomReminder(
        title: String,
        categoryId: String = "reminder-general",
        scheduleMode: String,
        fixedLocalTime: String,
        recurrenceUnit: String? = null,
        recurrenceInterval: Int? = null,
        recurrenceWeekdays: String? = null,
        recurrenceDayOfMonth: Int? = null,
        recurrenceMonth: Int? = null,
        recurrenceDay: Int? = null,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val now = nowProvider()
        viewModelScope.launch {
            reminderRepository.saveReminder(
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    kind = "custom",
                    title = trimmedTitle,
                    categoryId = categoryId,
                    isEnabled = false,
                    scheduleMode = scheduleMode,
                    intervalMinutes = null,
                    fixedLocalTime = fixedLocalTime,
                    recurrenceUnit = recurrenceUnit,
                    recurrenceInterval = recurrenceInterval,
                    recurrenceWeekdays = recurrenceWeekdays,
                    recurrenceDayOfMonth = recurrenceDayOfMonth,
                    recurrenceMonth = recurrenceMonth,
                    recurrenceDay = recurrenceDay,
                    soundKey = "ring2",
                    iconKey = "notifications",
                    lastFiredAt = null,
                    nextScheduledAt = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    private fun newReminderSkeleton(title: String, now: Long): ReminderEntity = ReminderEntity(
        id = UUID.randomUUID().toString(),
        kind = "custom",
        title = title,
        categoryId = "reminder-general",
        isEnabled = false,
        scheduleMode = "interval",
        intervalMinutes = 30,
        fixedLocalTime = null,
        recurrenceUnit = null,
        recurrenceInterval = null,
        recurrenceWeekdays = null,
        recurrenceDayOfMonth = null,
        recurrenceMonth = null,
        recurrenceDay = null,
        soundKey = "ring2",
        iconKey = "notifications",
        lastFiredAt = null,
        nextScheduledAt = null,
        createdAt = now,
        updatedAt = now,
    )

    companion object {
        fun factory(
            reminderRepository: ReminderRepository,
            reminderAlarmScheduler: ReminderAlarmScheduler = ReminderAlarmScheduler.NoOp,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RemindersViewModel(reminderRepository, reminderAlarmScheduler) as T
            }
    }
}
