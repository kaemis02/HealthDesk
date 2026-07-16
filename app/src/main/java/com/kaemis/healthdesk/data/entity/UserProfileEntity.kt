package com.kaemis.healthdesk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_PROFILE_ID = "local-user"

/**
 * Singleton local profile. This is not an account and must never imply sync or login.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = DEFAULT_PROFILE_ID,
    val displayName: String,
    val avatarMode: String,
    val avatarLocalPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
