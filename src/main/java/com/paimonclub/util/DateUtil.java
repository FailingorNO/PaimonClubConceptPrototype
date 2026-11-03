package com.paimonclub.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具：统一对外的时间字符串格式。
 */
public final class DateUtil {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");

    private DateUtil() {
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : DATE_TIME.format(time);
    }

    public static String format(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }

    public static String formatMonthDay(LocalDateTime time) {
        return time == null ? null : MONTH_DAY.format(time);
    }

    public static LocalDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim().replace('T', ' ');
        if (value.length() == 10) {
            value = value + " 00:00:00";
        }
        if (value.length() == 16) {
            value = value + ":00";
        }
        return LocalDateTime.parse(value, DATE_TIME);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
