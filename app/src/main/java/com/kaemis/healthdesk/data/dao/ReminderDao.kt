package com.kaemis.healthdesk.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.kaemis.healthdesk.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAt ASC")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY nextScheduledAt ASC")
    fun observeEnabledReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminder(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY createdAt ASC")
    suspend fun getAll(): List<ReminderEntity>

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Upsert
    suspend fun upsertAll(reminders: List<ReminderEntity>)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun clear()
}
