package com.kaemis.healthdesk.ui.tasks

import com.kaemis.healthdesk.data.dao.TaskDao
import com.kaemis.healthdesk.data.datastore.SettingsSnapshot
import com.kaemis.healthdesk.data.entity.TaskEntity
import com.kaemis.healthdesk.data.repository.TaskRepository
import com.kaemis.healthdesk.platform.audio.TaskFeedbackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createEditCompleteRestoreDeleteTasks() = runTest(dispatcher) {
        val taskDao = FakeTaskDao()
        val feedback = RecordingTaskFeedbackController()
        val viewModel = TasksViewModel(
            taskRepository = TaskRepository(taskDao),
            settingsFlow = MutableStateFlow(SettingsSnapshot(taskSoundKey = "ring3", hapticsEnabled = false)),
            taskFeedbackController = feedback,
            nowProvider = { 1000L },
        )
        try {
            runCurrent()

            viewModel.createTask("Write report")
            runCurrent()
            assertEquals("Write report", viewModel.state.value.pendingTasks.single().title)

            val task = viewModel.state.value.pendingTasks.single()
            viewModel.editTask(task, "Write final report")
            runCurrent()
            assertEquals("Write final report", viewModel.state.value.pendingTasks.single().title)

            viewModel.completeTask(viewModel.state.value.pendingTasks.single())
            runCurrent()
            assertTrue(viewModel.state.value.pendingTasks.isEmpty())
            assertEquals(1, viewModel.state.value.completedTasks.size)
            assertEquals("ring3", feedback.soundKey)
            assertEquals(false, feedback.hapticsEnabled)

            viewModel.restoreTask(viewModel.state.value.completedTasks.single())
            runCurrent()
            assertEquals(1, viewModel.state.value.pendingTasks.size)
            assertTrue(viewModel.state.value.completedTasks.isEmpty())

            viewModel.deleteTask(viewModel.state.value.pendingTasks.single())
            runCurrent()
            assertTrue(viewModel.state.value.pendingTasks.isEmpty())
        } finally {
            viewModel.cancelJobsForTest()
        }
    }

    @Test
    fun moveTaskUpdatesSortOrder() = runTest(dispatcher) {
        val taskDao = FakeTaskDao()
        val viewModel = TasksViewModel(
            taskRepository = TaskRepository(taskDao),
            nowProvider = { 1000L },
        )
        try {
            viewModel.createTask("First")
            viewModel.createTask("Second")
            runCurrent()

            val firstVisible = viewModel.state.value.pendingTasks.first()
            viewModel.moveTask(firstVisible, 1)
            runCurrent()

            assertEquals(firstVisible.id, viewModel.state.value.pendingTasks[1].id)
        } finally {
            viewModel.cancelJobsForTest()
        }
    }
}

private class FakeTaskDao : TaskDao {
    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun observePendingTasks(): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { !it.isCompleted }.sortedWith(compareBy<TaskEntity> { it.sortOrder }.thenByDescending { it.createdAt })
    }

    override fun observeCompletedTasks(limit: Int): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.isCompleted }.sortedByDescending { it.completedAt }.take(limit)
    }

    override fun observeCompletedTasksFrom(fromEpochMillis: Long): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.isCompleted && (it.completedAt ?: 0L) >= fromEpochMillis }.sortedByDescending { it.completedAt }
    }

    override fun observePendingTasksByCategory(categoryId: String): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { !it.isCompleted && it.categoryId == categoryId }.sortedBy { it.sortOrder }
    }

    override suspend fun getAll(): List<TaskEntity> = tasks.value

    override suspend fun upsert(task: TaskEntity) {
        tasks.value = tasks.value.filterNot { it.id == task.id } + task
    }

    override suspend fun upsertAll(tasks: List<TaskEntity>) {
        tasks.forEach { upsert(it) }
    }

    override suspend fun delete(task: TaskEntity) {
        tasks.value = tasks.value.filterNot { it.id == task.id }
    }

    override suspend fun clear() {
        tasks.value = emptyList()
    }
}

private class RecordingTaskFeedbackController : TaskFeedbackController {
    var soundKey: String? = null
    var hapticsEnabled: Boolean? = null

    override fun playTaskComplete(soundKey: String, hapticsEnabled: Boolean) {
        this.soundKey = soundKey
        this.hapticsEnabled = hapticsEnabled
    }
}
