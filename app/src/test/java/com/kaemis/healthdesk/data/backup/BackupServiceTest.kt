package com.kaemis.healthdesk.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupServiceTest {
    private val service = BackupService()

    @Test
    fun exportsAndParsesNativeBackup() {
        val payload = NativeBackupPayload(
            appVersionName = "1.0.0",
            appVersionCode = 1,
            exportedAt = 1000L,
            profile = BackupProfile(
                id = "local-user",
                displayName = "User",
                avatarMode = "initials",
                createdAt = 1000L,
                updatedAt = 1000L,
            ),
        )

        val json = service.export(payload)
        val result = service.parse(json)

        assertTrue(result is BackupParseResult.Valid)
        assertEquals("User", (result as BackupParseResult.Valid).payload.profile?.displayName)
    }

    @Test
    fun rejectsFutureSchemaVersion() {
        val json = """
            {
              "payloadType": "healthdesk.native.backup",
              "schemaVersion": 999,
              "appVersionName": "1.0.0",
              "appVersionCode": 1,
              "exportedAt": 1000
            }
        """.trimIndent()

        val result = service.parse(json)

        assertEquals(BackupParseResult.FutureSchema(999), result)
    }

    @Test
    fun rejectsWrongPayloadType() {
        val json = """
            {
              "payloadType": "other",
              "schemaVersion": 1,
              "appVersionName": "1.0.0",
              "appVersionCode": 1,
              "exportedAt": 1000
            }
        """.trimIndent()

        assertTrue(service.parse(json) is BackupParseResult.Invalid)
    }

    @Test
    fun parsesNativeFixture() {
        val result = service.parse(readFixture("backup/native-backup-v1.json"))

        assertTrue(result is BackupParseResult.Valid)
        val payload = (result as BackupParseResult.Valid).payload
        assertEquals("1.0.0", payload.appVersionName)
        assertEquals("Plan day", payload.tasks.single().title)
    }

    @Test
    fun parsesFlutterLegacyExportIntoNativePayload() {
        val result = service.parse(readFixture("backup/flutter-legacy-v1.json"))

        assertTrue(result is BackupParseResult.Valid)
        val payload = (result as BackupParseResult.Valid).payload
        assertEquals("flutter-legacy", payload.appVersionName)
        assertEquals(2, payload.tasks.size)
        assertEquals(true, payload.tasks.single { it.id == "task-2" }.isCompleted)
        assertEquals(2, payload.reminders.size)
        assertEquals(true, payload.reminders.single { it.id == "reminder-water-built-in" }.isEnabled)
        assertEquals(5, payload.workingHours.count { it.isEnabled })
        assertEquals(45, payload.settings?.workSessionMinutes)
        assertEquals("es", payload.settings?.languageCode)
        assertEquals(1, payload.reminderEvents.size)
    }

    private fun readFixture(path: String): String = requireNotNull(javaClass.classLoader?.getResource(path)) {
        "Missing fixture $path"
    }.readText()
}
