package com.tf.backend.core.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter DATE_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private TimeUtils() {}

    /**
     * 格式化 LocalDate 为 yyyy-MM-dd 格式。
     */
    public static String formatDate(LocalDate localDate) {
        return localDate.format(DATE);
    }

    /**
     * 格式化 LocalDate 为紧凑格式 yyyyMMdd。
     */
    public static String formatCompactDate(LocalDate localDate) {
        return localDate.format(DATE_COMPACT);
    }

    /**
     * 格式化 LocalDate 为路径格式 yyyy/MM/dd。
     */
    public static String formatDatePath(LocalDate localDate) {
        return localDate.format(DATE_PATH);
    }

    /**
     * 格式化 LocalDateTime 为 yyyy-MM-dd HH:mm:ss。
     */
    public static String formatDateTime(LocalDateTime localDateTime) {
        return localDateTime.format(DATETIME);
    }

    /**
     * 返回当天的紧凑日期格式 yyyyMMdd。
     */
    public static String todayCompact() {
        return LocalDate.now().format(DATE_COMPACT);
    }

    /**
     * 返回今天的目录结构日期格式 yyyy/MM/dd。
     */
    public static String todayPath() {
        return LocalDate.now().format(DATE_PATH);
    }
}
