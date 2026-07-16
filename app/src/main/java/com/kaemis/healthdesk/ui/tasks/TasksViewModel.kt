package com.kaemis.healthdesk.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.TaskEntity
import com.kaemis.healthdesk.data.repository.TaskRepository
import com.kaemis.healthdesk.platform.audio.TaskFeedbackController
import java.util.UUID
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasksUiState(
    val pendingTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
)

class TasksViewModel(
    private val taskRepository: TaskRepository,
    settingsFlow: Flow<SettingsSnapshot> = flowOf(SettingsSnapshot()),
    private val taskFeedbackController: TaskFeedbackController = TaskFeedbackController.NoOp,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private var settings = SettingsSnapshot()

    val state: StateFlow<TasksUiState> = kotlinx.coroutines.flow.combine(
        taskRepository.observePendingTasks(),
        taskRepository.observeCompletedTasks(),
    ) { pending, completed ->
        TasksUiState(
            pendingTasks = pending,
            completedTasks = completed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TasksUiState(),
    )

    init {
        viewModelScope.launch {
            settingsFlow.collect { settings = it }
        }
    }

    fun createTask(title: String, categoryId: String? = "task-general") {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val now = nowProvider()
        val nextSortOrder = (state.value.pendingTasks.minOfOrNull { it.sortOrder } ?: 0) - 1
        viewModelScope.launch {
            taskRepository.saveTask(
                TaskEntity(
                    id = UUID.randomUUID().toString(),
                    title = trimmedTitle,
                    categoryId = categoryId,
                    isCompleted = false,
                    sortOrder = nextSortOrder,
                    createdAt = now,
                    updatedAt = now,
                    completedAt = null,
                ),
            )
        }
    }

    fun editTask(task: TaskEntity, title: String, categoryId: String? = task.categoryId) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        if (trimmedTitle == task.title && categoryId == task.categoryId) return
        viewModelScope.launch {
            taskRepository.saveTask(task.copy(title = trimmedTitle, categoryId = categoryId, updatedAt = nowProvider()))
        }
    }

    fun completeTask(task: TaskEntity) {
        val now = nowProvider()
        viewModelScope.launch {
            taskRepository.saveTask(
                task.copy(
                    isCompleted = true,
                    updatedAt = now,
                    completedAt = now,
                ),
            )
            taskFeedbackController.playTaskComplete(settings.taskSoundKey, settings.hapticsEnabled)
        }
    }

    fun restoreTask(task: TaskEntity) {
        val now = nowProvider()
        val nextSortOrder = (state.value.pendingTasks.minOfOrNull { it.sortOrder } ?: 0) - 1
        viewModelScope.launch {
            taskRepository.saveTask(
                task.copy(
                    isCompleted = false,
                    sortOrder = nextSortOrder,
                    updatedAt = now,
                    completedAt = null,
                ),
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.deleteTask(task) }
    }

    fun moveTask(task: TaskEntity, direction: Int) {
        val tasks = state.value.pendingTasks
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index == -1) return
        val targetIndex = (index + direction).coerceIn(tasks.indices)
        if (index == targetIndex) return
        val reordered = tasks.toMutableList().apply {
            removeAt(index)
            add(targetIndex, task)
        }.mapIndexed { sortOrder, item -> item.copy(sortOrder = sortOrder, updatedAt = nowProvider()) }
        viewModelScope.launch { taskRepository.saveTasks(reordered) }
    }

    /** Persists one completed drag operation after the UI has already animated its local order. */
    fun reorderTasks(tasks: List<TaskEntity>) {
        val now = nowProvider()
        val normalized = tasks.mapIndexed { sortOrder, task ->
            task.copy(sortOrder = sortOrder, updatedAt = now)
        }
        viewModelScope.launch { taskRepository.saveTasks(normalized) }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            settingsFlow: Flow<SettingsSnapshot> = flowOf(SettingsSnapshot()),
            taskFeedbackController: TaskFeedbackController = TaskFeedbackController.NoOp,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TasksViewModel(taskRepository, settingsFlow, taskFeedbackController) as T
        }
    }

    internal fun cancelJobsForTest() {
        viewModelScope.coroutineContext.cancelChildren()
    }
}
