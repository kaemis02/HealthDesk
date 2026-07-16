package com.kaemis.healthdesk.data.repository

import com.kaemis.healthdesk.data.dao.ReminderDao
import com.kaemis.healthdesk.data.dao.ReminderEventDao
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val reminderEventDao: ReminderEventDao,
) {
    fun observeReminders(): Flow<List<ReminderEntity>> = reminderDao.observeReminders()

    fun observeEnabledReminders(): Flow<List<ReminderEntity>> = reminderDao.observeEnabledReminders()

    fun observeRecentEvents(limit: Int = 12): Flow<List<ReminderEventEntity>> =
        reminderEventDao.observeRecentEvents(limit)

    suspend fun getReminder(id: String): ReminderEntity? = reminderDao.getReminder(id)

    suspend fun saveReminder(reminder: ReminderEntity) {
        reminderDao.upsert(reminder)
    }

    suspend fun saveReminders(reminders: List<ReminderEntity>) {
        reminderDao.upsertAll(reminders)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.delete(reminder)
    }

    suspend fun recordEvent(event: ReminderEventEntity) {
        reminderEventDao.insert(event)
    }
}
