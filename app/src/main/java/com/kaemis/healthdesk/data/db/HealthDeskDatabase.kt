package com.kaemis.healthdesk.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kaemis.healthdesk.data.dao.CategoryDao
import com.kaemis.healthdesk.data.dao.FocusSessionDao
import com.kaemis.healthdesk.data.dao.ReminderDao
import com.kaemis.healthdesk.data.dao.ReminderEventDao
import com.kaemis.healthdesk.data.dao.TaskDao
import com.kaemis.healthdesk.data.dao.UserProfileDao
import com.kaemis.healthdesk.data.dao.WorkingHourRuleDao
import com.kaemis.healthdesk.data.entity.CategoryEntity
import com.kaemis.healthdesk.data.entity.FocusSessionEntity
import com.kaemis.healthdesk.data.entity.ReminderEntity
import com.kaemis.healthdesk.data.entity.ReminderEventEntity
import com.kaemis.healthdesk.data.entity.TaskEntity
import com.kaemis.healthdesk.data.entity.UserProfileEntity
import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity

/**
 * Main local Room database for HealthDesk native.
 *
 * The schema is local-only and must remain export/import friendly.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        TaskEntity::class,
        CategoryEntity::class,
        FocusSessionEntity::class,
        ReminderEntity::class,
        ReminderEventEntity::class,
        WorkingHourRuleEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class HealthDeskDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderEventDao(): ReminderEventDao
    abstract fun workingHourRuleDao(): WorkingHourRuleDao
}

val HEALTHDESK_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE reminders ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'notifications'")
    }
}

val HEALTHDESK_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceEndDate TEXT")
    }
}
