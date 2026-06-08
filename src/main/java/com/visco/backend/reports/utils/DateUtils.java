package com.visco.backend.reports.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
// Utility for date formatting and calculating next scheduled execution times.
public final class DateUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String formatDate(LocalDateTime date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    public static String formatDateTime(LocalDateTime date) {
        return date != null ? date.format(DATE_TIME_FMT) : "";
    }

    public static LocalDateTime calculateNextExecution(LocalTime scheduleTime, DayOfWeek dayOfWeek, Integer dayOfMonth) {
        LocalDate today = LocalDate.now();
        LocalDateTime base = LocalDateTime.of(today, scheduleTime != null ? scheduleTime : LocalTime.of(8, 0));

        if (base.isBefore(LocalDateTime.now())) {
            base = base.plusDays(1);
        }

        if (dayOfWeek != null) {
            return base.with(TemporalAdjusters.next(dayOfWeek));
        }

        if (dayOfMonth != null) {
            LocalDate next = today.withDayOfMonth(Math.min(dayOfMonth, today.lengthOfMonth()));
            if (next.isBefore(today) || (next.isEqual(today) && base.toLocalTime().isBefore(LocalTime.now()))) {
                next = next.plusMonths(1);
                next = next.withDayOfMonth(Math.min(dayOfMonth, next.lengthOfMonth()));
            }
            return LocalDateTime.of(next, base.toLocalTime());
        }

        if (base.toLocalDate().equals(today) && base.toLocalTime().isBefore(LocalTime.now())) {
            base = base.plusDays(1);
        }
        return base;
    }
}
