package com.kaemis.healthdesk.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderEventDao {
    @Query("SELECT * FROM reminder_events WHERE firedAt >= :fromEpochMillis ORDER BY firedAt DESC")
    fun observeEventsFrom(fromEpochMillis: Long): Flow<List<ReminderEventEntity>>

    @Query("SELECT * FROM reminder_events ORDER BY firedAt DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int): Flow<List<ReminderEventEntity>>

    @Query("SELECT * FROM reminder_events ORDER BY firedAt ASC")
    suspend fun getAll(): List<ReminderEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ReminderEventEntity)

    @Upsert
    suspend fun upsertAll(events: List<ReminderEventEntity>)

    @Query("DELETE FROM reminder_events")
    suspend fun clear()
}
