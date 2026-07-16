package com.kaemis.healthdesk.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkingHourRuleDao {
    @Query("SELECT * FROM working_hour_rules ORDER BY sortOrder ASC")
    fun observeRules(): Flow<List<WorkingHourRuleEntity>>

    @Query("SELECT * FROM working_hour_rules ORDER BY sortOrder ASC")
    suspend fun getAll(): List<WorkingHourRuleEntity>

    @Upsert
    suspend fun upsert(rule: WorkingHourRuleEntity)

    @Upsert
    suspend fun upsertAll(rules: List<WorkingHourRuleEntity>)

    @Query("DELETE FROM working_hour_rules")
    suspend fun clear()
}
