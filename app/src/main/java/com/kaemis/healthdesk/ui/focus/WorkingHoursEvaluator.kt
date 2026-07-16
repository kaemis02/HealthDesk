package com.kaemis.healthdesk.ui.focus

import com.kaemis.healthdesk.data.entity.WorkingHourRuleEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object WorkingHoursEvaluator {
    fun isWithinWorkingHours(
        rules: List<WorkingHourRuleEntity>,
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val localDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()
        val localTime = localDateTime.toLocalTime()
        val today = localDateTime.dayOfWeek.value
        val yesterday = if (today == 1) 7 else today - 1

        val todayRule = rules.firstOrNull { it.dayOfWeek == today && it.isEnabled }
        if (todayRule != null && contains(todayRule, localTime, checkAfterMidnight = false)) return true

        val yesterdayRule = rules.firstOrNull { it.dayOfWeek == yesterday && it.isEnabled }
        return yesterdayRule != null && contains(yesterdayRule, localTime, checkAfterMidnight = true)
    }

    fun currentWorkingPeriodEndMillis(
        rules: List<WorkingHourRuleEntity>,
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val localDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()
        val localDate = localDateTime.toLocalDate()
        val localTime = localDateTime.toLocalTime()
        val today = localDateTime.dayOfWeek.value
        val yesterday = if (today == 1) 7 else today - 1

        val yesterdayRule = rules.firstOrNull { it.dayOfWeek == yesterday && it.isEnabled }
        val yesterdayEnd = yesterdayRule?.endIfAfterMidnight(localDate, localTime, zoneId)
        if (yesterdayEnd != null) return yesterdayEnd

        val todayRule = rules.firstOrNull { it.dayOfWeek == today && it.isEnabled } ?: return null
        val start = LocalTime.parse(todayRule.startLocalTime)
        val end = LocalTime.parse(todayRule.endLocalTime)
        if (start == end) return null

        val crossesMidnight = end.isBefore(start)
        val isWithin = if (crossesMidnight) {
            !localTime.isBefore(start)
        } else {
            !localTime.isBefore(start) && localTime.isBefore(end)
        }
        if (!isWithin) return null

        val endDate = if (crossesMidnight) localDate.plusDays(1) else localDate
        return LocalDateTime.of(endDate, end).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun WorkingHourRuleEntity.endIfAfterMidnight(
        todayDate: java.time.LocalDate,
        localTime: LocalTime,
        zoneId: ZoneId,
    ): Long? {
        val start = LocalTime.parse(startLocalTime)
        val end = LocalTime.parse(endLocalTime)
        if (start == end || !end.isBefore(start) || !localTime.isBefore(end)) return null
        return LocalDateTime.of(todayDate, end).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun contains(
        rule: WorkingHourRuleEntity,
        localTime: LocalTime,
        checkAfterMidnight: Boolean,
    ): Boolean {
        val start = LocalTime.parse(rule.startLocalTime)
        val end = LocalTime.parse(rule.endLocalTime)
        if (start == end) return true

        val crossesMidnight = end.isBefore(start)
        return if (crossesMidnight) {
            if (checkAfterMidnight) localTime.isBefore(end) else !localTime.isBefore(start)
        } else if (checkAfterMidnight) {
            false
        } else {
            !localTime.isBefore(start) && localTime.isBefore(end)
        }
    }
}
