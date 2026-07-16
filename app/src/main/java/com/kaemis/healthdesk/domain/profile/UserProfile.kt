package com.kaemis.healthdesk.domain.profile

data class UserProfile(
    val id: String,
    val displayName: String,
    val avatarMode: String,
    val avatarLocalPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
