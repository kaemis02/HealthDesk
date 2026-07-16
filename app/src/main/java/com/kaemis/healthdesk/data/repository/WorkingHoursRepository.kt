package com.kaemis.healthdesk.data.repository

import com.kaemis.healthdesk.data.dao.WorkingHourRuleDao
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import kotlinx.coroutines.flow.Flow

class WorkingHoursRepository(
    private val workingHourRuleDao: WorkingHourRuleDao,
) {
    fun observeRules(): Flow<List<WorkingHourRuleEntity>> = workingHourRuleDao.observeRules()

    suspend fun getRules(): List<WorkingHourRuleEntity> = workingHourRuleDao.getAll()

    suspend fun saveRule(rule: WorkingHourRuleEntity) {
        workingHourRuleDao.upsert(rule)
    }

    suspend fun saveRules(rules: List<WorkingHourRuleEntity>) {
        workingHourRuleDao.upsertAll(rules)
    }
}
