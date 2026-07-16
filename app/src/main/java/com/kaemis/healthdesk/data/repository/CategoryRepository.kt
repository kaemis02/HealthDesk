package com.kaemis.healthdesk.data.repository

import com.kaemis.healthdesk.data.dao.CategoryDao
import com.kaemis.healthdesk.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao,
) {
    fun observeCategories(scope: String): Flow<List<CategoryEntity>> = categoryDao.observeCategories(scope)

    suspend fun saveCategory(category: CategoryEntity) {
        categoryDao.upsert(category)
    }

    suspend fun saveCategories(categories: List<CategoryEntity>) {
        categoryDao.upsertAll(categories)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.delete(category)
    }
}
