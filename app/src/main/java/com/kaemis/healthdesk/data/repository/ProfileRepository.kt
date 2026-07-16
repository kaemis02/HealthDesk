package com.kaemis.healthdesk.data.repository

import com.kaemis.healthdesk.data.dao.UserProfileDao
import com.kaemis.healthdesk.data.entity.DEFAULT_PROFILE_ID
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val userProfileDao: UserProfileDao,
) {
    fun observeProfile(): Flow<UserProfileEntity?> = userProfileDao.observeProfile(DEFAULT_PROFILE_ID)

    suspend fun saveProfile(profile: UserProfileEntity) {
        userProfileDao.upsert(profile)
    }
}
