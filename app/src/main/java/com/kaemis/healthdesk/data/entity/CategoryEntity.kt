package com.kaemis.healthdesk.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User-editable category shared by task and reminder scopes.
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["scope", "name"], unique = true), Index("scope"), Index("sortOrder")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val scope: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val sortOrder: Int,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
