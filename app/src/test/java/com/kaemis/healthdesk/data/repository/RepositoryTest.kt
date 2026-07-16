package com.kaemis.healthdesk.data.repository

import com.kaemis.healthdesk.data.dao.CategoryDao
import com.kaemis.healthdesk.data.dao.FocusSessionDao
import com.kaemis.healthdesk.data.dao.ReminderDao
import com.kaemis.healthdesk.data.dao.ReminderEventDao
import com.kaemis.healthdesk.data.dao.TaskDao
import com.kaemis.healthdesk.data.dao.UserProfileDao
import com.kaemis.healthdesk.data.dao.WorkingHourRuleDao
import com.kaemis.healthdesk.data.defaults.DefaultData
import com.kaemis.healthdesk.data.entity.CategoryEntity
import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import com.kaemis.healthdesk.data.entity.TaskEntity
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryTest {
    @Test
    fun profileRepositorySavesAndObservesProfile() = runTest {
        val dao = FakeUserProfileDao()
        val repository = ProfileRepository(dao)
        val profile = DefaultData.profile(now = 1000L)

        assertNull(repository.observeProfile().first())

        repository.saveProfile(profile)

        assertEquals(profile, repository.observeProfile().first())
    }

    @Test
    fun taskRepositorySeparatesPendingAndCompletedTasks() = runTest {
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao)
        val pending = task("pending", isCompleted = false)
        val completed = task("completed", isCompleted = true)

        repository.saveTasks(listOf(pending, completed))

        assertEquals(listOf(pending), repository.observePendingTasks().first())
        assertEquals(listOf(completed), repository.observeCompletedTasks().first())
    }

    @Test
    fun categoryRepositoryFiltersByScope() = runTest {
        val dao = FakeCategoryDao()
        val repository = CategoryRepository(dao)
        val taskCategory = category("task-general", "task", "General")
        val reminderCategory = category("reminder-general", "reminder", "General")

        repository.saveCategories(listOf(taskCategory, reminderCategory))

        assertEquals(listOf(taskCategory), repository.observeCategories("task").first())
        assertEquals(listOf(reminderCategory), repository.observeCategories("reminder").first())
    }

    @Test
    fun focusSessionRepositoryObservesActiveSession() = runTest {
        val dao = FakeFocusSessionDao()
        val repository = FocusSessionRepository(dao)
        val activeSession = focusSession("active-session", status = "active")

        repository.saveSession(focusSession("completed-session", status = "completed"))
        repository.saveSession(activeSession)

        assertEquals(activeSession, repository.observeActiveSession().first())
    }

    @Test
    fun reminderRepositoryRecordsEvents() = runTest {
        val reminderDao = FakeReminderDao()
        val eventDao = FakeReminderEventDao()
        val repository = ReminderRepository(reminderDao, eventDao)
        val reminder = DefaultData.waterReminder(now = 1000L).copy(isEnabled = true)
        val event = ReminderEventEntity(
            id = "event-1",
            reminderId = reminder.id,
            titleSnapshot = reminder.title,
            categorySnapshot = "Water",
            firedAt = 2000L,
            deliveryResult = "shown",
        )

        repository.saveReminder(reminder)
        repository.recordEvent(event)

        assertEquals(listOf(reminder), repository.observeEnabledReminders().first())
        assertEquals(listOf(event), repository.observeRecentEvents().first())
    }

    @Test
    fun workingHoursRepositorySavesRules() = runTest {
        val dao = FakeWorkingHourRuleDao()
        val repository = WorkingHoursRepository(dao)
        val rules = DefaultData.workingHourRules(now = 1000L)

        repository.saveRules(rules)

        assertEquals(rules, repository.observeRules().first())
    }

    private fun task(id: String, isCompleted: Boolean): TaskEntity = TaskEntity(
        id = id,
        title = id,
        categoryId = "task-general",
        isCompleted = isCompleted,
        sortOrder = 0,
        createdAt = 1000L,
        updatedAt = 1000L,
        completedAt = if (isCompleted) 2000L else null,
    )

    private fun category(id: String, scope: String, name: String): CategoryEntity = CategoryEntity(
        id = id,
        scope = scope,
        name = name,
        iconKey = "label",
        colorKey = "sage",
        sortOrder = 0,
        isBuiltIn = true,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private fun focusSession(id: String, status: String): FocusSessionEntity = FocusSessionEntity(
        id = id,
        startedAt = 1000L,
        endedAt = if (status == "completed") 2000L else null,
        plannedDurationMinutes = 50,
        actualFocusSeconds = 0,
        plannedRestMinutes = 10,
        actualRestSeconds = 0,
        status = status,
        endReason = null,
        snoozeCount = 0,
        createdAt = 1000L,
        updatedAt = 1000L,
    )
}

private class FakeUserProfileDao : UserProfileDao {
    private val profiles = MutableStateFlow<Map<String, UserProfileEntity>>(emptyMap())

    override fun observeProfile(id: String): Flow<UserProfileEntity?> = profiles.map { it[id] }

    override suspend fun getProfile(id: String): UserProfileEntity? = profiles.value[id]

    override suspend fun upsert(profile: UserProfileEntity) {
        profiles.value = profiles.value + (profile.id to profile)
    }

    override suspend fun clear() {
        profiles.value = emptyMap()
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

    override fun observePendingTasksByCategory(categoryId: String): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { !it.isCompleted && it.categoryId == categoryId }
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

private class FakeCategoryDao : CategoryDao {
    private val categories = MutableStateFlow<List<CategoryEntity>>(emptyList())

    override fun observeCategories(scope: String): Flow<List<CategoryEntity>> = categories.map { list ->
        list.filter { it.scope == scope }.sortedWith(compareBy<CategoryEntity> { it.sortOrder }.thenBy { it.name })
    }

    override suspend fun getCategory(id: String): CategoryEntity? = categories.value.firstOrNull { it.id == id }

    override suspend fun getAll(): List<CategoryEntity> = categories.value

    override suspend fun upsert(category: CategoryEntity) {
        categories.value = categories.value.filterNot { it.id == category.id } + category
    }

    override suspend fun upsertAll(categories: List<CategoryEntity>) {
        categories.forEach { upsert(it) }
    }

    override suspend fun delete(category: CategoryEntity) {
        categories.value = categories.value.filterNot { it.id == category.id }
    }

    override suspend fun clear() {
        categories.value = emptyList()
    }
}

private class FakeFocusSessionDao : FocusSessionDao {
    private val sessions = MutableStateFlow<List<FocusSessionEntity>>(emptyList())

    override fun observeActiveSession(): Flow<FocusSessionEntity?> = sessions.map { list ->
        list.filter { it.status == "active" }.maxByOrNull { it.updatedAt }
    }

    override fun observeSessionsFrom(fromEpochMillis: Long): Flow<List<FocusSessionEntity>> = sessions.map { list ->
        list.filter { it.startedAt >= fromEpochMillis }.sortedBy { it.startedAt }
    }

    override suspend fun getAll(): List<FocusSessionEntity> = sessions.value

    override suspend fun upsert(session: FocusSessionEntity) {
        sessions.value = sessions.value.filterNot { it.id == session.id } + session
    }

    override suspend fun upsertAll(sessions: List<FocusSessionEntity>) {
        sessions.forEach { upsert(it) }
    }

    override suspend fun clear() {
        sessions.value = emptyList()
    }
}

private class FakeReminderDao : ReminderDao {
    private val reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())

    override fun observeReminders(): Flow<List<ReminderEntity>> = reminders

    override fun observeEnabledReminders(): Flow<List<ReminderEntity>> = reminders.map { list ->
        list.filter { it.isEnabled }.sortedBy { it.nextScheduledAt }
    }

    override suspend fun getReminder(id: String): ReminderEntity? = reminders.value.firstOrNull { it.id == id }

    override suspend fun getAll(): List<ReminderEntity> = reminders.value

    override suspend fun upsert(reminder: ReminderEntity) {
        reminders.value = reminders.value.filterNot { it.id == reminder.id } + reminder
    }

    override suspend fun upsertAll(reminders: List<ReminderEntity>) {
        reminders.forEach { upsert(it) }
    }

    override suspend fun delete(reminder: ReminderEntity) {
        reminders.value = reminders.value.filterNot { it.id == reminder.id }
    }

    override suspend fun clear() {
        reminders.value = emptyList()
    }
}

private class FakeReminderEventDao : ReminderEventDao {
    private val events = MutableStateFlow<List<ReminderEventEntity>>(emptyList())

    override fun observeEventsFrom(fromEpochMillis: Long): Flow<List<ReminderEventEntity>> = events.map { list ->
        list.filter { it.firedAt >= fromEpochMillis }.sortedByDescending { it.firedAt }
    }

    override fun observeRecentEvents(limit: Int): Flow<List<ReminderEventEntity>> = events.map { list ->
        list.sortedByDescending { it.firedAt }.take(limit)
    }

    override suspend fun getAll(): List<ReminderEventEntity> = events.value

    override suspend fun insert(event: ReminderEventEntity) {
        events.value = events.value.filterNot { it.id == event.id } + event
    }

    override suspend fun upsertAll(events: List<ReminderEventEntity>) {
        events.forEach { insert(it) }
    }

    override suspend fun clear() {
        events.value = emptyList()
    }
}

private class FakeWorkingHourRuleDao : WorkingHourRuleDao {
    private val rules = MutableStateFlow<List<WorkingHourRuleEntity>>(emptyList())

    override fun observeRules(): Flow<List<WorkingHourRuleEntity>> = rules.map { list ->
        list.sortedBy { it.sortOrder }
    }

    override suspend fun getAll(): List<WorkingHourRuleEntity> = rules.value

    override suspend fun upsert(rule: WorkingHourRuleEntity) {
        rules.value = rules.value.filterNot { it.id == rule.id } + rule
    }

    override suspend fun upsertAll(rules: List<WorkingHourRuleEntity>) {
        rules.forEach { upsert(it) }
    }

    override suspend fun clear() {
        rules.value = emptyList()
    }
}
