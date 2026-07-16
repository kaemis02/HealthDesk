package com.kaemis.healthdesk.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE status = 'active' ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE startedAt >= :fromEpochMillis ORDER BY startedAt ASC")
    fun observeSessionsFrom(fromEpochMillis: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt ASC")
    suspend fun getAll(): List<FocusSessionEntity>

    @Upsert
    suspend fun upsert(session: FocusSessionEntity)

    @Upsert
    suspend fun upsertAll(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun clear()
}
