package com.kaemis.healthdesk.data.repository

import com.kaemis.healthdesk.data.dao.TaskDao
import com.kaemis.healthdesk.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
) {
    fun observePendingTasks(): Flow<List<TaskEntity>> = taskDao.observePendingTasks()

    fun observeCompletedTasks(limit: Int = 12): Flow<List<TaskEntity>> = taskDao.observeCompletedTasks(limit)

    fun observePendingTasksByCategory(categoryId: String): Flow<List<TaskEntity>> =
        taskDao.observePendingTasksByCategory(categoryId)

    suspend fun saveTask(task: TaskEntity) {
        taskDao.upsert(task)
    }

    suspend fun saveTasks(tasks: List<TaskEntity>) {
        taskDao.upsertAll(tasks)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.delete(task)
    }
}
