package com.kaemis.healthdesk.domain.categories

data class Category(
    val id: String,
    val scope: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val sortOrder: Int,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
