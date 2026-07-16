package com.kaemis.healthdesk.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): UserProfileEntity?

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}
