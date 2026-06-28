package com.qiwenshare.file.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类 — Java 8 Time API 封装
 * 注意：不和 cn.hutool.core.date.DateUtil 同时 import，避免类名冲突
 */
public class DateUtil {

    /** 标准日期时间格式 */
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /** 标准日期格式 */
    public static final String PATTERN_DATE = "yyyy-MM-dd";

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATETIME);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATE);

    /**
     * 获取当前时间字符串（兼容旧版 varchar 格式）
     */
    public static String getCurrentTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    /**
     * 获取当前日期字符串
     */
    public static String getCurrentDate() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }

    /**
     * 格式化 LocalDateTime 为标准字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    /**
     * 解析标准日期时间字符串
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }
}
