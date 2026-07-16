package com.kaemis.healthdesk.ui.focus

import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkingHoursEvaluatorTest {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    @Test
    fun currentWorkingPeriodEndReturnsSameDayEnd() {
        val now = epochMillis(LocalDate.of(2026, 7, 13), LocalTime.of(9, 0))
        val expectedEnd = epochMillis(LocalDate.of(2026, 7, 13), LocalTime.of(18, 0))

        assertEquals(expectedEnd, WorkingHoursEvaluator.currentWorkingPeriodEndMillis(listOf(rule(1, "08:00", "18:00")), now, zoneId))
    }

    @Test
    fun currentWorkingPeriodEndHandlesCrossMidnightFromStartDay() {
        val now = epochMillis(LocalDate.of(2026, 7, 13), LocalTime.of(23, 0))
        val expectedEnd = epochMillis(LocalDate.of(2026, 7, 14), LocalTime.of(2, 0))

        assertEquals(expectedEnd, WorkingHoursEvaluator.currentWorkingPeriodEndMillis(listOf(rule(1, "22:00", "02:00")), now, zoneId))
    }

    @Test
    fun currentWorkingPeriodEndHandlesCrossMidnightAfterMidnight() {
        val now = epochMillis(LocalDate.of(2026, 7, 14), LocalTime.of(1, 0))
        val expectedEnd = epochMillis(LocalDate.of(2026, 7, 14), LocalTime.of(2, 0))

        assertEquals(expectedEnd, WorkingHoursEvaluator.currentWorkingPeriodEndMillis(listOf(rule(1, "22:00", "02:00")), now, zoneId))
    }

    @Test
    fun currentWorkingPeriodEndReturnsNullOutsideHours() {
        val now = epochMillis(LocalDate.of(2026, 7, 13), LocalTime.of(7, 0))

        assertNull(WorkingHoursEvaluator.currentWorkingPeriodEndMillis(listOf(rule(1, "08:00", "18:00")), now, zoneId))
    }

    @Test
    fun isWithinWorkingHoursHandlesAllDayRule() {
        val now = epochMillis(LocalDate.of(2026, 7, 13), LocalTime.of(3, 0))

        assertTrue(WorkingHoursEvaluator.isWithinWorkingHours(listOf(rule(1, "08:00", "08:00")), now, zoneId))
    }

    private fun rule(dayOfWeek: Int, start: String, end: String): WorkingHourRuleEntity = WorkingHourRuleEntity(
        id = "rule-$dayOfWeek",
        dayOfWeek = dayOfWeek,
        isEnabled = true,
        startLocalTime = start,
        endLocalTime = end,
        sortOrder = dayOfWeek,
        updatedAt = 1000L,
    )

    private fun epochMillis(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
}
