package com.kaemis.healthdesk.domain.reminders

import com.kaemis.healthdesk.data.entity.ReminderEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object ReminderScheduler {
    fun nextScheduledAt(
        reminder: ReminderEntity,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? = when (reminder.scheduleMode) {
        "interval" -> nextInterval(reminder, nowMillis)
        "fixedTimeOnce" -> nextFixedTime(reminder, nowMillis, zoneId)
        "recurring" -> nextRecurring(reminder, nowMillis, zoneId)
        else -> null
    }

    private fun nextInterval(reminder: ReminderEntity, nowMillis: Long): Long? {
        val intervalMinutes = reminder.intervalMinutes?.coerceAtLeast(1) ?: return null
        val base = reminder.lastFiredAt ?: nowMillis
        val candidate = base + intervalMinutes * 60_000L
        return if (candidate > nowMillis) candidate else nowMillis + intervalMinutes * 60_000L
    }

    private fun nextFixedTime(reminder: ReminderEntity, nowMillis: Long, zoneId: ZoneId): Long? {
        val time = parseTime(reminder.fixedLocalTime) ?: return null
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val today = LocalDateTime.of(now.toLocalDate(), time)
        val next = if (today.isAfter(now)) today else today.plusDays(1)
        return next.atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun nextRecurring(reminder: ReminderEntity, nowMillis: Long, zoneId: ZoneId): Long? {
        val unit = reminder.recurrenceUnit ?: return null
        val interval = reminder.recurrenceInterval?.coerceAtLeast(1) ?: 1
        val time = parseTime(reminder.fixedLocalTime) ?: LocalTime.of(9, 0)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val candidate = when (unit) {
            "daily" -> nextDaily(now, time, interval)
            "weekly" -> nextWeekly(now, time, interval, weekdays(reminder.recurrenceWeekdays))
            "monthly" -> nextMonthlyDays(now, time, interval, monthDays(reminder.recurrenceWeekdays))
            "monthlyDay" -> nextMonthlyDay(now, time, interval, reminder.recurrenceDayOfMonth ?: 1)
            "monthlyWeekday" -> nextMonthlyWeekday(
                now = now,
                time = time,
                interval = interval,
                weekday = weekdays(reminder.recurrenceWeekdays).firstOrNull() ?: return null,
                nth = reminder.recurrenceDayOfMonth ?: 1,
            )
            "yearly" -> nextYearly(now, time, interval, reminder.recurrenceMonth ?: 1, reminder.recurrenceDay ?: 1)
            else -> null
        } ?: return null
        val endDate = reminder.recurrenceEndDate?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
        if (endDate != null && candidate.toLocalDate().isAfter(endDate)) return null
        return candidate.atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun nextDaily(now: LocalDateTime, time: LocalTime, interval: Int): LocalDateTime {
        val today = now.toLocalDate()
        for (offset in 0..3660) {
            if (offset % interval != 0) continue
            val candidate = LocalDateTime.of(today.plusDays(offset.toLong()), time)
            if (candidate.isAfter(now)) return candidate
        }
        return LocalDateTime.of(today.plusDays(interval.toLong()), time)
    }

    private fun nextWeekly(
        now: LocalDateTime,
        time: LocalTime,
        interval: Int,
        weekdays: List<DayOfWeek>,
    ): LocalDateTime? {
        if (weekdays.isEmpty()) return null
        val startOfThisWeek = now.toLocalDate().with(DayOfWeek.MONDAY)
        for (offset in 0..(7 * 520)) {
            val candidateDate = now.toLocalDate().plusDays(offset.toLong())
            val weeks = ChronoUnit.WEEKS.between(startOfThisWeek, candidateDate.with(DayOfWeek.MONDAY))
            if (weeks % interval != 0L || candidateDate.dayOfWeek !in weekdays) continue
            val candidate = LocalDateTime.of(candidateDate, time)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    private fun nextMonthlyDay(now: LocalDateTime, time: LocalTime, interval: Int, dayOfMonth: Int): LocalDateTime {
        val startMonth = YearMonth.from(now)
        for (offset in 0..240) {
            if (offset % interval != 0) continue
            val month = startMonth.plusMonths(offset.toLong())
            val day = dayOfMonth.coerceIn(1, month.lengthOfMonth())
            val candidate = LocalDateTime.of(month.atDay(day), time)
            if (candidate.isAfter(now)) return candidate
        }
        val month = startMonth.plusMonths(interval.toLong())
        return LocalDateTime.of(month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth())), time)
    }

    private fun nextMonthlyDays(
        now: LocalDateTime,
        time: LocalTime,
        interval: Int,
        daysOfMonth: List<Int>,
    ): LocalDateTime? {
        if (daysOfMonth.isEmpty()) return null
        val startMonth = YearMonth.from(now)
        for (offset in 0..240) {
            if (offset % interval != 0) continue
            val month = startMonth.plusMonths(offset.toLong())
            daysOfMonth.sorted().forEach { dayOfMonth ->
                val safeDay = dayOfMonth.coerceIn(1, month.lengthOfMonth())
                val candidate = LocalDateTime.of(month.atDay(safeDay), time)
                if (candidate.isAfter(now)) return candidate
            }
        }
        return null
    }

    private fun nextMonthlyWeekday(
        now: LocalDateTime,
        time: LocalTime,
        interval: Int,
        weekday: DayOfWeek,
        nth: Int,
    ): LocalDateTime? {
        val startMonth = YearMonth.from(now)
        for (offset in 0..240) {
            if (offset % interval != 0) continue
            val month = startMonth.plusMonths(offset.toLong())
            val candidateDate = nthWeekday(month, weekday, nth)
            val candidate = LocalDateTime.of(candidateDate, time)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    private fun nextYearly(now: LocalDateTime, time: LocalTime, interval: Int, month: Int, day: Int): LocalDateTime {
        val safeMonth = month.coerceIn(1, 12)
        for (offset in 0..50) {
            if (offset % interval != 0) continue
            val yearMonth = YearMonth.of(now.year + offset, safeMonth)
            val candidate = LocalDateTime.of(yearMonth.atDay(day.coerceIn(1, yearMonth.lengthOfMonth())), time)
            if (candidate.isAfter(now)) return candidate
        }
        val yearMonth = YearMonth.of(now.year + interval, safeMonth)
        return LocalDateTime.of(yearMonth.atDay(day.coerceIn(1, yearMonth.lengthOfMonth())), time)
    }

    private fun nthWeekday(month: YearMonth, weekday: DayOfWeek, nth: Int): LocalDate {
        return if (nth < 0) {
            month.atEndOfMonth().with(TemporalAdjusters.previousOrSame(weekday))
        } else {
            month.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(nth.coerceAtLeast(1), weekday))
        }
    }

    private fun parseTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    private fun weekdays(value: String?): List<DayOfWeek> = value
        ?.split(',')
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it in 1..7 }
        ?.map(DayOfWeek::of)
        .orEmpty()

    private fun monthDays(value: String?): List<Int> = value
        ?.split(',')
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it in 1..31 }
        .orEmpty()
}
