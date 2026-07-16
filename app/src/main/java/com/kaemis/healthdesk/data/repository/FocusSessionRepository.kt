package com.kaemis.healthdesk.data.repository

import com.kaemis.healthdesk.data.dao.FocusSessionDao
import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

class FocusSessionRepository(
    private val focusSessionDao: FocusSessionDao,
) {
    fun observeActiveSession(): Flow<FocusSessionEntity?> = focusSessionDao.observeActiveSession()

    fun observeSessionsFrom(fromEpochMillis: Long): Flow<List<FocusSessionEntity>> =
        focusSessionDao.observeSessionsFrom(fromEpochMillis)

    suspend fun saveSession(session: FocusSessionEntity) {
        focusSessionDao.upsert(session)
    }
}
