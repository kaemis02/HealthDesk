package com.kaemis.healthdesk.di

import android.content.Context
import android.app.NotificationManager
import androidx.room.Room
import com.kaemis.healthdesk.data.backup.BackupImportSummary
import com.kaemis.healthdesk.data.backup.BackupService
import com.kaemis.healthdesk.data.backup.NativeBackupPayload
import com.kaemis.healthdesk.data.datastore.SettingsDataStore
import com.kaemis.healthdesk.data.db.HEALTHDESK_MIGRATION_1_2
import com.kaemis.healthdesk.data.db.HealthDeskDatabase
import com.kaemis.healthdesk.data.defaults.DefaultData
import com.kaemis.healthdesk.data.entity.DEFAULT_PROFILE_ID
import com.kaemis.healthdesk.domain.reminders.ReminderScheduler
import com.kaemis.healthdesk.data.repository.CategoryRepository
import com.kaemis.healthdesk.data.repository.FocusSessionRepository
import com.kaemis.healthdesk.data.repository.ProfileRepository
import com.kaemis.healthdesk.data.repository.ReminderRepository
import com.kaemis.healthdesk.data.repository.StatsRepository
import com.kaemis.healthdesk.data.repository.TaskRepository
import com.kaemis.healthdesk.data.repository.WorkingHoursRepository
import com.kaemis.healthdesk.platform.audio.AlarmFeedbackController
import com.kaemis.healthdesk.platform.audio.AndroidAlarmFeedbackController
import com.kaemis.healthdesk.platform.audio.AndroidReminderFeedbackController
import com.kaemis.healthdesk.platform.audio.AndroidTaskFeedbackController
import com.kaemis.healthdesk.platform.audio.ReminderFeedbackController
import com.kaemis.healthdesk.platform.audio.TaskFeedbackController
import com.kaemis.healthdesk.platform.alarm.AndroidFocusAlarmScheduler
import com.kaemis.healthdesk.platform.alarm.AndroidReminderAlarmScheduler
import com.kaemis.healthdesk.platform.alarm.FocusAlarmScheduler
import com.kaemis.healthdesk.platform.alarm.ReminderAlarmScheduler
import com.kaemis.healthdesk.platform.notification.HealthDeskNotificationChannels
import com.kaemis.healthdesk.platform.service.FocusActionBus
import com.kaemis.healthdesk.platform.service.AndroidFocusServiceController
import com.kaemis.healthdesk.platform.service.FocusServiceController
import com.kaemis.healthdesk.ui.focus.FocusUiState
import com.kaemis.healthdesk.widgets.FocusWidgetStateStore
import com.kaemis.healthdesk.widgets.HealthDeskWidgetUpdater
import kotlinx.coroutines.flow.first

/**
 * Manual dependency container for the first native MVP phases.
 *
 * Keep platform and persistence dependencies here until the app is complex enough
 * to justify introducing a DI framework.
 */
class AppContainer(
    applicationContext: Context,
) {
    val context: Context = applicationContext.applicationContext
    val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
    val focusActionBus: FocusActionBus = FocusActionBus(context)
    val focusServiceController: FocusServiceController = AndroidFocusServiceController(context)
    val focusAlarmScheduler: FocusAlarmScheduler = AndroidFocusAlarmScheduler(context)
    val reminderAlarmScheduler: ReminderAlarmScheduler = AndroidReminderAlarmScheduler(context)
    val alarmFeedbackController: AlarmFeedbackController = AndroidAlarmFeedbackController(context)
    val reminderFeedbackController: ReminderFeedbackController = AndroidReminderFeedbackController(context)
    val taskFeedbackController: TaskFeedbackController = AndroidTaskFeedbackController(context)

    val database: HealthDeskDatabase by lazy {
        Room.databaseBuilder(context, HealthDeskDatabase::class.java, "healthdesk.db")
            .addMigrations(HEALTHDESK_MIGRATION_1_2)
            .build()
    }

    val profileRepository: ProfileRepository by lazy { ProfileRepository(database.userProfileDao()) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val focusSessionRepository: FocusSessionRepository by lazy {
        FocusSessionRepository(database.focusSessionDao())
    }
    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao(), database.reminderEventDao())
    }
    val workingHoursRepository: WorkingHoursRepository by lazy {
        WorkingHoursRepository(database.workingHourRuleDao())
    }
    val statsRepository: StatsRepository by lazy {
        StatsRepository(focusSessionRepository, taskRepository, reminderRepository)
    }
    val backupService: BackupService by lazy { BackupService(database, settingsDataStore) }

    suspend fun importNativeBackup(payload: NativeBackupPayload): BackupImportSummary {
        val result = backupService.importNativeBackup(payload, avatarDirectory = java.io.File(context.filesDir, "avatars"))
        if (result is BackupImportSummary.Imported) {
            resyncReminderAlarms()
        }
        return result
    }

    suspend fun initializeDefaults() {
        HealthDeskNotificationChannels.create(context)
        val now = System.currentTimeMillis()
        if (database.userProfileDao().observeProfile(DEFAULT_PROFILE_ID).first() == null) {
            database.userProfileDao().upsert(DefaultData.profile(now))
        }
        if (database.categoryDao().observeCategories("task").first().isEmpty()) {
            database.categoryDao().upsertAll(DefaultData.taskCategories(now))
        }
        if (database.categoryDao().observeCategories("reminder").first().isEmpty()) {
            database.categoryDao().upsertAll(DefaultData.reminderCategories(now))
        }
        if (database.workingHourRuleDao().observeRules().first().isEmpty()) {
            database.workingHourRuleDao().upsertAll(DefaultData.workingHourRules(now))
        }
        if (database.reminderDao().getReminder("reminder-water-built-in") == null) {
            database.reminderDao().upsert(DefaultData.waterReminder(now))
        }
        resyncReminderAlarms()
    }

    suspend fun resyncReminderAlarms() {
        val now = System.currentTimeMillis()
        database.reminderDao().observeEnabledReminders().first().forEach { reminder ->
            val next = ReminderScheduler.nextScheduledAt(reminder, now)
            if (next == null) {
                reminderAlarmScheduler.cancel(reminder.id)
            } else {
                val updated = reminder.copy(nextScheduledAt = next, updatedAt = now)
                database.reminderDao().upsert(updated)
                reminderAlarmScheduler.schedule(updated.id, next)
            }
        }
    }

    suspend fun resetLocalData() {
        // Cancel platform work before removing its persisted source of truth.
        focusAlarmScheduler.cancelPhaseEnd()
        focusAlarmScheduler.cancelWorkdayEnd()
        database.reminderDao().getAll().forEach { reminderAlarmScheduler.cancel(it.id) }
        alarmFeedbackController.stopAlarm()
        focusServiceController.update(FocusUiState())
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        java.io.File(context.filesDir, "avatars").deleteRecursively()
        FocusWidgetStateStore.clear(context)
        focusActionBus.clear()
        HealthDeskWidgetUpdater.updateAll(context)
        database.clearAllTables()
        settingsDataStore.resetToDefaults()
        initializeDefaults()
        HealthDeskWidgetUpdater.updateAll(context)
    }
}
